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

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class IndexEventAckHandler {
  private final long ackIntervalMs;
  private final long startedAt;
  private final Map<String, Long> lastAckByEventType = new HashMap<>();

  @Inject
  IndexEventAckHandler(Configuration cfg) {
    this.ackIntervalMs = cfg.index().ackIntervalMs();
    this.startedAt = TimeUtil.nowMs();
  }

  public synchronized void ackIfDue(
      IndexEvent event, MessageAcknowledgement<Event> acknowledgement) {
    long now = TimeUtil.nowMs();
    long lastAck = lastAckByEventType.getOrDefault(event.getType(), startedAt);
    if (ackIntervalMs == 0 || now - lastAck >= ackIntervalMs) {
      acknowledgement.ack(event);
      lastAckByEventType.put(event.getType(), now);
    }
  }
}
