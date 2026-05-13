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

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.google.gerrit.server.events.Event;

/**
 * Allows a consumer component to customize the manual acknowledgement strategy for routed events.
 */
public interface ManualAcker<EventType extends Event> {

  /**
   * Acknowledges the event when required by the implementation strategy.
   *
   * <p>The default implementation acks immediately. Implementations may override this when they
   * need a different strategy.
   */
  default void ackIfNeeded(
      EventType event, MessageAcknowledgement<Event> ack, SubscriberMetrics subscriberMetrics) {
    if (AckHelper.tryAck(event, ack, subscriberMetrics)) {
      subscriberMetrics.incrementSubscriberConsumedMessage();
    }
  }
}
