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

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.google.common.base.MoreObjects;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CacheKeyJsonParser {
  private final Gson gson;
  private final DynamicMap<CacheDef<?, ?>> cachesMap;
  private static final HashMap<String, Class<?>> keyClassesByName = new HashMap<>();

  @Inject
  public CacheKeyJsonParser(@EventGson Gson gson, DynamicMap<CacheDef<?, ?>> cachesMap) {
    this.gson = gson;
    this.cachesMap = cachesMap;
  }

  @SuppressWarnings("cast")
  public Object from(String cacheName, Object cacheKeyValue) {
    Object parsedKey;
    // Need to add a case for 'adv_bases'
    switch (cacheName) {
      case Constants.GROUPS:
        parsedKey =
            AccountGroup.id(jsonElement(cacheKeyValue).getAsJsonObject().get("id").getAsInt());
        break;
      case Constants.GROUPS_BYSUBGROUP:
        parsedKey =
            AccountGroup.uuid(
                jsonElement(cacheKeyValue).getAsJsonObject().get("uuid").getAsString());
        break;
      default:
        Optional<Class<?>> clazz = getCacheDef(cacheName);
        if (clazz.isEmpty()) {
          throw new IllegalStateException(cacheName);
        }
        parsedKey = gson.fromJson(jsonElement(cacheKeyValue), clazz.get());
    }
    return parsedKey;
  }

  private JsonElement jsonElement(Object json) {
    String nullToEmptyJson = nullToEmpty(json);
    try {
      return gson.fromJson(nullToEmptyJson, JsonElement.class);
    } catch (JsonParseException e) {
      return new JsonPrimitive(nullToEmptyJson);
    }
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

  private Optional<Class<?>> getCacheDef(String name) {
    if (keyClassesByName.containsKey(name)) {
      return Optional.ofNullable(keyClassesByName.get(name));
    } else {
      Map<String, Class<?>> cacheDefMap = getDynamicCacheDefs();
      if (cacheDefMap.containsKey(name)) {
        keyClassesByName.put(name, cacheDefMap.get(name));
      }
      return Optional.ofNullable(cacheDefMap.get(name));
    }
  }
}
