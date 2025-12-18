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

import com.google.gerrit.server.events.EventTypes;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class KeyObjectAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {
  private static final String CLASS_NAME_FIELD = "keyClassName";
  private static final String KEY_VALUE_FIELD = "keyValue";

  @Override
  public JsonElement serialize(Object key, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject wrapper = new JsonObject();
    wrapper.addProperty(CLASS_NAME_FIELD, key.getClass().getName());
    wrapper.add(KEY_VALUE_FIELD, context.serialize(key));
    return wrapper;
  }

  @Override
  public Object deserialize(
      JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.isJsonObject() && json.getAsJsonObject().has(CLASS_NAME_FIELD)) {
      JsonObject jsonObject = json.getAsJsonObject();
      String typeName = jsonObject.get(CLASS_NAME_FIELD).getAsString();
      try {
        Class<?> cls = EventTypes.getClass(typeName);
        if (cls == null) {
          cls = Class.forName(typeName);
        }
        return context.deserialize(jsonObject.get(KEY_VALUE_FIELD), cls);
      } catch (ClassNotFoundException e) {
        throw new JsonParseException(e);
      }
    } else {
      return context.deserialize(json, type);
    }
  }
}
