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
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgementException;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;

class AckHelper {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static boolean tryAck(
      Event event, MessageAcknowledgement<Event> ack, SubscriberMetrics subscriberMetrics) {
    try {
      ack.ack(event);
      return true;
    } catch (MessageAcknowledgementException e) {
      logger.atSevere().withCause(e).log("Cannot manually ack message '%s'", event);
      subscriberMetrics.incrementSubscriberFailedToAckMessage();
      return false;
    }
  }
}
