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

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.index.project.ProjectIndexCollection;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.index.account.AccountIndex;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventAckHandlerTest {
  private static final String INSTANCE_ID = "instance-id";
  private static final long TEST_TIME = 1000;

  @Mock private MessageAcknowledgement<Event> ack;
  @Mock private AccountIndexCollection accountIndexes;
  @Mock private ChangeIndexCollection changeIndexes;
  @Mock private GroupIndexCollection groupIndexes;
  @Mock private ProjectIndexCollection projectIndexes;
  @Mock private AccountIndex accountIndex;
  @Mock private ChangeIndex changeIndex;
  @Mock private Configuration cfg;
  @Mock private Configuration.Index indexConfig;
  private IndexEventAckHandler ackHandler;

  @Before
  public void setUp() {
    when(cfg.index()).thenReturn(indexConfig);
    when(indexConfig.ackIntervalMs()).thenReturn(0L);
    ackHandler = newAckHandler();
  }

  @After
  public void tearDown() {
    TimeUtil.resetCurrentMillisSupplier();
  }

  @Test
  public void shouldAckWhenAckIsDue() throws Exception {
    ChangeIndexEvent event = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));

    ackHandler.ackIfDue(event, ack);

    verify(changeIndex).flushAndCommit();
    verify(ack).ack(event);
  }

  @Test
  public void shouldNotAckAgainBeforeAckInterval() throws Exception {
    when(indexConfig.ackIntervalMs()).thenReturn(60000L);
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    AtomicLong now = new AtomicLong(TEST_TIME);
    TimeUtil.setCurrentMillisSupplier(now::get);
    ackHandler = newAckHandler();
    ChangeIndexEvent firstEvent = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);
    ChangeIndexEvent secondEvent = new ChangeIndexEvent("projectName", 4, false, INSTANCE_ID);
    ChangeIndexEvent thirdEvent = new ChangeIndexEvent("projectName", 5, false, INSTANCE_ID);

    ackHandler.ackIfDue(firstEvent, ack);
    ackHandler.ackIfDue(secondEvent, ack);
    now.set(TEST_TIME + 60000);
    ackHandler.ackIfDue(thirdEvent, ack);

    verify(ack, never()).ack(firstEvent);
    verify(ack, never()).ack(secondEvent);
    verify(changeIndex).flushAndCommit();
    verify(ack).ack(thirdEvent);
  }

  @Test
  public void shouldFlushAllDirtyIndexesBeforeAcking() throws Exception {
    when(indexConfig.ackIntervalMs()).thenReturn(60000L);
    when(accountIndexes.getWriteIndexes()).thenReturn(List.of(accountIndex));
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    AtomicLong now = new AtomicLong(TEST_TIME);
    TimeUtil.setCurrentMillisSupplier(now::get);
    ackHandler = newAckHandler();
    AccountIndexEvent accountEvent = new AccountIndexEvent(1, INSTANCE_ID);
    ChangeIndexEvent changeEvent = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);

    ackHandler.ackIfDue(accountEvent, ack);
    now.set(TEST_TIME + 60000);
    ackHandler.ackIfDue(changeEvent, ack);

    verify(accountIndex).flushAndCommit();
    verify(changeIndex).flushAndCommit();
    verify(ack).ack(changeEvent);
  }

  @Test
  public void shouldNotAckWhenFlushFails() throws Exception {
    ChangeIndexEvent event = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);
    when(changeIndexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    doThrow(new IOException("flush failed")).when(changeIndex).flushAndCommit();

    assertThrows(IOException.class, () -> ackHandler.ackIfDue(event, ack));

    verify(changeIndex).flushAndCommit();
    verifyNoInteractions(ack);
  }

  private IndexEventAckHandler newAckHandler() {
    return new IndexEventAckHandler(
        accountIndexes, changeIndexes, groupIndexes, projectIndexes, cfg);
  }
}
