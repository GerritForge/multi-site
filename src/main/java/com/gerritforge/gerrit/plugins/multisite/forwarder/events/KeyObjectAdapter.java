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

import com.google.gson.*;

import java.lang.reflect.Type;

public class KeyObjectAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {

  @Override
  public JsonElement serialize(Object key, Type typeOfSrc, JsonSerializationContext context) {
    JsonElement elem = new Gson().toJsonTree(key);
    if (elem.isJsonPrimitive()) {
      return elem;
    }
    elem.getAsJsonObject().addProperty("type", key.getClass().getName());
    return elem;
  }

  @Override
  public Object deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    if (json.isJsonPrimitive()) {
      return json.getAsString();
    }
    JsonObject jsonObject = json.getAsJsonObject();
    String typeName = jsonObject.get("key-type").getAsString();
    try {
      Class<? extends Object> cls = Class.forName(typeName);
      return new Gson().fromJson(jsonObject, cls);
    } catch (ClassNotFoundException e) {
      throw new JsonParseException(e);
    }
  }
}
