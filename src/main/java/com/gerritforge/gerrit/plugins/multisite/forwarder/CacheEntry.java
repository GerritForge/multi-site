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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import com.google.common.base.Objects;

/** Represents a cache entry to evict */
public class CacheEntry {
  private final String pluginName;
  private final String cacheName;
  private final Object key;

  /**
   * Cache entry
   *
   * @param pluginName the plugin name to which the cache belongs, or "gerrit" for a Gerrit core
   *     cache
   * @param cacheName the name of the cache to evict the entry from
   * @param key the key identifying the entry in the cache
   */
  public CacheEntry(String pluginName, String cacheName, Object key) {
    this.pluginName = pluginName;
    this.cacheName = cacheName;
    this.key = key;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getCacheName() {
    return cacheName;
  }

  public Object getKey() {
    return key;
  }

  /**
   * Build a CacheEntry from the specified cache and key
   *
   * @param cacheNameAndPlugin CacheNameAndPlugin representing the cache and plugin names
   * @param key The Object representing the key
   * @return the CacheEntry
   */
  public static CacheEntry from(CacheNameAndPlugin cacheNameAndPlugin, Object key) {
    return new CacheEntry(cacheNameAndPlugin.pluginName(), cacheNameAndPlugin.cacheName(), key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CacheEntry that = (CacheEntry) o;
    return Objects.equal(pluginName, that.pluginName)
        && Objects.equal(cacheName, that.cacheName)
        && Objects.equal(key, that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pluginName, cacheName, key);
  }
}
