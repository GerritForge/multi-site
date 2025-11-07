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

package com.gerritforge.gerrit.plugins.multisite.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.events.NewProjectCreatedListener;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.cache.CacheRemovalListener;
import com.gerritforge.gerrit.plugins.multisite.ExecutorProvider;
import java.util.concurrent.Executor;

public class CacheModule extends LifecycleModule {

  private final Class<? extends ExecutorProvider> cacheExecutorProviderClass;

  public CacheModule() {
    this(CacheExecutorProvider.class);
  }

  @VisibleForTesting
  public CacheModule(Class<? extends ExecutorProvider> cacheExecutorProviderClass) {
    this.cacheExecutorProviderClass = cacheExecutorProviderClass;
  }

  @Override
  protected void configure() {
    bind(Executor.class).annotatedWith(CacheExecutor.class).toProvider(cacheExecutorProviderClass);
    listener().to(CacheExecutorProvider.class);
    DynamicSet.bind(binder(), CacheRemovalListener.class).to(CacheEvictionHandler.class);
    DynamicSet.bind(binder(), NewProjectCreatedListener.class).to(ProjectListUpdateHandler.class);
    DynamicSet.bind(binder(), ProjectDeletedListener.class).to(ProjectListUpdateHandler.class);
  }
}
