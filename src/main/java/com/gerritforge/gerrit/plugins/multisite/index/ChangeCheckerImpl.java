// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.multisite.index;

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.change.ChangeFinder;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ChangeCheckerImpl implements ChangeChecker {
  private static final Logger log = LoggerFactory.getLogger(ChangeCheckerImpl.class);
  private final GitRepositoryManager gitRepoMgr;
  private final OneOffRequestContext oneOffReqCtx;
  private final ChangeFinder changeFinder;
  private final String instanceId;

  @Inject
  public ChangeCheckerImpl(
      GitRepositoryManager gitRepoMgr,
      ChangeFinder changeFinder,
      OneOffRequestContext oneOffReqCtx,
      @GerritInstanceId String instanceId,
      @GerritServerConfig Config config) {
    this.changeFinder = changeFinder;
    this.gitRepoMgr = gitRepoMgr;
    this.oneOffReqCtx = oneOffReqCtx;
    this.instanceId = instanceId;
  }

  @Override
  public Optional<ChangeIndexEvent> newIndexEvent(
      String projectName, int changeNum, boolean deleted) {
    String changeId = projectName + "~" + changeNum;
    return getChangeNotes(changeId)
        .flatMap(
            changeNotes ->
                getComputedChangeTs(changeId)
                    .map(
                        ts -> {
                          ChangeIndexEvent event =
                              new ChangeIndexEvent(projectName, changeNum, deleted, instanceId);
                          event.eventCreatedOn = ts;
                          event.targetSha = getBranchTargetSha(changeNotes);
                          return event;
                        }));
  }

  @Override
  public Optional<ChangeNotes> getChangeNotes(String changeId) {
    try (ManualRequestContext ctx = oneOffReqCtx.open()) {
      return changeFinder.findOne(changeId);
    }
  }

  @Override
  public boolean isUpToDate(Optional<ChangeIndexEvent> indexEventOptional) {
    if (indexEventOptional.isEmpty()) {
      log.warn("Unable to compute last updated ts for change because of an empty indexEvent");
      return true;
    }

    ChangeIndexEvent indexEvent = indexEventOptional.get();

    String changeId = indexEvent.projectName + "~" + indexEvent.changeId;
    Optional<Long> computedChangeTs = getComputedChangeTs(changeId);
    if (computedChangeTs.isEmpty()) {
      log.warn("Unable to compute last updated ts for change {}", changeId);
      return true;
    }

    if (indexEvent.targetSha == null) {
      return computedChangeTs.get() >= indexEvent.eventCreatedOn;
    }

    return getChangeNotes(indexEvent.projectName + "~" + indexEvent.changeId)
        .map(
            changeNotes ->
                (computedChangeTs.get() > indexEvent.eventCreatedOn)
                    || ((computedChangeTs.get() == indexEvent.eventCreatedOn)
                        && repositoryHas(changeNotes, indexEvent.targetSha)))
        .orElse(false);
  }

  private Optional<Long> getComputedChangeTs(String changeId) {
    return getChangeNotes(changeId).map(this::getTsFromChange);
  }

  private String getBranchTargetSha(ChangeNotes changeNotes) {
    String changeId = changeNotes.getProjectName() + "~" + changeNotes.getChangeId().get();
    try {
      try (Repository repo = gitRepoMgr.openRepository(changeNotes.getProjectName())) {
        String refName = changeNotes.getChange().getDest().branch();
        Ref ref = repo.exactRef(refName);
        if (ref == null) {
          log.warn("Unable to find target ref {} for change {}", refName, changeId);
          return null;
        }
        return ref.getTarget().getObjectId().getName();
      }
    } catch (IOException e) {
      log.warn("Unable to resolve target branch SHA for change {}", changeId, e);
      return null;
    }
  }

  private boolean repositoryHas(ChangeNotes changeNotes, String targetSha) {
    String changeId = changeNotes.getProjectName() + "~" + changeNotes.getChangeId().get();
    try (Repository repo = gitRepoMgr.openRepository(changeNotes.getProjectName())) {
      return repo.parseCommit(ObjectId.fromString(targetSha)) != null;
    } catch (IOException e) {
      log.warn("Unable to find SHA1 {} for change {}", targetSha, changeId, e);
      return false;
    }
  }

  @Override
  public boolean isConsistent(String changeId) {
    Optional<ChangeNotes> notes = getChangeNotes(changeId);
    if (notes.isEmpty()) {
      log.warn("Unable to compute change notes for change {}", changeId);
      return false;
    }

    ObjectId currentPatchSetCommitId = notes.get().getCurrentPatchSet().commitId();
    try (Repository repo = gitRepoMgr.openRepository(notes.get().getProjectName());
        RevWalk walk = new RevWalk(repo)) {
      walk.parseCommit(currentPatchSetCommitId);
    } catch (StorageException | MissingObjectException e) {
      log.warn(
          String.format(
              "Consistency check failed for change %s, missing current patchset commit %s",
              changeId, currentPatchSetCommitId.getName()),
          e);
      return false;
    } catch (IOException e) {
      log.warn(
          String.format(
              "Cannot check consistency for change %s, current patchset commit %s. Assuming change"
                  + " is consistent",
              changeId, currentPatchSetCommitId.getName()),
          e);
    }
    return true;
  }

  private long getTsFromChange(ChangeNotes notes) {
    Change change = notes.getChange();
    Timestamp changeTs = Timestamp.from(change.getLastUpdatedOn());
    return changeTs.getTime() / 1000;
  }
}
