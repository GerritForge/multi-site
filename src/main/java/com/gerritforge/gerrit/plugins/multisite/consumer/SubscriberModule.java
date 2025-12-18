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

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.MultiSiteEvent;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;

public class SubscriberModule extends LifecycleModule {

  @Override
  protected void configure() {
    MultiSiteEvent.registerEventTypes();

    DynamicSet.setOf(binder(), AbstractSubcriber.class);
    DynamicSet.setOf(binder(), DroppedEventListener.class);

    DynamicSet.bind(binder(), AbstractSubcriber.class).to(IndexEventSubscriber.class);
    DynamicSet.bind(binder(), AbstractSubcriber.class).to(BatchIndexEventSubscriber.class);
    DynamicSet.bind(binder(), AbstractSubcriber.class).to(StreamEventSubscriber.class);
    DynamicSet.bind(binder(), AbstractSubcriber.class).to(CacheEvictionEventSubscriber.class);
    DynamicSet.bind(binder(), AbstractSubcriber.class).to(ProjectUpdateEventSubscriber.class);
  }
}
