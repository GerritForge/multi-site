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

import static com.google.gerrit.extensions.registration.PluginName.GERRIT;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.google.common.base.MoreObjects;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.cache.PersistentCacheDef;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.NavigableMap;
import java.util.Optional;

public final class CacheKeyJsonParser {
  private final Gson gson;
  private final DynamicMap<CacheDef<?, ?>> cachesMap;

  @Inject
  public CacheKeyJsonParser(@EventGson Gson gson, DynamicMap<CacheDef<?, ?>> cachesMap) {
    this.gson = gson;
    this.cachesMap = cachesMap;
  }

  public <K> String toJson(String cacheNameWithPlugin, K cacheKeyValue) {
    int cacheNameStart = cacheNameWithPlugin.indexOf('.');
    String pluginName =
            cacheNameStart > 0 ? cacheNameWithPlugin.substring(0, cacheNameStart) : GERRIT;
    String cacheName =
            cacheNameStart > 0
                    ? cacheNameWithPlugin.substring(cacheNameStart + 1)
                    : cacheNameWithPlugin;
    Optional<CacheSerializer<K>> keySerializer = getCacheKeySerializerFromDefs(pluginName, cacheName);
    return keySerializer.map(ser -> gson.toJson(ser.serialize(cacheKeyValue))).orElseGet(() -> gson.toJson(cacheKeyValue));
  }

  @SuppressWarnings("cast")
  public <K> K from(String cacheNameWithPlugin, Object cacheKeyValue) {
    int cacheNameStart = cacheNameWithPlugin.indexOf('.');
    String pluginName =
        cacheNameStart > 0 ? cacheNameWithPlugin.substring(0, cacheNameStart) : GERRIT;
    String cacheName =
        cacheNameStart > 0
            ? cacheNameWithPlugin.substring(cacheNameStart + 1)
            : cacheNameWithPlugin;
    Object parsedKey;
    // Need to add a case for 'adv_bases'
    switch (cacheName) {
      case Constants.ACCOUNTS:
        parsedKey = Account.id(jsonElement(cacheKeyValue).getAsJsonObject().get("id").getAsInt());
        break;
      case Constants.GROUPS:
        parsedKey =
            AccountGroup.id(jsonElement(cacheKeyValue).getAsJsonObject().get("id").getAsInt());
        break;
      case Constants.GROUPS_BYSUBGROUP:
        parsedKey =
            AccountGroup.uuid(
                jsonElement(cacheKeyValue).getAsJsonObject().get("uuid").getAsString());
        break;
      case Constants.PROJECTS:
        parsedKey = Project.nameKey(nullToEmpty(cacheKeyValue));
        break;
      case Constants.PROJECT_LIST:
        parsedKey = gson.fromJson(nullToEmpty(cacheKeyValue).toString(), Object.class);
        break;
      default:
        Optional<CacheSerializer<K>> keySerializer = getCacheKeySerializerFromDefs(pluginName, cacheName);
        parsedKey = keySerializer
                .map(ser -> ser.deserialize(gson.fromJson(jsonElement(cacheKeyValue), byte[].class)))
                .orElseGet(() -> (K) gson.fromJson(jsonElement(cacheKeyValue), getCacheKeyClassFromDefs(pluginName, cacheName)));
    }
    return (K) parsedKey;
  }

  private JsonElement jsonElement(Object json) {
    String jsonString = nullToEmpty(json);
    try {
      return gson.fromJson(jsonString, JsonElement.class);
    } catch (JsonParseException e) {
      return new JsonPrimitive(jsonString);
    }
  }

  private static String nullToEmpty(Object value) {
    return MoreObjects.firstNonNull(value, "").toString().trim();
  }

  private Class<?> getCacheKeyClassFromDefs(String pluginName, String cacheName) {
    CacheDef<?, ?> cacheDef = getCacheDef(pluginName, cacheName);
    return cacheDef.keyType().getRawType();
  }

  private <K> Optional<CacheSerializer<K>> getCacheKeySerializerFromDefs(String pluginName, String cacheName) {
    CacheDef<K,?> cacheDef = getCacheDef(pluginName, cacheName);
    return switch (cacheDef) {
      case PersistentCacheDef<K,?> persistentCacheDef -> Optional.of(persistentCacheDef.keySerializer());
      default -> Optional.empty();
    };
  }

  private <K> CacheDef<K,?> getCacheDef(String pluginName, String cacheName) {
    NavigableMap<String, Provider<CacheDef<?, ?>>> cachesByPlugin = cachesMap.byPlugin(pluginName);
    if (cachesByPlugin == null) {
      throw new IllegalStateException("Unable to find any cache provided by " + pluginName);
    }
    Provider<CacheDef<?, ?>> cacheDefProvider = cachesByPlugin.get(cacheName);
    if (cacheDefProvider == null) {
      throw new IllegalStateException(
          "Unable to find definition for cache '" + cacheName + "' provided by " + pluginName);
    }
    return (CacheDef<K, ?>) cacheDefProvider.get();
  }
}
