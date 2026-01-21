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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CacheEntryTest {

  @Test
  public void cacheEntry() throws Exception {
    CacheEntry entry = CacheEntry.from("my_plugin", "my_cache", "someOtherKey");
    assertThat(entry.getPluginName()).isEqualTo("my_plugin");
    assertThat(entry.getCacheName()).isEqualTo("my_cache");
    assertThat(entry.getKey()).isEqualTo("someOtherKey");
  }
}
