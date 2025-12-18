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

import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class KeyObjectAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {

  private Gson gson = new EventGsonProvider().get();

  @Override
  public JsonElement serialize(Object key, Type typeOfSrc, JsonSerializationContext context) {
    KeyObject keyObject = new KeyObject(key);
    JsonElement elem = gson.toJsonTree(keyObject);
    return elem;
  }

  @Override
  public Object deserialize(
      JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {
    if (json.isJsonObject()) {
      JsonObject jsonObject = json.getAsJsonObject();
      String typeName = jsonObject.get("keyType").getAsString();
      try {
        Class<? extends Object> cls = Class.forName(typeName);
        return gson.fromJson(jsonObject.get("key"), cls);
      } catch (ClassNotFoundException e) {
        throw new JsonParseException(e);
      }
    } else {
      return gson.fromJson(json, type);
    }
  }
}
