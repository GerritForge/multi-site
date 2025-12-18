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
import static com.google.gerrit.server.events.EventTypes.register;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.CacheEvictionEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.MultiSiteEvent;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.client.DiffPreferencesInfo;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.patch.filediff.FileDiffCacheImpl;
import com.google.gerrit.server.patch.filediff.FileDiffCacheKey;
import com.google.gerrit.server.patch.gitfilediff.GitFileDiffCacheImpl;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Test;

public class CacheKeyJsonParserTest {
  private static final String CACHE_NAME = "test-cache";
  private static final Object EMPTY_JSON = "{}";

  private final Gson gson = new EventGsonProvider().get();
  private final CacheKeyJsonParser gsonParser = new CacheKeyJsonParser(gson);

  public static class ComplexKeyType extends MultiSiteEvent {
    public static final String type = "aType";
    public String aKey;

    protected ComplexKeyType(String key, String instanceId) {
      super(type, instanceId);
      this.aKey = key;
    }

    @Override
    public String toString() {
      return String.format(
          "CacheEvictionEvent { cacheName='%s', instanceId='%s', key=(%s) }",
          CACHE_NAME, "myinstance", this.getClass().getName());
    }
  }

  @Before
  public void setUp() throws Exception {
    MultiSiteEvent.registerEventTypes();
  }

  @Test
  public void serializeDeserializeCacheEvictionEventWithComplexKeyType() {
    ComplexKeyType complexKeyType = new ComplexKeyType("cache-key", "myinstance");
    register(complexKeyType.getClass().getName(), ComplexKeyType.class);
    CacheEvictionEvent event =
        new CacheEvictionEvent(CACHE_NAME, complexKeyType, complexKeyType.instanceId);
    String jsonEvent = gson.toJson(event);
    Event parsedEvent = gson.fromJson(jsonEvent, Event.class);
    assertThat(parsedEvent).isInstanceOf(CacheEvictionEvent.class);
    CacheEvictionEvent castedEvent = (CacheEvictionEvent) parsedEvent;
    assertThat(castedEvent.key).isInstanceOf(ComplexKeyType.class);
  }

  @Test
  public void serializeDeserializeCacheEvictionWithPrimitiveType() {
    CacheEvictionEvent event = new CacheEvictionEvent(CACHE_NAME, "cache-key", "myinstance");
    String jsonEvent = gson.toJson(event);
    Event parsedEvent = gson.fromJson(jsonEvent, Event.class);
    assertThat(parsedEvent).isEqualTo(event);
  }

  @Test
  public void serializeDeserializeFileDiffCacheKey() {
    FileDiffCacheKey fileDiffCacheKey = FileDiffCacheKey.builder()
        .project(Project.NameKey.parse("test"))
        .oldCommit(ObjectId.zeroId())
        .newCommit(ObjectId.zeroId())
        .newFilePath("aFilePath")
        .renameScore(1)
        .diffAlgorithm(GitFileDiffCacheImpl.DiffAlgorithm.HISTOGRAM_WITH_FALLBACK_MYERS)
        .whitespace(DiffPreferencesInfo.Whitespace.IGNORE_NONE)
        .useTimeout(true)
        .build();
    CacheEvictionEvent event = new CacheEvictionEvent(CACHE_NAME, fileDiffCacheKey, "myinstance");
    String jsonEvent = gson.toJson(event);
    Event parsedEvent = gson.fromJson(jsonEvent, Event.class);
    assertThat(parsedEvent).isEqualTo(event);
  }

  @Test
  public void deserializeCacheEvictionSerializedWithoutKeyType() {
    String oldEventString =
        "{\n"
            + "  \"cacheName\" : \"test-cache\","
            + "  \"key\" : \"cache-key\","
            + "  \"type\" : \"cache-eviction\","
            + "  \"eventCreatedOn\" : 1767002059,"
            + "  \"instanceId\" : \"myinstance\"}";

    Event parsedEvent = gson.fromJson(oldEventString, Event.class);
    assertThat(parsedEvent).isInstanceOf(CacheEvictionEvent.class);
    CacheEvictionEvent event = (CacheEvictionEvent) parsedEvent;
    assertThat(event.key).isInstanceOf(String.class);
  }

  @Test
  public void failToDeserializeComplexKeyCacheEvictionSerializedWithoutKeyType() {
    String oldEventString =
        "{"
            + "  \"cacheName\" : \"test-cache\","
            + "  \"key\" : {\"someField\" : \"cache-key\"},"
            + "  \"type\" : \"cache-eviction\","
            + "  \"eventCreatedOn\" : 1767010101,"
            + "  \"instanceId\" : \"myinstance\"}";

    assertThrows(JsonParseException.class, () -> gson.fromJson(oldEventString, Event.class));
  }

  @Test
  public void failToDeserializeCacheEvictionWithUnknownKeyType() {
    String oldEventString =
        "{"
            + "  \"cacheName\" : \"test-cache\","
            + "  \"key\" : {"
            + "    \"keyValue\" : {\"aKey\" : \"cache-key\"},"
            + "    \"keyClassName\" : \"unknownKeyType\"},"
            + "  \"type\" : \"cache-eviction\","
            + "  \"eventCreatedOn\" : 1767018518,"
            + "  \"instanceId\" : \"myinstance\"}";

    assertThrows(JsonParseException.class, () -> gson.fromJson(oldEventString, Event.class));
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
