// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.multisite.index;

/**
 * Interface that allows the multi-site plugin to query for a Git entity consistency before running
 * its indexing operation and fail.
 *
 * @param <K> The entity key type
 */
public interface ConsistencyChecker<K> {

  /**
   * Checks if the local entity's metadata is consistent with its underlying the Git data. Checking
   * for consistency means:
   *
   * <p>- Verifying that the Git repository contains the entity (e.g. the associated ref exists)
   *
   * <p>- Verifying that the metadata points to sub-entities that exist on the Git repository (e.g.
   * the change NoteDb points to existing patch-sets)
   *
   * @param entityKey key of the entity to check
   * @return true if local entity is consistent
   */
  boolean isConsistent(K entityKey);
}
