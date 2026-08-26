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

package com.gerritforge.gerrit.plugins.multisite.broker;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.BrokerApiLoggingWrapper;
import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import java.util.concurrent.Executor;

public class BrokerApiWrapper extends BrokerApiLoggingWrapper {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final Executor executor;
  private final BrokerMetrics metrics;
  private final String nodeInstanceId;

  @Inject
  public BrokerApiWrapper(
      @BrokerExecutor Executor executor,
      DynamicItem<BrokerApi> apiDelegate,
      BrokerMetrics metrics,
      MessageLogger msgLog,
      @GerritInstanceId String instanceId) {
    super(apiDelegate, msgLog);
    this.executor = executor;
    this.metrics = metrics;
    this.nodeInstanceId = instanceId;
  }

  public boolean sendSync(String topic, Event event) {
    try {
      return send(topic, event).get();
    } catch (Throwable e) {
      log.atSevere().withCause(e).log("Failed to publish event '%s' to topic '%s'", event, topic);
      metrics.incrementBrokerFailedToPublishMessage();
      return false;
    }
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    SettableFuture<Boolean> resultFuture = SettableFuture.create();
    if (!nodeInstanceId.equals(message.instanceId)) {
      resultFuture.set(true);
      return resultFuture;
    }

    if (Strings.isNullOrEmpty(message.instanceId)) {
      log.atWarning().log(
          "Dropping event '%s' because event instance id cannot be null or empty", message);
      resultFuture.set(true);
      return resultFuture;
    }

    ListenableFuture<Boolean> resfultF = super.send(topic, message);
    Futures.addCallback(
        resfultF,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              metrics.incrementBrokerPublishedMessage();
            } else {
              metrics.incrementBrokerFailedToPublishMessage();
            }
          }

          @Override
          public void onFailure(Throwable throwable) {
            metrics.incrementBrokerFailedToPublishMessage();
          }
        },
        executor);

    return resfultF;
  }

  public ListenableFuture<Boolean> requeue(String topic, Event message) {
    try {
      ListenableFuture<Boolean> resfultF =
          super.send(topic, message, MessageLogger.Direction.REQUEUE);
      Futures.addCallback(
          resfultF,
          new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
              if (result) {
                metrics.incrementBrokerRequeuedMessage(topic, message.getType());
              } else {
                metrics.incrementBrokerFailedToRequeueMessage(topic, message.getType());
              }
            }

            @Override
            public void onFailure(Throwable throwable) {
              metrics.incrementBrokerFailedToRequeueMessage(topic, message.getType());
            }
          },
          executor);

      return resfultF;
    } catch (RuntimeException e) {
      metrics.incrementBrokerFailedToRequeueMessage(topic, message.getType());
      throw e;
    }
  }
}
