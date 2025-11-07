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

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;

public interface CacheEvictionForwarder {
  /**
   * Forward a cache eviction event to the other master.
   *
   * @param task that triggered the forwarding of the cache event.
   * @param cacheEvictionEvent the details of the cache eviction event.
   * @return true if successful, otherwise false.
   */
  boolean evict(ForwarderTask task, CacheEvictionEvent cacheEvictionEvent);
}
