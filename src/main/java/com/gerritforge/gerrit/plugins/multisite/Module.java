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

package com.gerritforge.gerrit.plugins.multisite;

import com.gerritforge.gerrit.globalrefdb.validation.LibModule;
import com.gerritforge.gerrit.plugins.multisite.broker.BrokerModule;
import com.gerritforge.gerrit.plugins.multisite.cache.CacheModule;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwarderModule;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.KeyWrapperAdapterInjector;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.RouterModule;
import com.gerritforge.gerrit.plugins.multisite.index.IndexModule;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.spi.Message;
import java.util.Collection;

public class Module extends LifecycleModule {
  private Configuration config;

  @Inject
  public Module(Configuration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    boolean brokerRouterNeeded = false;

    Collection<Message> validationErrors = config.validate();
    if (!validationErrors.isEmpty()) {
      throw new CreationException(validationErrors);
    }

    install(new LibModule());

    install(new ForwarderModule());
    bind(KeyWrapperAdapterInjector.class).asEagerSingleton();

    if (config.cache().synchronize()) {
      install(new CacheModule());
      brokerRouterNeeded = true;
    }
    if (config.event().synchronize()) {
      brokerRouterNeeded = true;
    }
    if (!config.index().synchronize().isEmpty()) {
      install(new IndexModule());
      brokerRouterNeeded = true;
    }

    if (brokerRouterNeeded) {
      install(new BrokerModule());
      install(new RouterModule(config.index()));
    }
  }
}
