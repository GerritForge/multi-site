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

import static com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic.PROJECT_LIST_TOPIC;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.broker.BrokerApiWrapper;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwarderTask;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ProjectListUpdateForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectListUpdateEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BrokerProjectListUpdateForwarder extends BrokerForwarder
    implements ProjectListUpdateForwarder {

  @Inject
  BrokerProjectListUpdateForwarder(BrokerApiWrapper broker, Configuration cfg) {
    super(broker, cfg);
  }

  @Override
  public boolean updateProjectList(ForwarderTask task, ProjectListUpdateEvent event) {
    return send(task, PROJECT_LIST_TOPIC, event);
  }
}
