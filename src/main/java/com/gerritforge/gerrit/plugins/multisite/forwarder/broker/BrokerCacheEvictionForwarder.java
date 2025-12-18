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

package com.gerritforge.gerrit.plugins.multisite.forwarder.broker;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.broker.BrokerApiWrapper;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEvictionForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwarderTask;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BrokerCacheEvictionForwarder extends BrokerForwarder
    implements CacheEvictionForwarder {

  @Inject
  BrokerCacheEvictionForwarder(BrokerApiWrapper broker, Configuration cfg) {
    super(broker, cfg);
  }

  @Override
  public boolean evict(ForwarderTask task, CacheEvictionEvent event) {
    return send(task, EventTopic.CACHE_TOPIC, event);
  }
}
