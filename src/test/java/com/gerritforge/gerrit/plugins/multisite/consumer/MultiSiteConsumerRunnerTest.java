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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.AckAwareConsumer;
import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventsBrokerConfiguration;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.Configuration.Broker;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.Event;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiSiteConsumerRunnerTest {
  private static final String TOPIC = "index-topic";
  private static final String CACHE_TOPIC = "cache-topic";
  private static final String GROUP_ID = "multi-site-group";
  private static final List<String> INDEX_PARTITIONS =
      List.of(
          ChangeIndexEvent.TYPE,
          AccountIndexEvent.TYPE,
          ProjectIndexEvent.TYPE,
          GroupIndexEvent.TYPE);

  @Mock private BrokerApi brokerApi;
  @Mock private Configuration cfg;
  @Mock private Broker brokerCfg;
  @Mock private EventsBrokerConfiguration eventsBrokerConfiguration;
  @Mock private AbstractSubcriber subscriber;
  @Mock private AbstractSubcriber cacheSubscriber;
  @Mock private AckAwareConsumer<Event> consumer;
  @Mock private AckAwareConsumer<Event> cacheConsumer;

  @Test
  public void shouldSubscribeWithConfiguredGroupId() {
    configureSubscriber(Optional.of(GROUP_ID));

    runner().start();

    verify(brokerApi).receiveAsync(TOPIC, GROUP_ID, consumer);
    verify(brokerApi, never()).receiveAsync(TOPIC, consumer);
  }

  @Test
  public void shouldSubscribeMultipleTopicsWithConfiguredGroupId() {
    configureSubscriber(Optional.of(GROUP_ID));
    when(brokerCfg.getTopic(EventTopic.CACHE_TOPIC.topicAliasKey(), "GERRIT.EVENT.CACHE"))
        .thenReturn(CACHE_TOPIC);
    when(cacheSubscriber.getTopic()).thenReturn(EventTopic.CACHE_TOPIC);
    when(cacheSubscriber.getConsumer(true)).thenReturn(cacheConsumer);

    DynamicSet<AbstractSubcriber> consumers = new DynamicSet<>();
    consumers.add("multi-site", subscriber);
    consumers.add("multi-site", cacheSubscriber);
    new MultiSiteConsumerRunner(
            DynamicItem.itemOf(BrokerApi.class, brokerApi),
            consumers,
            cfg,
            eventsBrokerConfiguration)
        .start();

    verify(brokerApi).receiveAsync(TOPIC, GROUP_ID, consumer);
    verify(brokerApi).receiveAsync(CACHE_TOPIC, GROUP_ID, cacheConsumer);
    verify(brokerApi, never()).receiveAsync(TOPIC, consumer);
    verify(brokerApi, never()).receiveAsync(CACHE_TOPIC, cacheConsumer);
  }

  @Test
  public void shouldUseBrokerDefaultWhenGroupIdIsNotConfigured() {
    configureSubscriber(Optional.empty());

    runner().start();

    verify(brokerApi).receiveAsync(TOPIC, consumer);
    verify(brokerApi, never()).receiveAsync(TOPIC, GROUP_ID, consumer);
  }

  @Test
  public void shouldSubscribeToConfiguredIndexPartitions() {
    configureSubscriber(Optional.of(GROUP_ID), INDEX_PARTITIONS);

    runner().start();

    INDEX_PARTITIONS.forEach(
        partition ->
            verify(brokerApi)
                .receiveAsyncWithPartition(TOPIC, partition, groupId(partition), consumer));
    verify(brokerApi, never()).receiveAsync(eq(TOPIC), any());
  }

  @Test
  public void shouldRequireGroupIdForPartitionSubscriptions() {
    configureSubscriber(Optional.empty(), INDEX_PARTITIONS);

    assertThrows(IllegalStateException.class, () -> runner().start());

    verify(brokerApi, never()).receiveAsyncWithPartition(any(), any(), any(), any());
  }

  @Test
  public void shouldFailWhenAnExpectedIndexPartitionIsNotConfigured() {
    configureSubscriber(
        Optional.of(GROUP_ID),
        List.of(ChangeIndexEvent.TYPE, ProjectIndexEvent.TYPE, GroupIndexEvent.TYPE));
    doThrow(new IllegalArgumentException("missing partition"))
        .when(brokerApi)
        .receiveAsyncWithPartition(
            TOPIC, AccountIndexEvent.TYPE, groupId(AccountIndexEvent.TYPE), consumer);

    assertThrows(IllegalArgumentException.class, () -> runner().start());
  }

  private static String groupId(String partition) {
    return GROUP_ID + "-" + partition;
  }

  private void configureSubscriber(Optional<String> groupId, List<String> partitions) {
    when(brokerApi.isAutoAck()).thenReturn(true);
    when(cfg.broker()).thenReturn(brokerCfg);
    when(brokerCfg.getGroupId()).thenReturn(groupId);
    when(brokerCfg.getTopic(EventTopic.INDEX_TOPIC.topicAliasKey(), "GERRIT.EVENT.INDEX"))
        .thenReturn(TOPIC);
    when(eventsBrokerConfiguration.getPartitionsForTopic(TOPIC)).thenReturn(partitions);
    when(subscriber.getTopic()).thenReturn(EventTopic.INDEX_TOPIC);
    when(subscriber.getConsumer(true)).thenReturn(consumer);
  }

  private void configureSubscriber(Optional<String> groupId) {
    configureSubscriber(groupId, List.of());
  }

  private MultiSiteConsumerRunner runner() {
    DynamicSet<AbstractSubcriber> consumers = new DynamicSet<>();
    consumers.add("multi-site", subscriber);
    return new MultiSiteConsumerRunner(
        DynamicItem.itemOf(BrokerApi.class, brokerApi), consumers, cfg, eventsBrokerConfiguration);
  }
}
