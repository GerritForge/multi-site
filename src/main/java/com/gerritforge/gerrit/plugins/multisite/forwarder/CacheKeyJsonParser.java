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
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public final class CacheKeyJsonParser {
  private final Gson gson;

  @Inject
  public CacheKeyJsonParser(@EventGson Gson gson) {
    this.gson = gson;
  }

  @SuppressWarnings("cast")
  public Object from(String cacheName, Object cacheKeyValue) {
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
      case Constants.GROUPS_BYINCLUDE:
      case Constants.GROUPS_MEMBERS:
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
        if (cacheKeyValue instanceof String) {
          parsedKey = (String) cacheKeyValue;
        } else {
          try {
            parsedKey = gson.fromJson(nullToEmpty(cacheKeyValue).toString().trim(), String.class);
          } catch (Exception e) {
            parsedKey = gson.fromJson(nullToEmpty(cacheKeyValue).toString(), Object.class);
          }
        }
    }
    return parsedKey;
  }

  private JsonElement jsonElement(Object json) {
    return gson.fromJson(nullToEmpty(json), JsonElement.class);
  }

  private static String nullToEmpty(Object value) {
    return MoreObjects.firstNonNull(value, "").toString().trim();
  }
}
