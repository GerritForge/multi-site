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
import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventsBrokerConfiguration;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class MultiSiteConsumerRunner implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final List<String> INDEX_PARTITIONS =
      List.of(
          ChangeIndexEvent.TYPE,
          AccountIndexEvent.TYPE,
          ProjectIndexEvent.TYPE,
          GroupIndexEvent.TYPE);

  private final DynamicSet<AbstractSubcriber> consumers;
  private DynamicItem<BrokerApi> brokerApi;
  private Configuration cfg;
  private final EventsBrokerConfiguration eventsBrokerConfiguration;

  @Inject
  public MultiSiteConsumerRunner(
      DynamicItem<BrokerApi> brokerApi,
      DynamicSet<AbstractSubcriber> consumers,
      Configuration cfg,
      EventsBrokerConfiguration eventsBrokerConfiguration) {
    this.consumers = consumers;
    this.brokerApi = brokerApi;
    this.cfg = cfg;
    this.eventsBrokerConfiguration = eventsBrokerConfiguration;
  }

  @Override
  public void start() {
    logger.atInfo().log("starting consumers");
    consumers.forEach(this::subscribe);
  }

  @Override
  public void stop() {}

  private void subscribe(AbstractSubcriber subscriber) {
    BrokerApi broker = brokerApi.get();
    String topic = subscriber.getTopic().topic(cfg);
    AckAwareConsumer<Event> consumer = subscriber.getConsumer(broker.isAutoAck());
    if (isPartitionAwareIndexTopic(subscriber, topic)) {
      String groupId =
          cfg.broker()
              .getGroupId()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "broker.groupId is required for partition-aware subscriptions"));
      INDEX_PARTITIONS.forEach(
          partition -> broker.receiveAsyncWithPartition(topic, partition, groupId, consumer));
      return;
    }

    Optional<String> groupId = cfg.broker().getGroupId();
    if (groupId.isPresent()) {
      broker.receiveAsync(topic, groupId.get(), consumer);
    } else {
      broker.receiveAsync(topic, consumer);
    }
  }

  private boolean isPartitionAwareIndexTopic(AbstractSubcriber consumer, String topic) {
    return consumer.getTopic() == EventTopic.INDEX_TOPIC
        && !eventsBrokerConfiguration.getPartitionsForTopic(topic).isEmpty();
  }
}
