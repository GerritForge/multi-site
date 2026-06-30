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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler.Operation;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.index.AccountChecker;
import com.google.gerrit.entities.Account;
import com.google.gerrit.server.index.account.AccountIndexer;
import com.google.gerrit.server.util.OneOffRequestContext;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedIndexAccountHandlerTest {

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock private AccountIndexer indexerMock;
  @Mock private Configuration config;
  @Mock private Configuration.Index index;
  @Mock private AccountChecker accountChecker;
  @Mock private OneOffRequestContext oneOffRequestContext;
  @Mock private ScheduledExecutorService indexExecutor;
  private ForwardedIndexAccountHandler handler;
  private Account.Id id;

  @Before
  public void setUp() throws Exception {
    handler =
        new ForwardedIndexAccountHandler(
            indexerMock, accountChecker, config, oneOffRequestContext, indexExecutor);
    id = Account.id(123);
  }

  @Test
  public void testSuccessfulIndexing() throws Exception {
    handler.index(id, Operation.INDEX, Optional.empty());
    verify(indexerMock).index(id);
  }

  @Test
  public void deleteIsSupported() throws Exception {
    handler.index(id, Operation.DELETE, Optional.empty());
    verify(indexerMock).index(id);
  }

  @Test
  public void shouldIndexSynchronouslyWhenAccountIsReady() throws Exception {
    AccountIndexEvent event = new AccountIndexEvent(id.get(), "target-sha", "instance-id", false);
    when(accountChecker.isConsistent(id)).thenReturn(true);
    when(accountChecker.isUpToDate(Optional.of(event))).thenReturn(true);

    assertThat(handler.handleSync(event))
        .isEqualTo(ForwardedIndexingHandlerWithRetries.IndexingResult.SUCCESS);

    verify(indexerMock).index(id);
  }

  @Test
  public void shouldFailSynchronousIndexingWhenAccountIsNotReady() throws IOException {
    AccountIndexEvent event = new AccountIndexEvent(id.get(), "target-sha", "instance-id", false);
    when(accountChecker.isConsistent(id)).thenReturn(false);

    assertThat(handler.handleSync(event))
        .isEqualTo(ForwardedIndexingHandlerWithRetries.IndexingResult.FAILURE);
    verify(indexerMock, never()).index(id);
  }

  @Test
  public void shouldSetAndUnsetForwardedContext() throws Exception {
    // this doAnswer is to allow to assert that context is set to forwarded
    // while cache eviction is called.
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  assertThat(ForwardedContext.isForwardedEvent()).isTrue();
                  return null;
                })
        .when(indexerMock)
        .index(id);

    assertThat(ForwardedContext.isForwardedEvent()).isFalse();
    handler.index(id, Operation.INDEX, Optional.empty());
    assertThat(ForwardedContext.isForwardedEvent()).isFalse();

    verify(indexerMock).index(id);
  }

  @Test
  public void shouldSetAndUnsetForwardedContextEvenIfExceptionIsThrown() throws Exception {
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  assertThat(ForwardedContext.isForwardedEvent()).isTrue();
                  throw new IOException("someMessage");
                })
        .when(indexerMock)
        .index(id);

    assertThat(ForwardedContext.isForwardedEvent()).isFalse();
    IOException thrown =
        assertThrows(IOException.class, () -> handler.index(id, Operation.INDEX, Optional.empty()));
    assertThat(thrown).hasMessageThat().isEqualTo("someMessage");
    assertThat(ForwardedContext.isForwardedEvent()).isFalse();

    verify(indexerMock).index(id);
  }
}
