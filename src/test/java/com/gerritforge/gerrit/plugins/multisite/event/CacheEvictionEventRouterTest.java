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

import static org.mockito.Mockito.verify;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEntry;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheKeyJsonParser;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedCacheEvictionHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.CacheEvictionEventRouter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheEvictionEventRouterTest {

  private static final String INSTANCE_ID = "instance-id";
  private static Gson gson = new EventGsonProvider().get();
  private CacheEvictionEventRouter router;
  @Mock private ForwardedCacheEvictionHandler cacheEvictionHandler;

  @Before
  public void setUp() {
    router = new CacheEvictionEventRouter(cacheEvictionHandler, new CacheKeyJsonParser(gson));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEviction() throws Exception {
    final CacheEvictionEvent event = new CacheEvictionEvent("cache", "key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(event.cacheName, event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_CacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event = new CacheEvictionEvent("cache", "some/key", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler).evict(CacheEntry.from(event.cacheName, event.key));
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ProjectCacheEvictionWithSlash()
      throws Exception {
    final CacheEvictionEvent event =
        new CacheEvictionEvent(Constants.PROJECTS, "some/project", INSTANCE_ID);
    router.route(event);

    verify(cacheEvictionHandler)
        .evict(CacheEntry.from(event.cacheName, Project.nameKey((String) event.key)));
  }
}
