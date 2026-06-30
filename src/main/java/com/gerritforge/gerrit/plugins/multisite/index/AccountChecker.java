// Copyright (C) 2026 GerritForge, Inc.
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

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

@Singleton
public class AccountChecker implements UpToDateChecker<AccountIndexEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager repoManager;
  private final AllUsersName allUsers;

  @Inject
  AccountChecker(GitRepositoryManager repoManager, AllUsersName allUsers) {
    this.repoManager = repoManager;
    this.allUsers = allUsers;
  }

  @Override
  public boolean isUpToDate(Optional<AccountIndexEvent> event) {
    if (event.isEmpty() || event.get().targetSha == null) {
      logger.atWarning().log("Account index event has no target SHA; skipping the revision check");
      return true;
    }

    AccountIndexEvent accountEvent = event.get();
    try (Repository repo = repoManager.openRepository(allUsers);
        RevWalk revWalk = new RevWalk(repo)) {
      Ref accountRef = repo.exactRef(RefNames.refsUsers(Account.id(accountEvent.accountId)));
      if (accountRef == null) {
        return false;
      }
      RevCommit expected = revWalk.parseCommit(ObjectId.fromString(accountEvent.targetSha));
      RevCommit current = revWalk.parseCommit(accountRef.getObjectId());
      return revWalk.isMergedInto(expected, current);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Could not check whether account %d is up-to-date", accountEvent.accountId);
      return false;
    }
  }
}
