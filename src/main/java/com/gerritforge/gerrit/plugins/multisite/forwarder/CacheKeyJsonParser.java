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
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.NavigableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CacheKeyJsonParser {
  private final Gson gson;
  private final DynamicMap<CacheDef<?, ?>> cachesMap;
  private static final HashMap<CachePluginAndNameRecord, Class<?>> keyClassesByName = new HashMap<>();

  @Inject
  public CacheKeyJsonParser(@EventGson Gson gson, DynamicMap<CacheDef<?, ?>> cachesMap) {
    this.gson = gson;
    this.cachesMap = cachesMap;
  }

  @SuppressWarnings("cast")
  public Object from(CachePluginAndNameRecord cacheNameWithPlugin, Object cacheKeyValue) {
    Object parsedKey;
    // Need to add a case for 'adv_bases'
    switch (cacheNameWithPlugin.name()) {
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
        Class<?> cls = getCacheDef(cacheNameWithPlugin);
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

  private Class<?> getCacheKeyClassFromDefs(CachePluginAndNameRecord cacheNameDetails) {
    NavigableMap<String, Provider<CacheDef<?, ?>>> cachesByPlugin = cachesMap.byPlugin(cacheNameDetails.plugin());
    if (cachesByPlugin == null) {
      throw new IllegalStateException("Unable to find any cache provided by " + cacheNameDetails.plugin());
    }
    Provider<CacheDef<?, ?>> cacheDefProvider = cachesByPlugin.get(cacheNameDetails.name());
    if (cacheDefProvider == null) {
      throw new IllegalStateException(
          "Unable to find definition for cache '" + cacheNameDetails.name() + "' provided by " + cacheNameDetails.name());
    }

    return cacheDefProvider.get().keyType().getRawType();
  }

  private Class<?> getCacheDef(CachePluginAndNameRecord name) {
    if (keyClassesByName.containsKey(name)) {
      return keyClassesByName.get(name);
    } else {
      Class<?> cacheValueType = getCacheKeyClassFromDefs(name);
        keyClassesByName.put(name, cacheValueType);
      return cacheValueType;
    }
  }
}
