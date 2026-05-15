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

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;

public class IndexEventAckHandler {
  private final long ackIntervalMs;
  private final Object ackLock = new Object();
  private long lastAckTs;

  @Inject
  public IndexEventAckHandler(Configuration cfg) {
    this.ackIntervalMs = cfg.index().ackIntervalMs();
    this.lastAckTs = TimeUtil.nowMs();
  }

  public void ackIfDue(IndexEvent sourceEvent, MessageAcknowledgement<Event> ack) {
    synchronized (ackLock) {
      long now = TimeUtil.nowMs();
      if (ackIntervalMs == 0 || now - lastAckTs >= ackIntervalMs) {
        ack.ack(sourceEvent);
        lastAckTs = now;
      }
    }
  }
}
