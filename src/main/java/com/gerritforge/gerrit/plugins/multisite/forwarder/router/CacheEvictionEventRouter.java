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

import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheEntry;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheKeyJsonParser;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheNotFoundException;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedCacheEvictionHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.google.inject.Inject;

public class CacheEvictionEventRouter implements ForwardedEventRouter<CacheEvictionEvent> {
  private final ForwardedCacheEvictionHandler cacheEvictionHanlder;
  private final CacheKeyJsonParser gsonParser;

  @Inject
  public CacheEvictionEventRouter(
      ForwardedCacheEvictionHandler cacheEvictionHanlder, CacheKeyJsonParser gsonParser) {
    this.cacheEvictionHanlder = cacheEvictionHanlder;
    this.gsonParser = gsonParser;
  }

  @Override
  public void route(CacheEvictionEvent cacheEvictionEvent) throws CacheNotFoundException {
    Object parsedKey =
        gsonParser.from(
            cacheEvictionEvent.pluginName, cacheEvictionEvent.cacheName, cacheEvictionEvent.key);
    cacheEvictionHanlder.evict(CacheEntry.from(cacheEvictionEvent.cacheName, parsedKey));
  }
}
