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
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.MultiSiteEvent;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PrivateInternals_DynamicMapImpl;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.Weigher;

import com.google.gson.Gson;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

public class CacheKeyJsonParserTest {
  private static final String CACHE_NAME_WITH_COMPLEX_KEY_TYPE = "complex-test-cache";
  private static final String CACHE_NAME_WITH_SIMPLE_KEY_TYPE = "simple-test-cache";
  private static final Object EMPTY_JSON = "{}";

  private final Gson gson = new EventGsonProvider().get();
  private final CacheKeyJsonParser gsonParser = new CacheKeyJsonParser(gson, getDynamicMap());

  public static class ComplexKeyType {
    public String aKey;

    protected ComplexKeyType(String key) {
      this.aKey = key;
    }
  }

  private PrivateInternals_DynamicMapImpl<CacheDef<?, ?>> cacheDefMap;

  private DynamicMap<CacheDef<?, ?>> getDynamicMap() {
    cacheDefMap = (PrivateInternals_DynamicMapImpl<CacheDef<?, ?>>)
        DynamicMap.<CacheDef<?, ?>>emptyMap();

    CacheDef<ComplexKeyType, Object> complexKeyTypeCacheDef =
        new TestCacheDef<>(CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKeyType.class, Object.class);
    CacheDef<java.lang.String, Object> simpleKeyTypeCacheDef =
        new TestCacheDef<>(CACHE_NAME_WITH_SIMPLE_KEY_TYPE, java.lang.String.class, Object.class);
    cacheDefMap.put("gerrit", CACHE_NAME_WITH_COMPLEX_KEY_TYPE, Providers.of(complexKeyTypeCacheDef));
    cacheDefMap.put("gerrit", CACHE_NAME_WITH_SIMPLE_KEY_TYPE, Providers.of(simpleKeyTypeCacheDef));
    return cacheDefMap;
  }

  @Before
  public void setUp() throws Exception {
    MultiSiteEvent.registerEventTypes();
  }

  @Test
  public void serializeDeserializeCacheEvictionEventWithComplexKeyType() {
    ComplexKeyType complexKeyType = new ComplexKeyType("cache-key");
    String jsonEvent = gson.toJson(complexKeyType);
    Object parsedKey = gsonParser.from(CACHE_NAME_WITH_COMPLEX_KEY_TYPE, jsonEvent);
    assertThat(parsedKey).isInstanceOf(ComplexKeyType.class);
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
    assertThat(accountGroupUuid).isEqualTo(gsonParser.from(Constants.GROUPS_BYSUBGROUP, json));
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
    assertThat(key).isEqualTo(gsonParser.from(CACHE_NAME_WITH_SIMPLE_KEY_TYPE, key));
  }

  @Test
  public void noKeyParse() {
    Object object = new Object();
    String json = gson.toJson(object);
    assertThat(json).isEqualTo(EMPTY_JSON);
  }

  private static class TestCacheDef<K, V> implements CacheDef<K, V> {
    private final String name;
    private final TypeLiteral<K> keyType;
    private final TypeLiteral<V> valueType;
    TestCacheDef(String name, Class<K> keyClass, Class<V> valueClass) {
      this.name = name;
      this.keyType = TypeLiteral.get(keyClass);
      this.valueType = TypeLiteral.get(valueClass);
    }
    @Override public String name() { return name; }
    @Override public String configKey() { return name; }
    @Override public TypeLiteral<K> keyType() { return keyType; }
    @Override public TypeLiteral<V> valueType() { return valueType; }
    @Override public long maximumWeight() { return 0; }
    @Override public Duration expireAfterWrite() { return null; }
    @Override public Duration expireFromMemoryAfterAccess() { return null; }
    @Override public Duration refreshAfterWrite() { return null; }
    @Override public Weigher<K, V> weigher() { return null; }
    @Override public CacheLoader<K, V> loader() { return null; }
  }
}
