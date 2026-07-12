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

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.google.gerrit.testing.InMemoryTestEnvironment;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AccountCheckerTest {
  private static final Account.Id ACCOUNT_ID = Account.id(1);
  private static final AllUsersName ALL_USERS = new AllUsersName(AllUsersNameProvider.DEFAULT);

  @Rule public InMemoryTestEnvironment testEnvironment = new InMemoryTestEnvironment();

  @Inject private InMemoryRepositoryManager repoManager;
  @Inject private AccountCheckerImpl checker;
  private TestRepository<InMemoryRepository> testRepo;

  @Before
  public void setUp() throws Exception {
    testRepo = new TestRepository<>(repoManager.createRepository(ALL_USERS));
  }

  @Test
  public void shouldAcceptAccountRefAheadOfEventRevision() throws Exception {
    RevCommit expected = testRepo.commit().create();
    RevCommit current = testRepo.commit().parent(expected).create();
    testRepo.update(RefNames.refsUsers(ACCOUNT_ID), current);

    assertThat(checker.isUpToDate(event(expected))).isTrue();
  }

  @Test
  public void shouldRejectAccountRefBehindEventRevision() throws Exception {
    RevCommit current = testRepo.commit().create();
    RevCommit expected = testRepo.commit().parent(current).create();
    testRepo.update(RefNames.refsUsers(ACCOUNT_ID), current);

    assertThat(checker.isUpToDate(event(expected))).isFalse();
  }

  @Test
  public void shouldAcceptLegacyEventWithoutRevision() {
    assertThat(
            checker.isUpToDate(Optional.of(new AccountIndexEvent(1, null, "instance-id", false))))
        .isTrue();
  }

  @Test
  public void existingAccountShouldBeConsistent() throws Exception {
    testRepo.update(RefNames.refsUsers(ACCOUNT_ID), testRepo.commit().create());
    assertThat(checker.isConsistent(ACCOUNT_ID)).isTrue();
  }

  @Test
  public void nonExistingAccountShouldNotBeConsistent() {
    assertThat(checker.isConsistent(ACCOUNT_ID)).isFalse();
  }

  private Optional<AccountIndexEvent> event(RevCommit target) {
    return Optional.of(
        new AccountIndexEvent(ACCOUNT_ID.get(), target.name(), "instance-id", false));
  }
}
