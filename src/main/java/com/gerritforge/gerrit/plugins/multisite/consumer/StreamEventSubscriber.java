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
import com.gerritforge.gerrit.globalrefdb.validation.ProjectsFilter;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.StreamEventRouter;

@Singleton
public class StreamEventSubscriber extends AbstractSubcriber {
  private final ProjectsFilter projectsFilter;

  @Inject
  public StreamEventSubscriber(
      StreamEventRouter eventRouter,
      DynamicSet<DroppedEventListener> droppedEventListeners,
      @GerritInstanceId String instanceId,
      MessageLogger msgLog,
      SubscriberMetrics subscriberMetrics,
      Configuration cfg,
      ProjectsFilter projectsFilter) {
    super(eventRouter, droppedEventListeners, instanceId, msgLog, subscriberMetrics, cfg);
    this.projectsFilter = projectsFilter;
  }

  @Override
  protected EventTopic getTopic() {
    return EventTopic.STREAM_EVENT_TOPIC;
  }

  @Override
  protected Boolean shouldConsumeEvent(Event event) {
    if (event instanceof ProjectEvent projectEvent) {
      return projectsFilter.matches(projectEvent.getProjectNameKey().get());
    }
    return true;
  }
}
