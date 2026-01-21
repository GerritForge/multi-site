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

import static com.google.gerrit.extensions.registration.PluginName.GERRIT;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.plugins.multisite.NoOpCacheKeyDef;
import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEntry;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheKeyJsonParser;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CachePluginAndNameRecord;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedCacheEvictionHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.CacheEvictionEventRouter;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PrivateInternals_DynamicMapImpl;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheEvictionEventRouterTest {

  private static final String INSTANCE_ID = "instance-id";
  private static final String CACHE_NAME = "test-cache";
  private static Gson gson = new EventGsonProvider().get();
  private CacheEvictionEventRouter router;
  @Mock private ForwardedCacheEvictionHandler cacheEvictionHandler;

  private PrivateInternals_DynamicMapImpl<CacheDef<?, ?>> cacheDefMap;

  @Before
  public void setUp() {
    cacheDefMap =
        (PrivateInternals_DynamicMapImpl<CacheDef<?, ?>>) DynamicMap.<CacheDef<?, ?>>emptyMap();
    defineCache(GERRIT, CACHE_NAME, String.class);
    defineCache(GERRIT, Constants.PROJECTS, Project.class);
    router =
        new CacheEvictionEventRouter(
            cacheEvictionHandler, new CacheKeyJsonParser(gson, cacheDefMap));
  }

  private void defineCache(String pluginName, String cacheName, Class<?> keyRawType) {
    RegistrationHandle unused =
        cacheDefMap.put(
            pluginName,
            cacheName,
            Providers.of(new NoOpCacheKeyDef<>(cacheName, keyRawType, Object.class)));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEviction() throws Exception {
    final CacheEvictionEvent event = new CacheEvictionEvent(CACHE_NAME, "key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(CachePluginAndNameRecord.from(event.cacheName), event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event = new CacheEvictionEvent(CACHE_NAME, "some/key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(CachePluginAndNameRecord.from(event.cacheName), event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ProjectCacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event =
        new CacheEvictionEvent(Constants.PROJECTS, "some/project", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler)
        .evict(CacheEntry.from(CachePluginAndNameRecord.from(event.cacheName), Project.nameKey((String) event.key)));
  }
}
