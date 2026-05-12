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

package com.gerritforge.gerrit.plugins.multisite.consumer;

import static org.junit.Assert.assertEquals;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgementException;
import com.google.gerrit.server.events.Event;
import org.junit.Ignore;

@Ignore
public final class AckTestHelper {

  public abstract static class TestAck implements MessageAcknowledgement<Event> {
    private final boolean autoAck;
    private final boolean fail;
    private int ackCount;

    protected TestAck(boolean autoAck, boolean fail) {
      this.autoAck = autoAck;
      this.fail = fail;
    }

    @Override
    public void ack(Event event) {
      ackCount++;
      if (fail) {
        throw new MessageAcknowledgementException("ack failed");
      }
    }

    public boolean isAutoAck() {
      return autoAck;
    }

    public void assertAckAttemptedOnce() {
      assertEquals(1, ackCount);
    }

    public void assertNotAcked() {
      assertEquals(0, ackCount);
    }
  }

  public static class TestAutoAck extends TestAck {
    public TestAutoAck() {
      super(/* autoAck */ true, /* fail */ false);
    }
  }

  public static class TestManualAck extends TestAck {
    public TestManualAck() {
      this(/* fail */ false);
    }

    public TestManualAck(boolean fail) {
      super(/* autoAck */ false, fail);
    }

    public static TestManualAck failing() {
      return new TestManualAck(/* fail */ true);
    }
  }
}
