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
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.IndexEventRouter;
import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.change.ChangeFinder;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class IndexEventSubscriber extends AbstractSubcriber {
  private final ProjectsFilter projectsFilter;
  private final ChangeFinder changeFinder;

  @Inject
  public IndexEventSubscriber(
      IndexEventRouter eventRouter,
      DynamicSet<DroppedEventListener> droppedEventListeners,
      @GerritInstanceId String instanceId,
      MessageLogger msgLog,
      SubscriberMetrics subscriberMetrics,
      Configuration cfg,
      ProjectsFilter projectsFilter,
      ChangeFinder changeFinder) {
    super(eventRouter, droppedEventListeners, instanceId, msgLog, subscriberMetrics, cfg);
    this.projectsFilter = projectsFilter;
    this.changeFinder = changeFinder;
  }

  @Override
  protected EventTopic getTopic() {
    return EventTopic.INDEX_TOPIC;
  }

  @Override
  protected Boolean shouldConsumeEvent(Event event) {
    if (event instanceof ChangeIndexEvent changeIndexEvent) {
      String projectName = changeIndexEvent.projectName;
      if (isDeletedChangeWithEmptyProject(changeIndexEvent)) {
        projectName = findProjectFromChangeId(changeIndexEvent.changeId).orElse(projectName);
      }
      return projectsFilter.matches(projectName);
    }
    if (event instanceof ProjectIndexEvent projectIndexEvent) {
      return projectsFilter.matches(projectIndexEvent.projectName);
    }
    return true;
  }

  private boolean isDeletedChangeWithEmptyProject(ChangeIndexEvent changeIndexEvent) {
    return changeIndexEvent.deleted && changeIndexEvent.projectName.isEmpty();
  }

  private Optional<String> findProjectFromChangeId(int changeId) {
    return changeFinder.findOne(Change.id(changeId)).map(c -> c.getChange().getProject().get());
  }
}
