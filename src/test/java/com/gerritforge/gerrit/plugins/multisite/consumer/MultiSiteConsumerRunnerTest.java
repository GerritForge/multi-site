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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.AckAwareConsumer;
import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.Configuration.Broker;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.Event;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiSiteConsumerRunnerTest {
  private static final String TOPIC = "index-topic";
  private static final String GROUP_ID = "multi-site-group";

  @Mock private BrokerApi brokerApi;
  @Mock private Configuration cfg;
  @Mock private Broker brokerCfg;
  @Mock private AbstractSubcriber subscriber;
  @Mock private AckAwareConsumer<Event> consumer;

  @Test
  public void shouldSubscribeWithConfiguredGroupId() {
    configureSubscriber(Optional.of(GROUP_ID));

    runner().start();

    verify(brokerApi).receiveAsync(TOPIC, GROUP_ID, consumer);
    verify(brokerApi, never()).receiveAsync(TOPIC, consumer);
  }

  @Test
  public void shouldUseBrokerDefaultWhenGroupIdIsNotConfigured() {
    configureSubscriber(Optional.empty());

    runner().start();

    verify(brokerApi).receiveAsync(TOPIC, consumer);
    verify(brokerApi, never()).receiveAsync(TOPIC, GROUP_ID, consumer);
  }

  private void configureSubscriber(Optional<String> groupId) {
    when(brokerApi.isAutoAck()).thenReturn(true);
    when(cfg.broker()).thenReturn(brokerCfg);
    when(brokerCfg.getGroupId()).thenReturn(groupId);
    when(brokerCfg.getTopic(EventTopic.INDEX_TOPIC.topicAliasKey(), "GERRIT.EVENT.INDEX"))
        .thenReturn(TOPIC);
    when(subscriber.getTopic()).thenReturn(EventTopic.INDEX_TOPIC);
    when(subscriber.getConsumer(true)).thenReturn(consumer);
  }

  private MultiSiteConsumerRunner runner() {
    DynamicSet<AbstractSubcriber> consumers = new DynamicSet<>();
    consumers.add("multi-site", subscriber);
    return new MultiSiteConsumerRunner(
        DynamicItem.itemOf(BrokerApi.class, brokerApi), consumers, cfg);
  }
}
