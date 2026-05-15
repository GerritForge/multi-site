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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
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
  private static final String INSTANCE_ID = "instance-id";
  private static final long TEST_TIME = 1000;

  @Mock private MessageAcknowledgement<Event> ack;
  @Mock private Configuration cfg;
  @Mock private Configuration.Index indexConfig;
  private IndexEventAckHandler ackHandler;

  @Before
  public void setUp() {
    when(cfg.index()).thenReturn(indexConfig);
    when(indexConfig.ackIntervalMs()).thenReturn(0L);
    ackHandler = new IndexEventAckHandler(cfg);
  }

  @After
  public void tearDown() {
    TimeUtil.resetCurrentMillisSupplier();
  }

  @Test
  public void shouldAckWhenAckIsDue() {
    ChangeIndexEvent event = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);

    ackHandler.ackIfDue(event, ack);

    verify(ack).ack(event);
  }

  @Test
  public void shouldNotAckAgainBeforeAckInterval() {
    when(indexConfig.ackIntervalMs()).thenReturn(60000L);
    AtomicLong now = new AtomicLong(TEST_TIME);
    TimeUtil.setCurrentMillisSupplier(now::get);
    ackHandler = new IndexEventAckHandler(cfg);
    ChangeIndexEvent firstEvent = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);
    ChangeIndexEvent secondEvent = new ChangeIndexEvent("projectName", 4, false, INSTANCE_ID);
    ChangeIndexEvent thirdEvent = new ChangeIndexEvent("projectName", 5, false, INSTANCE_ID);

    ackHandler.ackIfDue(firstEvent, ack);
    ackHandler.ackIfDue(secondEvent, ack);
    now.set(TEST_TIME + 60000);
    ackHandler.ackIfDue(thirdEvent, ack);

    verify(ack, never()).ack(firstEvent);
    verify(ack, never()).ack(secondEvent);
    verify(ack).ack(thirdEvent);
  }
}
