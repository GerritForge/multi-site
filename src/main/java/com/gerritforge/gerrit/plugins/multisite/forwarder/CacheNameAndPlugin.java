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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;

public record CacheNameAndPlugin(String pluginName, String cacheName) {
  public static CacheNameAndPlugin from(String cacheName) {
    int dot = cacheName.indexOf('.');
    if (dot > 0) {
      return new CacheNameAndPlugin(cacheName.substring(0, dot), cacheName.substring(dot + 1));
    }
    return new CacheNameAndPlugin(Constants.GERRIT, cacheName);
  }
}
