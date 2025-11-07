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

import com.google.inject.Inject;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.broker.BrokerApiWrapper;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwarderTask;
import com.gerritforge.gerrit.plugins.multisite.forwarder.IndexEventForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;

public class BrokerIndexEventForwarder extends BrokerForwarder implements IndexEventForwarder {

  @Inject
  BrokerIndexEventForwarder(BrokerApiWrapper broker, Configuration cfg) {
    super(broker, cfg);
  }

  @Override
  public boolean index(ForwarderTask task, IndexEvent event) {
    return send(task, EventTopic.INDEX_TOPIC, event);
  }

  @Override
  public boolean batchIndex(ForwarderTask task, IndexEvent event) {
    return send(task, EventTopic.BATCH_INDEX_TOPIC, event);
  }
}
