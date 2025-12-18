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

package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

import com.google.common.base.Objects;
import com.google.gson.annotations.JsonAdapter;

public class CacheEvictionEvent extends MultiSiteEvent {
  static final String TYPE = "cache-eviction";

  public String cacheName;

  @JsonAdapter(KeyWrapperAdapter.class)
  public Object key;

  public CacheEvictionEvent(String cacheName, Object key, String instanceId) {
    super(TYPE, instanceId);
    this.cacheName = cacheName;
    this.key = key;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(cacheName, key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CacheEvictionEvent that = (CacheEvictionEvent) o;
    return Objects.equal(cacheName, that.cacheName) && Objects.equal(key, that.key);
  }

  @Override
  public String toString() {
    return String.format(
        "CacheEvictionEvent { cacheName='%s', instanceId='%s', key=(%s) '%s' }",
        cacheName, instanceId, key.getClass(), key);
  }
}
