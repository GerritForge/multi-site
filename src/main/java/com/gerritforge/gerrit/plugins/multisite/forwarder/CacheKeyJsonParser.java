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

import com.google.common.base.MoreObjects;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

public final class CacheKeyJsonParser {
  private final Gson gson;
  private final DynamicMap<CacheDef<?,?>> cachesMap;

  @Inject
  public CacheKeyJsonParser(@EventGson Gson gson, DynamicMap<CacheDef<?,?>> cachesMap) {
    this.gson = gson;
    this.cachesMap = cachesMap;
  }

  @SuppressWarnings("cast")
  public Object from(String cacheName, Object cacheKeyValue) {
    Map<String, Class<?>> cacheDefMap = getDynamicCacheDefs();
    if (!cacheDefMap.containsKey(cacheName)) {
      throw new IllegalStateException(cacheName);
    }
    Class<?> cls = cacheDefMap.get(cacheName);
    JsonElement json = jsonElement(cacheKeyValue);
    if(json.isJsonPrimitive()){
      return gson.fromJson(json.getAsJsonPrimitive().getAsString(), cls);
    } else {
      return gson.fromJson(json.getAsJsonObject(), cls);
    }
  }

  private JsonElement jsonElement(Object json) {
    return gson.fromJson(nullToEmpty(json), JsonElement.class);
  }

  private static String nullToEmpty(Object value) {
    return MoreObjects.firstNonNull(value, "").toString().trim();
  }

  private Map<String, Class<?>> getDynamicCacheDefs() {
    Map<String, Class<?>> cacheDefMap = new HashMap<>();
    for (String pluginName : cachesMap.plugins()) {
      for (String cacheName : cachesMap.byPlugin(pluginName).keySet()) {
        CacheDef<?, ?> cacheDef = cachesMap.byPlugin(pluginName).get(cacheName).get();
        cacheDefMap.put(cacheDef.name(), cacheDef.keyType().getRawType());
      }
    }
    return cacheDefMap;
  }
}
