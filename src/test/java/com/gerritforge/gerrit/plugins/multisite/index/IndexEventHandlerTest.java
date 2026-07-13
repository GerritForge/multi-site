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

import static com.gerritforge.gerrit.plugins.replication.pull.api.PullReplicationEndpoints.APPLY_OBJECT_API_ENDPOINT;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.IndexEventForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventHandlerTest {

  private static final String INSTANCE_ID = "instance-id";
  private static final String PROJECT_NAME = "test_project";
  private static final int ACCOUNT_ID = 2;
  private static final String ACCOUNT_TARGET_SHA = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
  private static final int CHANGE_ID = 1;

  private IndexEventHandler eventHandler;

  @Mock private IndexEventForwarder forwarder;
  @Mock private ChangeChecker changeCheckerMock;
  @Mock private AccountCache accountCache;
  @Mock private RequestContext mockCtx;

  private CurrentRequestContext currCtx =
      new CurrentRequestContext(null, null, null) {
        @Override
        public void onlyWithContext(Consumer<RequestContext> body) {
          body.accept(mockCtx);
        }
      };

  @Before
  public void setUp() throws IOException {
    eventHandler =
        new IndexEventHandler(
            MoreExecutors.directExecutor(),
            asDynamicSet(forwarder),
            changeCheckerMock,
            new TestGroupChecker(true),
            accountCache,
            INSTANCE_ID,
            currCtx);
  }

  private DynamicSet<IndexEventForwarder> asDynamicSet(IndexEventForwarder forwarder) {
    DynamicSet<IndexEventForwarder> result = new DynamicSet<>();
    result.add("multi-site", forwarder);
    return result;
  }

  @Test
  public void shouldNotForwardIndexChangeIfCurrentThreadIsPullReplicationApplyObject()
      throws Exception {
    String currentThreadName = Thread.currentThread().getName();
    try {
      Thread.currentThread().setName("pull-replication~" + APPLY_OBJECT_API_ENDPOINT);
      lenient()
          .when(changeCheckerMock.isConsistent(any()))
          .thenThrow(
              new IllegalStateException("Change indexing event should have not been triggered"));

      eventHandler.onChangeIndexed(PROJECT_NAME, CHANGE_ID);
      verifyNoInteractions(changeCheckerMock);
    } finally {
      Thread.currentThread().setName(currentThreadName);
    }
  }

  @Test
  public void shouldNotForwardIndexChangeWhenContextIsMissingAndForcedIndexingDisabled()
      throws Exception {
    eventHandler = createIndexEventHandler(changeCheckerMock, false);
    eventHandler.onChangeIndexed(PROJECT_NAME, CHANGE_ID);
    verifyNoInteractions(changeCheckerMock);
    verifyNoInteractions(forwarder);
  }

  @Test
  public void shouldForwardIndexChangeWhenContextIsMissingAndForcedIndexingEnabled()
      throws Exception {
    when(changeCheckerMock.newIndexEvent(PROJECT_NAME, CHANGE_ID, false))
        .thenReturn(Optional.of(new ChangeIndexEvent(PROJECT_NAME, CHANGE_ID, false, INSTANCE_ID)));
    eventHandler = createIndexEventHandler(changeCheckerMock, true);
    eventHandler.onChangeIndexed(PROJECT_NAME, CHANGE_ID);
    verify(changeCheckerMock).newIndexEvent(PROJECT_NAME, CHANGE_ID, false);
    verify(forwarder).index(any(), any());
  }

  @Test
  public void shouldAddAccountRevisionToIndexEvent() {
    Account account =
        Account.builder(Account.id(ACCOUNT_ID), Instant.EPOCH)
            .setMetaId(ACCOUNT_TARGET_SHA)
            .build();
    when(accountCache.get(account.id())).thenReturn(Optional.of(AccountState.forAccount(account)));

    eventHandler.onAccountIndexed(ACCOUNT_ID);

    verify(forwarder)
        .index(
            any(),
            argThat(
                event ->
                    event instanceof AccountIndexEvent accountEvent
                        && ACCOUNT_TARGET_SHA.equals(accountEvent.targetSha)));
  }

  @Test
  public void shouldDeleteAccountFromIndexEvent() {
    Account.Id accountId = Account.id(ACCOUNT_ID);
    when(accountCache.get(accountId)).thenReturn(Optional.empty());

    eventHandler.onAccountIndexed(ACCOUNT_ID);

    verify(forwarder)
        .index(
            any(),
            argThat(
                event -> event instanceof AccountIndexEvent accountEvent && accountEvent.deleted));
  }

  private IndexEventHandler createIndexEventHandler(
      ChangeChecker changeChecker, boolean synchronizeForced) {
    ThreadLocalRequestContext threadLocalCtxMock = mock(ThreadLocalRequestContext.class);
    OneOffRequestContext oneOffCtxMock = mock(OneOffRequestContext.class);
    Configuration cfgMock = mock(Configuration.class);
    Configuration.Index cfgIndex = mock(Configuration.Index.class);
    when(cfgMock.index()).thenReturn(cfgIndex);
    when(cfgIndex.synchronizeForced()).thenReturn(synchronizeForced);
    return new IndexEventHandler(
        MoreExecutors.directExecutor(),
        asDynamicSet(forwarder),
        changeChecker,
        new TestGroupChecker(true),
        accountCache,
        INSTANCE_ID,
        new CurrentRequestContext(threadLocalCtxMock, cfgMock, oneOffCtxMock));
  }
}
