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

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.CacheEvictionEventRouter;

@Singleton
public class CacheEvictionEventSubscriber extends AbstractSubcriber {
  @Inject
  public CacheEvictionEventSubscriber(
      CacheEvictionEventRouter eventRouter,
      DynamicSet<DroppedEventListener> droppedEventListeners,
      @GerritInstanceId String instanceId,
      MessageLogger msgLog,
      SubscriberMetrics subscriberMetrics,
      Configuration cfg) {

    super(eventRouter, droppedEventListeners, instanceId, msgLog, subscriberMetrics, cfg);
  }

  @Override
  protected EventTopic getTopic() {
    return EventTopic.CACHE_TOPIC;
  }

  @Override
  protected Boolean shouldConsumeEvent(Event event) {
    return true;
  }
}
