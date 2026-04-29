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

package com.gerritforge.gerrit.plugins.multisite.consumer;

import com.gerritforge.gerrit.eventbroker.AckAwareConsumer;
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgementException;
import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheNotFoundException;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ForwardedEventRouter;
import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import java.io.IOException;

public abstract class AbstractSubcriber {
  protected static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ForwardedEventRouter eventRouter;
  private final DynamicSet<DroppedEventListener> droppedEventListeners;
  private final String instanceId;
  private final MessageLogger msgLog;
  private SubscriberMetrics subscriberMetrics;
  private final Configuration cfg;
  private final String topic;

  public AbstractSubcriber(
      ForwardedEventRouter eventRouter,
      DynamicSet<DroppedEventListener> droppedEventListeners,
      @GerritInstanceId String gerritInstanceId,
      MessageLogger msgLog,
      SubscriberMetrics subscriberMetrics,
      Configuration cfg) {
    this.eventRouter = eventRouter;
    this.droppedEventListeners = droppedEventListeners;
    this.instanceId = gerritInstanceId;
    this.msgLog = msgLog;
    this.subscriberMetrics = subscriberMetrics;
    this.cfg = cfg;
    this.topic = getTopic().topic(cfg);
  }

  protected abstract EventTopic getTopic();

  protected abstract Boolean shouldConsumeEvent(Event event);

  public AckAwareConsumer<Event> getConsumer(boolean isAutoAck) {
    return (event, messageAcknowledgement) ->
        processRecord(event, messageAcknowledgement, isAutoAck);
  }

  private void processRecord(
      Event event, MessageAcknowledgement<Event> messageAcknowledgement, boolean isAutoAck) {
    String sourceInstanceId = event.instanceId;

    if (Strings.isNullOrEmpty(sourceInstanceId)) {
      logger.atWarning().log("Dropping event %s because sourceInstanceId cannot be null", event);
    } else if (instanceId.equals(sourceInstanceId)) {
      logger.atFiner().log("Dropping event %s produced by our instanceId %s", event, instanceId);
    } else if (!shouldConsumeEvent(event)) {
      try {
        droppedEventListeners.forEach(l -> l.onEventDropped(event));
      } finally {
        tryAckAndMarkAsConsumed(event, messageAcknowledgement, isAutoAck);
      }
    } else {
      try {
        msgLog.log(MessageLogger.Direction.CONSUME, topic, event);
        eventRouter.route(event);
        tryAckAndMarkAsConsumed(event, messageAcknowledgement, isAutoAck);
      } catch (IOException e) {
        logger.atSevere().withCause(e).log("Malformed event '%s'", event);
        subscriberMetrics.incrementSubscriberFailedToConsumeMessage();
      } catch (PermissionBackendException | CacheNotFoundException e) {
        logger.atSevere().withCause(e).log("Cannot handle message '%s'", event);
        subscriberMetrics.incrementSubscriberFailedToConsumeMessage();
      }
    }
    subscriberMetrics.updateReplicationStatusMetrics(event);
  }

  private void tryAckAndMarkAsConsumed(
      Event event, MessageAcknowledgement<Event> ack, boolean isAutoAck) {
    if (isAutoAck || tryAck(event, ack)) {
      subscriberMetrics.incrementSubscriberConsumedMessage();
    }
  }

  private boolean tryAck(Event event, MessageAcknowledgement<Event> ack) {
    try {
      ack.ack(event);
      return true;
    } catch (MessageAcknowledgementException e) {
      logger.atSevere().withCause(e).log("Cannot ack message '%s'", event);
      subscriberMetrics.incrementSubscriberFailedToAckMessage();
      return false;
    }
  }
}
