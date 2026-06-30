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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.util.time.TimeUtil;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventAckHandlerTest {
  private static final long START_TIME = 1000;
  private static final long ACK_INTERVAL = 60000;
  private static final String INSTANCE_ID = "instance-id";
  private static final ChangeIndexEvent CHANGE_EVENT =
      new ChangeIndexEvent("project", 1, false, INSTANCE_ID);
  private static final AccountIndexEvent ACCOUNT_EVENT =
      new AccountIndexEvent(1, null, INSTANCE_ID, false);

  @Mock private MessageAcknowledgement<Event> ack;
  @Mock private Configuration cfg;
  @Mock private Configuration.Index indexConfig;
  private final AtomicLong now = new AtomicLong(START_TIME);
  private IndexEventAckHandler ackHandler;

  @Before
  public void setUp() {
    TimeUtil.setCurrentMillisSupplier(now::get);
    when(cfg.index()).thenReturn(indexConfig);
    when(indexConfig.ackIntervalMs()).thenReturn(ACK_INTERVAL);
    ackHandler = new IndexEventAckHandler(cfg);
  }

  @After
  public void tearDown() {
    TimeUtil.resetCurrentMillisSupplier();
  }

  @Test
  public void shouldAckWhenIntervalIsDue() {
    ackHandler.ackIfDue(CHANGE_EVENT, ack);
    verifyNoInteractions(ack);

    now.addAndGet(ACK_INTERVAL);
    ackHandler.ackIfDue(CHANGE_EVENT, ack);

    verify(ack).ack(CHANGE_EVENT);
  }

  @Test
  public void shouldTrackPartitionsIndependently() {
    now.addAndGet(ACK_INTERVAL);

    ackHandler.ackIfDue(CHANGE_EVENT, ack);
    ackHandler.ackIfDue(ACCOUNT_EVENT, ack);

    verify(ack).ack(CHANGE_EVENT);
    verify(ack).ack(ACCOUNT_EVENT);
  }

  @Test
  public void shouldAckEveryEventWhenIntervalIsZero() {
    when(indexConfig.ackIntervalMs()).thenReturn(0L);
    ackHandler = new IndexEventAckHandler(cfg);

    ackHandler.ackIfDue(CHANGE_EVENT, ack);

    verify(ack).ack(CHANGE_EVENT);
  }
}
