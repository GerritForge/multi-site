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
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles acknowledgement throttling for partition-level index consumers.
 *
 * <p>The last acknowledgement timestamp is tracked independently for each index event type because
 * each type is consumed from a separate partition. An event is acknowledged only when the
 * configured {@code index.commitIntervalMs} has elapsed for that event type, or for every event
 * when the interval is set to {@code 0}.
 */
@Singleton
public class IndexEventAckHandler {
  private final long commitIntervalMs;
  private final Map<String, Long> lastAckByEventType = new HashMap<>();

  @Inject
  IndexEventAckHandler(Configuration cfg) {
    this.commitIntervalMs = cfg.index().commitIntervalMs();
  }

  /**
   * Acknowledges the event when the configured acknowledgement interval is due for its event type.
   *
   * <p>This method is synchronized because acknowledgement timestamps are mutable shared state on
   * the singleton handler.
   */
  public synchronized void ackIfDue(
      IndexEvent event, MessageAcknowledgement<Event> acknowledgement) {
    long now = TimeUtil.nowMs();
    Long lastAck = lastAckByEventType.get(event.getType());
    // Sparse partition can repeatedly reprocess already-handled events after consumer restarts
    // Ack the first event per type immediately (lastAck == null) then rate-limit the subsequent
    // acks.
    if (lastAck == null || commitIntervalMs == 0 || now - lastAck >= commitIntervalMs) {
      acknowledgement.ack(event);
      lastAckByEventType.put(event.getType(), now);
    }
  }
}
