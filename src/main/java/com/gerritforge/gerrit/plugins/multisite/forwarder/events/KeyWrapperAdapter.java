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

import com.google.gerrit.server.cache.CacheKeyType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class KeyWrapperAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {
  private static final String KEY_TYPE = "keyType";
  private static final String KEY_VALUE_FIELD = "keyValue";
  private static HashMap<Class<?>, String> keyClassesByName = new HashMap<>();

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
      Class<?> cls = CacheKeyType.getKeyClass(typeName);
      if (cls == null) {
        throw new KeyTypeNotRegisteredException(typeName);
      }
      return context.deserialize(jsonObject.get(KEY_VALUE_FIELD), cls);
    } else {
      return context.deserialize(json, type);
    }
  }

  private static Optional<String> getCacheKeyTypeByClass(Class<?> clazz) {
    if (keyClassesByName.containsKey(clazz)) {
      return Optional.ofNullable(keyClassesByName.get(clazz));
    } else {
      Optional<String> keyClassName = CacheKeyType.getRegisteredKeys().entrySet().stream()
          .filter(entry -> entry.getValue().equals(clazz))
          .map(Map.Entry::getKey)
          .findFirst();
      keyClassName.ifPresent(name -> keyClassesByName.put(clazz, name));
      return keyClassName;
    }
  }
}
