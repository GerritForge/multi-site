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

package com.gerritforge.gerrit.plugins.multisite.event;

import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisherConfig;
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisherModule;
import com.google.gerrit.extensions.events.GitBatchRefUpdateListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.OptionalBinder;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.EventTopic;
import com.gerritforge.gerrit.plugins.multisite.validation.ProjectVersionRefUpdate;
import com.gerritforge.gerrit.plugins.multisite.validation.ProjectVersionRefUpdateImpl;

public class EventModule extends LifecycleModule {
  private final Configuration configuration;

  @Inject
  public EventModule(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(StreamEventPublisherConfig.class)
        .toInstance(
            new StreamEventPublisherConfig(
                EventTopic.STREAM_EVENT_TOPIC.topic(configuration),
                configuration.broker().getStreamEventPublishTimeout()));

    install(new StreamEventPublisherModule());

    OptionalBinder<ProjectVersionRefUpdate> projectVersionRefUpdateBinder =
        OptionalBinder.newOptionalBinder(binder(), ProjectVersionRefUpdate.class);
    if (configuration.getSharedRefDbConfiguration().getSharedRefDb().isEnabled()) {
      if (configuration.replicationLagEnabled()) {
        DynamicSet.bind(binder(), GitBatchRefUpdateListener.class)
            .to(ProjectVersionRefUpdateImpl.class);
      }
      projectVersionRefUpdateBinder.setBinding().to(ProjectVersionRefUpdateImpl.class);
    }
  }
}
