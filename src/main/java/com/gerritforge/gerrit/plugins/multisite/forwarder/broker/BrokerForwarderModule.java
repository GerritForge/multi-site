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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEvictionForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.IndexEventForwarder;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ProjectListUpdateForwarder;

public class BrokerForwarderModule extends LifecycleModule {
  @Override
  protected void configure() {
    DynamicSet.bind(binder(), IndexEventForwarder.class).to(BrokerIndexEventForwarder.class);
    DynamicSet.bind(binder(), CacheEvictionForwarder.class).to(BrokerCacheEvictionForwarder.class);
    DynamicSet.bind(binder(), ProjectListUpdateForwarder.class)
        .to(BrokerProjectListUpdateForwarder.class);
  }
}
