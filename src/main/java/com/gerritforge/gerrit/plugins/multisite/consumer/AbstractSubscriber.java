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
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ForwardedEventManualAckingRouter;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ForwardedEventRouter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import java.io.IOException;

public abstract class AbstractSubscriber {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private enum AckMode {
    SUBSCRIBER_MANAGED,
    ROUTER_MANAGED
  }

  private final ForwardedEventRouter eventRouter;
  private final DynamicSet<DroppedEventListener> droppedEventListeners;
  private final String instanceId;
  private final MessageLogger msgLog;
  private SubscriberMetrics subscriberMetrics;
  private final String topic;

  @FunctionalInterface
  public interface RequeueEventAction {

    boolean requeue(Event t);

    RequeueEventAction NO_ACTION = (e) -> true;
  }

  public AbstractSubscriber(
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
    this.topic = getTopic().topic(cfg);
  }

  protected abstract EventTopic getTopic();

  protected abstract Boolean shouldConsumeEvent(Event event);

  public AckAwareConsumer<Event> getConsumer(boolean isAutoAck) {
    return (event, messageAcknowledgement) ->
        processRecord(event, messageAcknowledgement, isAutoAck, AckMode.SUBSCRIBER_MANAGED, null);
  }

  public AckAwareConsumer<Event> getManualAckConsumer(RequeueEventAction requeueAction) {
    if (!(eventRouter instanceof ForwardedEventManualAckingRouter<?>)) {
      throw new IllegalStateException("Router does not support manual acknowledgement");
    }
    return (event, messageAcknowledgement) ->
        processRecord(
            event,
            messageAcknowledgement, /* isAutoAck */
            false,
            AckMode.ROUTER_MANAGED,
            requeueAction);
  }

  private void processRecord(
      Event event,
      MessageAcknowledgement<Event> messageAcknowledgement,
      boolean isAutoAck,
      AckMode ackMode,
      @Nullable RequeueEventAction requeueAction) {
    if (ackMode == AckMode.ROUTER_MANAGED) {
      Preconditions.checkNotNull(requeueAction, "requeueAction is mandatory in ROUTER_MANAGED ack");
    }

    String sourceInstanceId = event.instanceId;

    if (Strings.isNullOrEmpty(sourceInstanceId)) {
      logger.atWarning().log("Dropping event %s because sourceInstanceId cannot be null", event);
      handleDroppedEvent(event, messageAcknowledgement, isAutoAck, ackMode);
    } else if (instanceId.equals(sourceInstanceId)) {
      logger.atFiner().log("Dropping event %s produced by our instanceId %s", event, instanceId);
      handleDroppedEvent(event, messageAcknowledgement, isAutoAck, ackMode);
    } else if (!shouldConsumeEvent(event)) {
      handleDroppedEvent(event, messageAcknowledgement, isAutoAck, ackMode);
    } else {
      try {
        msgLog.log(MessageLogger.Direction.CONSUME, topic, event);

        switch (ackMode) {
          case SUBSCRIBER_MANAGED:
            route(event, messageAcknowledgement, isAutoAck);
            break;

          case ROUTER_MANAGED:
            if (!routeManaged(event, messageAcknowledgement) && requeueAction.requeue(event)) {
              messageAcknowledgement.ack(event);
            }
            break;
        }
      } catch (IOException | PermissionBackendException | CacheNotFoundException e) {
        logger.atSevere().withCause(e).log("Cannot handle message '%s'", event);
        subscriberMetrics.incrementSubscriberFailedToConsumeMessage();
      } catch (MessageAcknowledgementException e) {
        logger.atSevere().withCause(e).log("Cannot ack message '%s'", event);
        subscriberMetrics.incrementSubscriberFailedToAckMessage();
      }
    }
    subscriberMetrics.updateReplicationStatusMetrics(event);
  }

  private void handleDroppedEvent(
      Event event,
      MessageAcknowledgement<Event> messageAcknowledgement,
      boolean isAutoAck,
      AckMode ackMode) {
    try {
      droppedEventListeners.forEach(l -> l.onEventDropped(event));
    } finally {
      if (ackMode == AckMode.ROUTER_MANAGED) {
        tryRouterAck(event, messageAcknowledgement);
      } else {
        tryAckAndMarkAsConsumed(event, messageAcknowledgement, isAutoAck);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void route(Event event, MessageAcknowledgement<Event> ack, boolean isAutoAck)
      throws IOException, PermissionBackendException, CacheNotFoundException {
    eventRouter.route(event);
    tryAckAndMarkAsConsumed(event, ack, isAutoAck);
  }

  private boolean routeManaged(Event event, MessageAcknowledgement<Event> ack)
      throws IOException, PermissionBackendException, CacheNotFoundException {
    boolean routeSuccessful =
        (((ForwardedEventManualAckingRouter<Event>) eventRouter).route(event, ack));
    subscriberMetrics.incrementSubscriberConsumedMessage();
    return routeSuccessful;
  }

  @SuppressWarnings("unchecked")
  private void tryRouterAck(Event event, MessageAcknowledgement<Event> ack) {
    try {
      ((ForwardedEventManualAckingRouter<Event>) eventRouter).ack(event, ack);
      subscriberMetrics.incrementSubscriberConsumedMessage();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Cannot handle message '%s'", event);
      subscriberMetrics.incrementSubscriberFailedToConsumeMessage();
    } catch (MessageAcknowledgementException e) {
      logger.atSevere().withCause(e).log("Cannot ack message '%s'", event);
      subscriberMetrics.incrementSubscriberFailedToAckMessage();
    }
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
