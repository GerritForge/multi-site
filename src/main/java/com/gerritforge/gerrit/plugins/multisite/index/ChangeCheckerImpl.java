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
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
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

@Singleton
public class ChangeCheckerImpl implements ChangeChecker {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
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
      String projectName, int changeNum, boolean deleted) throws IOException {
    String changeId = projectName + "~" + changeNum;
    try (Repository repo = gitRepoMgr.openRepository(Project.nameKey(projectName))) {
      Optional<ChangeNotes> changeNotes = getChangeNotes(changeId);
      if (changeNotes.isEmpty()) {
        throw new IOException(
            String.format(
                "Unable to get ChangeNotes on project %s for change %s", projectName, changeId));
      }

      ChangeNotes notes = changeNotes.get();
      ChangeIndexEvent event = new ChangeIndexEvent(projectName, changeNum, deleted, instanceId);
      event.metaSha = changeNotes.get().getRevision().getName();
      event.eventCreatedOn = getTsFromChange(notes);
      event.targetSha = getBranchTargetSha(repo, notes);
      return Optional.of(event);
    }
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
      log.atWarning().log(
          "Unable to compute last updated ts for change because of an empty indexEvent");
      return true;
    }

    ChangeIndexEvent indexEvent = indexEventOptional.get();

    String changeId = indexEvent.projectName + "~" + indexEvent.changeId;
    Optional<ChangeNotes> changeNotes = getChangeNotes(changeId);
    if (changeNotes.isEmpty()) {
      log.atWarning().log("Unable to compute last updated ts for change %s", changeId);
      return true;
    }
    long computedChangeTs = getTsFromChange(changeNotes.get());

    return (computedChangeTs > indexEvent.eventCreatedOn)
        || ((computedChangeTs == indexEvent.eventCreatedOn)
            && repositoryHasRequiredShas(changeNotes.get(), indexEvent));
  }

  private String getBranchTargetSha(Repository repo, ChangeNotes changeNotes) {
    String changeId = changeNotes.getProjectName() + "~" + changeNotes.getChangeId().get();
    try {
      String refName = changeNotes.getChange().getDest().branch();
      Ref ref = repo.exactRef(refName);
      if (ref == null) {
        log.atFiner().log("Unable to find target ref %s for change %s", refName, changeId);
        return null;
      }
      return ref.getTarget().getObjectId().getName();
    } catch (IOException e) {
      log.atWarning().withCause(e).log(
          "Unable to resolve target branch SHA for change %s", changeId);
      return null;
    }
  }

  private boolean repositoryHas(Repository repo, String changeId, String sha1ToCheck) {
    try {
      return repo.parseCommit(ObjectId.fromString(sha1ToCheck)) != null;
    } catch (IOException e) {
      log.atWarning().withCause(e).log(
          "Unable to find SHA1 %s for change %s", sha1ToCheck, changeId);
      return false;
    }
  }

  private boolean repositoryHasRequiredShas(ChangeNotes changeNotes, ChangeIndexEvent indexEvent) {
    if (indexEvent.targetSha == null && indexEvent.metaSha == null) {
      return true;
    }

    String changeId = changeNotes.getProjectName() + "~" + changeNotes.getChangeId().get();
    try (Repository repo = gitRepoMgr.openRepository(changeNotes.getProjectName())) {
      return (indexEvent.targetSha == null || repositoryHas(repo, changeId, indexEvent.targetSha))
          && (indexEvent.metaSha == null || repositoryHas(repo, changeId, indexEvent.metaSha));
    } catch (IOException e) {
      log.atWarning().withCause(e).log("Unable to open repository for change %s", changeId);
      return false;
    }
  }

  @Override
  public boolean isConsistent(String changeId) {
    Optional<ChangeNotes> notes = getChangeNotes(changeId);
    if (notes.isEmpty()) {
      log.atWarning().log("Unable to compute change notes for change %s", changeId);
      return false;
    }

    ObjectId currentPatchSetCommitId = notes.get().getCurrentPatchSet().commitId();
    try (Repository repo = gitRepoMgr.openRepository(notes.get().getProjectName());
        RevWalk walk = new RevWalk(repo)) {
      walk.parseCommit(currentPatchSetCommitId);
    } catch (StorageException | MissingObjectException e) {
      log.atWarning().withCause(e).log(
          String.format(
              "Consistency check failed for change %s, missing current patchset commit %s",
              changeId, currentPatchSetCommitId.getName()),
          e);
      return false;
    } catch (IOException e) {
      log.atWarning().withCause(e).log(
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
