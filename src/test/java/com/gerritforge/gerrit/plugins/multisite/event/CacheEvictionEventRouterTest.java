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

import static com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent.GERRIT_PLUGIN_NAME;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEntry;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheKeyJsonParser;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedCacheEvictionHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.CacheEvictionEventRouter;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.Weigher;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PrivateInternals_DynamicMapImpl;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import java.time.Duration;
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

  private DynamicMap<CacheDef<?, ?>> getDynamicMap() {
    cacheDefMap =
        (PrivateInternals_DynamicMapImpl<CacheDef<?, ?>>) DynamicMap.<CacheDef<?, ?>>emptyMap();

    CacheDef<java.lang.String, Object> keyTypeCacheDef =
        new CacheEvictionEventRouterTest.TestCacheDef<>(
            CACHE_NAME, java.lang.String.class, Object.class);
    cacheDefMap.put("gerrit", CACHE_NAME, Providers.of(keyTypeCacheDef));
    return cacheDefMap;
  }

  @Before
  public void setUp() {
    router =
        new CacheEvictionEventRouter(
            cacheEvictionHandler, new CacheKeyJsonParser(gson, getDynamicMap()));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEviction() throws Exception {
    final CacheEvictionEvent event =
        new CacheEvictionEvent(GERRIT_PLUGIN_NAME, CACHE_NAME, "key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(event.cacheName, event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event =
        new CacheEvictionEvent(GERRIT_PLUGIN_NAME, CACHE_NAME, "some/key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(event.cacheName, event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ProjectCacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event =
        new CacheEvictionEvent(GERRIT_PLUGIN_NAME, Constants.PROJECTS, "some/project", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler)
        .evict(CacheEntry.from(event.cacheName, Project.nameKey((String) event.key)));
  }

  private static class TestCacheDef<K, V> implements CacheDef<K, V> {
    private final String name;
    private final TypeLiteral<K> keyType;
    private final TypeLiteral<V> valueType;

    TestCacheDef(String name, Class<K> keyClass, Class<V> valueClass) {
      this.name = name;
      this.keyType = TypeLiteral.get(keyClass);
      this.valueType = TypeLiteral.get(valueClass);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String configKey() {
      return name;
    }

    @Override
    public TypeLiteral<K> keyType() {
      return keyType;
    }

    @Override
    public TypeLiteral<V> valueType() {
      return valueType;
    }

    @Override
    public long maximumWeight() {
      return 0;
    }

    @Override
    public Duration expireAfterWrite() {
      return null;
    }

    @Override
    public Duration expireFromMemoryAfterAccess() {
      return null;
    }

    @Override
    public Duration refreshAfterWrite() {
      return null;
    }

    @Override
    public Weigher<K, V> weigher() {
      return null;
    }

    @Override
    public CacheLoader<K, V> loader() {
      return null;
    }
  }
}
