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
import com.google.common.cache.CacheLoader;
import com.google.common.cache.Weigher;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PrivateInternals_DynamicMapImpl;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.server.cache.CacheDef;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class CacheKeyJsonParserTest {
  private static final String CACHE_NAME_WITH_COMPLEX_KEY_TYPE = "complex-test-cache";
  private static final String CACHE_NAME_WITH_SIMPLE_KEY_TYPE = "simple-test-cache";
  private static final Object EMPTY_JSON = "{}";
  private static final String PLUGIN_NAME1 = "plugin1";
  private static final String PLUGIN_NAME2 = "plugin2";

  private final Gson gson = new EventGsonProvider().get();
  private CacheKeyJsonParser gsonParser;

  public record ComplexKey(String key) {}

  private PrivateInternals_DynamicMapImpl<CacheDef<?, ?>> cacheDefMap;

  @Before
  public void setUp() throws Exception {
    MultiSiteEvent.registerEventTypes();
    cacheDefMap =
        (PrivateInternals_DynamicMapImpl<CacheDef<?, ?>>) DynamicMap.<CacheDef<?, ?>>emptyMap();

    defineCache("gerrit", CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    defineCache("gerrit", CACHE_NAME_WITH_SIMPLE_KEY_TYPE, String.class);
    defineCache(PLUGIN_NAME1, CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    defineCache(PLUGIN_NAME2, CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    gsonParser = new CacheKeyJsonParser(gson, cacheDefMap);
  }

  private void defineCache(String pluginName, String cacheName, Class<?> keyRawType) {
    RegistrationHandle unused =
        cacheDefMap.put(
            pluginName,
            cacheName,
            Providers.of(
                new TestCacheDef<>(CACHE_NAME_WITH_COMPLEX_KEY_TYPE, keyRawType, Object.class)));
  }

  @Test
  public void serializeDeserializeCacheEvictionEventWithComplexKeyType() {
    ComplexKey complexKeyType = new ComplexKey("cache-key");
    String jsonEvent = gson.toJson(complexKeyType);
    Object parsedKey = gsonParser.from(CACHE_NAME_WITH_COMPLEX_KEY_TYPE, jsonEvent);
    assertThat(parsedKey).isEqualTo(complexKeyType);
  }

  @Test
  public void serializeDeserializeCacheEvictionEventForMultiplePlugins() {
    ComplexKey complexKeyType1 = new ComplexKey("cache-key-1");
    ComplexKey complexKeyType2 = new ComplexKey("cache-key-2");
    String jsonEvent1 = gson.toJson(complexKeyType1);
    String jsonEvent2 = gson.toJson(complexKeyType2);
    Object parsedKey1 =
        gsonParser.from(PLUGIN_NAME1 + "." + CACHE_NAME_WITH_COMPLEX_KEY_TYPE, jsonEvent1);
    Object parsedKey2 =
        gsonParser.from(PLUGIN_NAME2 + "." + CACHE_NAME_WITH_COMPLEX_KEY_TYPE, jsonEvent2);
    assertThat(parsedKey1).isEqualTo(complexKeyType1);
    assertThat(parsedKey2).isEqualTo(complexKeyType2);
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
    assertThat(key).isEqualTo(gsonParser.from("any-cache-with-string-key", key));
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

    @Override
    public String name() {
      return name;
    }

    @Override
    public String configKey() {
      return name;
    }

    @Override
    public TypeLiteral<K> keyType() {
      return keyType;
    }

    @Override
    public TypeLiteral<V> valueType() {
      return valueType;
    }

    @Override
    public long maximumWeight() {
      return 0;
    }

    @Override
    public Duration expireAfterWrite() {
      return null;
    }

    @Override
    public Duration expireFromMemoryAfterAccess() {
      return null;
    }

    @Override
    public Duration refreshAfterWrite() {
      return null;
    }

    @Override
    public Weigher<K, V> weigher() {
      return null;
    }

    @Override
    public CacheLoader<K, V> loader() {
      return null;
    }
  }
}
