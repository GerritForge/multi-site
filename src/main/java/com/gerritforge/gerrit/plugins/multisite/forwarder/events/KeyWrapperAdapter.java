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

import com.google.gerrit.server.cache.CacheDef;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.google.gerrit.extensions.registration.DynamicMap;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeyWrapperAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {
  private static final String KEY_TYPE = "keyType";
  private static final String KEY_VALUE_FIELD = "keyValue";
  private static final HashMap<Class<?>, String> keyNamesByClasses = new HashMap<>();
  private static final HashMap<String, Class<?>> keyClassesByName = new HashMap<>();

  private static DynamicMap<CacheDef<?, ?>> cacheMap;

  public static void setCacheMap(DynamicMap<CacheDef<?, ?>> map) {
    cacheMap = map;
  }

  public static  DynamicMap<CacheDef<?, ?>> getCacheMap() {
    return cacheMap;
  }

  @Override
  public JsonElement serialize(Object key, Type typeOfSrc, JsonSerializationContext context) {
    if (key instanceof String || key instanceof Number) {
      return context.serialize(key);
    } else {
      JsonObject wrapper = new JsonObject();
      Optional<String> keyType = getCacheKeyTypeByClass(key.getClass());
      if (keyType.isPresent()) {
        wrapper.addProperty(KEY_TYPE, keyType.get());
        wrapper.add(KEY_VALUE_FIELD, context.serialize(key));
        return wrapper;
      } else {
        throw new KeyTypeNotRegisteredException(key.getClass().getName());
      }
    }
  }

  @Override
  public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.isJsonObject()) {
      JsonObject jsonObject = json.getAsJsonObject();
      if (!jsonObject.has(KEY_TYPE)) {
        throw new JsonParseException("JSON Object has no member " + KEY_TYPE);
      }
      String typeName = jsonObject.get(KEY_TYPE).getAsString();
      Optional<Class<?>> clazz = getCacheDef(typeName);
      if (clazz.isPresent()) {
        Class<?> cls = clazz.get();
        return context.deserialize(jsonObject.get(KEY_VALUE_FIELD), cls);
      } else {
        throw new KeyTypeNotRegisteredException(typeName);
      }
    } else {
      return context.deserialize(json, type);
    }
  }

  private static Optional<String> getCacheKeyTypeByClass(Class<?> clazz) {
    if (keyNamesByClasses.containsKey(clazz)) {
      return Optional.ofNullable(keyNamesByClasses.get(clazz));
    } else {
      Optional<String> keyClassName = getDynamicCacheDefs().entrySet().stream()
          .filter(entry -> entry.getValue().equals(clazz))
          .map(Map.Entry::getKey)
          .findFirst();
      keyClassName.ifPresent(name -> keyNamesByClasses.put(clazz, name));
      return keyClassName;
    }
  }

  private static Map<String, Class<?>> getDynamicCacheDefs() {
    Map<String, Class<?>> cacheDefMap = new HashMap<>();
    for (String pluginName : cacheMap.plugins()) {
      for (String cacheName : cacheMap.byPlugin(pluginName).keySet()) {
        CacheDef<?, ?> cacheDef = cacheMap.byPlugin(pluginName).get(cacheName).get();
        cacheDefMap.put(cacheDef.name(), cacheDef.keyType().getRawType());
      }
    }
    return cacheDefMap;
  }

  private static Optional<Class<?>> getCacheDef(String name) {
    if(keyClassesByName.containsKey(name)) {
      return Optional.ofNullable(keyClassesByName.get(name));
    } else {
      Map<String, Class<?>> cacheDefMap = getDynamicCacheDefs();
      if(cacheDefMap.containsKey(name)) {
        keyNamesByClasses.put(cacheDefMap.get(name), name);
      }
      return Optional.ofNullable(cacheDefMap.get(name));
    }
  }
}
