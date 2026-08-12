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

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

public class RouterModule extends LifecycleModule {

  private final Configuration.Index indexConfig;

  @Inject
  public RouterModule(Configuration.Index indexConfig) {
    this.indexConfig = indexConfig;
  }

  public static final TypeLiteral<ForwardedIndexingHandler<?, ? extends IndexEvent>> INDEX_HANDLER =
      new TypeLiteral<>() {};

  @Override
  protected void configure() {
    DynamicMap.mapOf(binder(), INDEX_HANDLER);

    indexConfig
        .synchronize()
        .forEach(
            (type, handler) -> bind(INDEX_HANDLER).annotatedWith(Exports.named(type)).to(handler));

    bind(CacheEvictionEventRouter.class).in(Scopes.SINGLETON);
    bind(ProjectListUpdateRouter.class).in(Scopes.SINGLETON);
  }
}
