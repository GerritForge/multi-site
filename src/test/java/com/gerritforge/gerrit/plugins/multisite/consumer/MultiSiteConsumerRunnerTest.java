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
  private static final String INDEX_TOPIC = "index-topic";
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
  @Mock private AbstractSubcriber indexSubscriber;
  @Mock private AckAwareConsumer<Event> consumer;

  @Test
  public void shouldSubscribeWithConfiguredGroupId() {
    configureIndexSubscriber(Optional.of(GROUP_ID), List.of());

    runner().start();

    verify(brokerApi).receiveAsync(INDEX_TOPIC, GROUP_ID, consumer);
    verify(brokerApi, never()).receiveAsync(INDEX_TOPIC, consumer);
  }

  @Test
  public void shouldUseBrokerDefaultWhenGroupIdIsNotConfigured() {
    configureIndexSubscriber(Optional.empty(), List.of());

    runner().start();

    verify(brokerApi).receiveAsync(INDEX_TOPIC, consumer);
    verify(brokerApi, never()).receiveAsync(INDEX_TOPIC, GROUP_ID, consumer);
  }

  @Test
  public void shouldSubscribeToConfiguredIndexPartitions() {
    configureIndexSubscriber(Optional.of(GROUP_ID), INDEX_PARTITIONS);

    runner().start();

    INDEX_PARTITIONS.forEach(
        partition ->
            verify(brokerApi)
                .receiveAsyncWithPartition(INDEX_TOPIC, partition, GROUP_ID, consumer));
    verify(brokerApi, never()).receiveAsync(eq(INDEX_TOPIC), any());
  }

  @Test
  public void shouldRequireGroupIdForPartitionSubscriptions() {
    configureIndexSubscriber(Optional.empty(), INDEX_PARTITIONS);

    assertThrows(IllegalStateException.class, () -> runner().start());

    verify(brokerApi, never()).receiveAsyncWithPartition(any(), any(), any(), any());
  }

  @Test
  public void shouldFailWhenAnExpectedIndexPartitionIsNotConfigured() {
    configureIndexSubscriber(
        Optional.of(GROUP_ID),
        List.of(ChangeIndexEvent.TYPE, ProjectIndexEvent.TYPE, GroupIndexEvent.TYPE));
    doThrow(new IllegalArgumentException("missing partition"))
        .when(brokerApi)
        .receiveAsyncWithPartition(INDEX_TOPIC, AccountIndexEvent.TYPE, GROUP_ID, consumer);

    assertThrows(IllegalArgumentException.class, () -> runner().start());
  }

  private void configureIndexSubscriber(Optional<String> groupId, List<String> partitions) {
    when(brokerApi.isAutoAck()).thenReturn(true);
    when(cfg.broker()).thenReturn(brokerCfg);
    when(brokerCfg.getGroupId()).thenReturn(groupId);
    when(brokerCfg.getTopic(EventTopic.INDEX_TOPIC.topicAliasKey(), "GERRIT.EVENT.INDEX"))
        .thenReturn(INDEX_TOPIC);
    when(eventsBrokerConfiguration.getPartitionsForTopic(INDEX_TOPIC)).thenReturn(partitions);
    when(indexSubscriber.getTopic()).thenReturn(EventTopic.INDEX_TOPIC);
    when(indexSubscriber.getConsumer(true)).thenReturn(consumer);
  }

  private MultiSiteConsumerRunner runner() {
    DynamicSet<AbstractSubcriber> consumers = new DynamicSet<>();
    consumers.add("multi-site", indexSubscriber);
    return new MultiSiteConsumerRunner(
        DynamicItem.itemOf(BrokerApi.class, brokerApi), consumers, cfg, eventsBrokerConfiguration);
  }
}
