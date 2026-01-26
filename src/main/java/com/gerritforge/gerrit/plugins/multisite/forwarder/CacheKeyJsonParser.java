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

  public String toJson(CacheNameAndPlugin cacheNameWithPlugin, Object cacheKeyValue) {
    return getCacheKeySerializerFromDefs(cacheNameWithPlugin)
        .map(ser -> ser.serialize(cacheKeyValue))
        .map(gson::toJson)
        .orElseGet(() -> gson.toJson(cacheKeyValue));
  }

  public Object from(CacheNameAndPlugin cacheNameWithPlugin, Object cacheKeyValue) {
    Object parsedKey;
    // Need to add a case for 'adv_bases'
    switch (cacheNameAndPlugin.cacheName()) {
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
        Optional<CacheSerializer<Object>> keySerializer =
            getCacheKeySerializerFromDefs(cacheNameWithPlugin);
        if (keySerializer.isPresent()) {
          return keySerializer
              .get()
              .deserialize(gson.fromJson(jsonElement(cacheKeyValue), byte[].class));
        }
        Class<?> cls = getCacheKeyClassFromDefs(cacheNameWithPlugin);
        parsedKey = gson.fromJson(jsonElement(cacheKeyValue), cls);
    }
    return parsedKey;
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

  private Class<?> getCacheKeyClassFromDefs(CacheNameAndPlugin cacheNameAndPlugin) {
    return getCacheDef(cacheNameAndPlugin).keyType().getRawType();
  }

  private <K> Optional<CacheSerializer<K>> getCacheKeySerializerFromDefs(
      CacheNameAndPlugin cacheNameWithPlugin) {
    switch (cacheNameWithPlugin.cacheName()) {
      case Constants.ACCOUNTS:
      case Constants.GROUPS:
      case Constants.GROUPS_BYSUBGROUP:
      case Constants.PROJECTS:
      case Constants.PROJECT_LIST:
        return Optional.empty();
      default:
        CacheDef<K, ?> cacheDef = getCacheDef(cacheNameWithPlugin);
        return switch (cacheDef) {
          case PersistentCacheDef<K, ?> persistentCacheDef ->
              Optional.of(persistentCacheDef.keySerializer());
          default -> Optional.empty();
        };
    }
  }

  // The cast to CacheDef<K, ?> is safe in the context of this class, as the
  // callers of this method are responsible for ensuring the type K is correct.
  // A runtime check is not possible due to Java's type erasure.
  @SuppressWarnings("unchecked")
  private <K> CacheDef<K, ?> getCacheDef(CacheNameAndPlugin cacheNameAndPlugin) {
    NavigableMap<String, Provider<CacheDef<?, ?>>> cachesByPlugin =
        cachesMap.byPlugin(cacheNameAndPlugin.pluginName());
    if (cachesByPlugin == null) {
      throw new IllegalStateException(
          "Unable to find any cache provided by " + cacheNameAndPlugin.pluginName());
    }
    Provider<CacheDef<?, ?>> cacheDefProvider = cachesByPlugin.get(cacheNameAndPlugin.cacheName());
    if (cacheDefProvider == null) {
      throw new IllegalStateException(
          "Unable to find definition for cache '"
              + cacheNameAndPlugin.cacheName()
              + "' provided by "
              + cacheNameAndPlugin.pluginName());
    }
    return (CacheDef<K, ?>) cacheDefProvider.get();
  }
}
