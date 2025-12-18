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

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.MultiSiteEvent;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import org.junit.Test;

public class CacheKeyJsonParserTest {
  private static final Object EMPTY_JSON = "{}";

  private final Gson gson = new EventGsonProvider().get();
  private final CacheKeyJsonParser gsonParser = new CacheKeyJsonParser(gson);

  public record ComplexKeyType(String key) { }

  @Test
  public void serializeDeserializeCacheEvictionEventWithComplexKeyType() {
    MultiSiteEvent.registerEventTypes();
    CacheEvictionEvent event = new CacheEvictionEvent("test-cache", new ComplexKeyType("cache-key"), "myinstance");
    String jsonEvent = gson.toJson(event);
    Event parsedEvent = gson.fromJson(jsonEvent, Event.class);
    assertThat(parsedEvent).isEqualTo(event);
  }

  @Test
  public void serializeDeserializeCacheEvictionWithPrimitiveType() {
    MultiSiteEvent.registerEventTypes();
    CacheEvictionEvent event = new CacheEvictionEvent("test-cache", "cache-key", "myinstance");
    String jsonEvent = gson.toJson(event);
    Event parsedEvent = gson.fromJson(jsonEvent, Event.class);
    assertThat(parsedEvent).isEqualTo(event);
  }

  @Test
  public void accountIDParse() {
    Account.Id accountId = Account.id(1);
    String json = gson.toJson(accountId);
    assertThat(accountId).isEqualTo(gsonParser.from(Constants.ACCOUNTS, json));
  }

  @Test
  public void accountGroupIDParse() {
    AccountGroup.Id accountGroupId = AccountGroup.id(1);
    String json = gson.toJson(accountGroupId);
    assertThat(accountGroupId).isEqualTo(gsonParser.from(Constants.GROUPS, json));
  }

  @Test
  public void accountGroupUUIDParse() {
    AccountGroup.UUID accountGroupUuid = AccountGroup.uuid("abc123");
    String json = gson.toJson(accountGroupUuid);
    assertThat(accountGroupUuid).isEqualTo(gsonParser.from(Constants.GROUPS_BYINCLUDE, json));
  }

  @Test
  public void projectNameKeyParse() {
    String projectNameString = "foo";
    Project.NameKey projectNameKey = Project.nameKey(projectNameString);
    assertThat(projectNameKey).isEqualTo(gsonParser.from(Constants.PROJECTS, projectNameString));
  }

  @Test
  public void stringParse() {
    String key = "key";
    assertThat(key).isEqualTo(gsonParser.from("any-cache-with-string-key", key));
  }

  @Test
  public void noKeyParse() {
    Object object = new Object();
    String json = gson.toJson(object);
    assertThat(json).isEqualTo(EMPTY_JSON);
  }
}
