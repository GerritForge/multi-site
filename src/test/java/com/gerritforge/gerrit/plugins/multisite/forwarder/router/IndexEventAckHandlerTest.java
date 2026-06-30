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

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.index.project.ProjectIndexCollection;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.index.account.AccountIndexCollection;
import com.google.gerrit.server.index.change.ChangeIndex;
import com.google.gerrit.server.index.change.ChangeIndexCollection;
import com.google.gerrit.server.index.group.GroupIndexCollection;
import com.google.gerrit.server.util.time.TimeUtil;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventAckHandlerTest {
  private static final long START_TIME = 1000;
  private static final long COMMIT_INTERVAL = 60000;
  private static final String INSTANCE_ID = "instance-id";
  private static final ChangeIndexEvent CHANGE_EVENT =
      new ChangeIndexEvent("project", 1, false, INSTANCE_ID);
  private static final AccountIndexEvent ACCOUNT_EVENT =
      new AccountIndexEvent(1, null, INSTANCE_ID, false);

  @Mock private MessageAcknowledgement<Event> ack;
  @Mock private AccountIndexCollection accountIndexes;
  @Mock private ChangeIndexCollection changeIndexes;
  @Mock private GroupIndexCollection groupIndexes;
  @Mock private ProjectIndexCollection projectIndexes;
  @Mock private ChangeIndex changeIndex;
  @Mock private Configuration cfg;
  @Mock private Configuration.Index indexConfig;
  private final AtomicLong now = new AtomicLong(START_TIME);
  private IndexEventAckHandler ackHandler;

  @Before
  public void setUp() {
    TimeUtil.setCurrentMillisSupplier(now::get);
    when(cfg.index()).thenReturn(indexConfig);
    when(indexConfig.commitIntervalMs()).thenReturn(COMMIT_INTERVAL);
    ackHandler = newAckHandler();
  }

  @After
  public void tearDown() {
    TimeUtil.resetCurrentMillisSupplier();
  }

  @Test
  public void shouldAckWhenIntervalIsDue() throws Exception {
    ackHandler.ackIfDue(CHANGE_EVENT, ack);
    verify(ack, times(1)).ack(CHANGE_EVENT);

    now.addAndGet(COMMIT_INTERVAL);
    ackHandler.ackIfDue(CHANGE_EVENT, ack);

    verify(ack, times(2)).ack(CHANGE_EVENT);
  }

  @Test
  public void shouldTrackPartitionsIndependently() throws Exception {
    now.addAndGet(COMMIT_INTERVAL);

    ackHandler.ackIfDue(CHANGE_EVENT, ack);
    ackHandler.ackIfDue(ACCOUNT_EVENT, ack);

    verify(ack).ack(CHANGE_EVENT);
    verify(ack).ack(ACCOUNT_EVENT);
  }

  @Test
  public void shouldFlushMatchingIndexBeforeAck() throws Exception {
    when(indexConfig.commitIntervalMs()).thenReturn(0L);
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    ackHandler = newAckHandler();

    ackHandler.ackIfDue(CHANGE_EVENT, ack);

    InOrder inOrder = inOrder(changeIndex, ack);
    inOrder.verify(changeIndex).flushAndCommit();
    inOrder.verify(ack).ack(CHANGE_EVENT);
    verifyNoInteractions(accountIndexes, groupIndexes, projectIndexes);
  }

  @Test
  public void shouldNotAckWhenFlushFails() throws Exception {
    when(indexConfig.commitIntervalMs()).thenReturn(0L);
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    doThrow(new IOException("flush failed")).when(changeIndex).flushAndCommit();
    ackHandler = newAckHandler();

    assertThrows(IOException.class, () -> ackHandler.ackIfDue(CHANGE_EVENT, ack));

    verifyNoInteractions(ack);
  }

  private IndexEventAckHandler newAckHandler() {
    return new IndexEventAckHandler(
        accountIndexes, changeIndexes, groupIndexes, projectIndexes, cfg);
  }
}
