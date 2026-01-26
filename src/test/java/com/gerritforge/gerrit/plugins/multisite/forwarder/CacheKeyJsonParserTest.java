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
import static com.google.gerrit.extensions.registration.PluginName.GERRIT;

import com.gerritforge.gerrit.plugins.multisite.NoOpCacheKeyDef;
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
import com.google.gerrit.server.cache.PersistentCacheDef;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.gerrit.server.change.ChangeKindCacheImpl;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import java.time.Duration;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Test;

public class CacheKeyJsonParserTest {
  private static final String CACHE_CHANGE_KIND = "change_kind";
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

    definePersistentCache(
        GERRIT,
        CACHE_CHANGE_KIND,
        ChangeKindCacheImpl.Key.class,
        new MockMultiSiteChangeKindCacheKeySerializer());
    defineCache(GERRIT, CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    defineCache(GERRIT, CACHE_NAME_WITH_SIMPLE_KEY_TYPE, String.class);
    defineCache(PLUGIN_NAME1, CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    defineCache(PLUGIN_NAME2, CACHE_NAME_WITH_COMPLEX_KEY_TYPE, ComplexKey.class);
    defineCache(GERRIT, Constants.GROUPS, AccountGroup.Id.class);
    defineCache(GERRIT, Constants.ACCOUNTS, Account.Id.class);
    gsonParser = new CacheKeyJsonParser(gson, cacheDefMap);
  }

  private void defineCache(String pluginName, String cacheName, Class<?> keyRawType) {
    RegistrationHandle unused =
        cacheDefMap.put(
            pluginName,
            cacheName,
            Providers.of(new NoOpCacheKeyDef<>(cacheName, keyRawType, Object.class)));
  }

  private <K> void definePersistentCache(
      String pluginName, String cacheName, Class<K> keyRawType, CacheSerializer<K> keySerializer) {
    RegistrationHandle unused =
        cacheDefMap.put(
            pluginName,
            cacheName,
            Providers.of(
                new TestPersistentCacheDef<>(cacheName, keyRawType, Object.class, keySerializer)));
  }

  @Test
  public void serializeDeserializeCacheEvictionEventWithComplexKeyType() {
    CacheNameAndPlugin cacheNameWithPlugin =
        CacheNameAndPlugin.from(CACHE_NAME_WITH_COMPLEX_KEY_TYPE);
    ComplexKey complexKeyType = new ComplexKey("cache-key");
    String jsonEvent = gsonParser.toJson(cacheNameWithPlugin, complexKeyType);
    Object parsedKey = gsonParser.from(cacheNameWithPlugin, jsonEvent);
    assertThat(parsedKey).isEqualTo(complexKeyType);
  }

  @Test
  public void serializeDeserializeCacheEvictionEventWithAutoValueKeyType() {
    ChangeKindCacheImpl.Key changeKindType =
        ChangeKindCacheImpl.Key.create(
            ObjectId.fromString("3ac9c9cb5bc4b38606b06bc3df49d970ca0ce7b2"),
            ObjectId.fromString("b99e90c10f33d7c4c1eead0531119ac9417f8c78"),
            "sample-strategy");
    CacheNameAndPlugin cacheNameWithPlugin = CacheNameAndPlugin.from(CACHE_CHANGE_KIND);
    String jsonEvent = gsonParser.toJson(cacheNameWithPlugin, changeKindType);
    Object parsedKey = gsonParser.from(cacheNameWithPlugin, jsonEvent);
    assertThat(parsedKey).isEqualTo(changeKindType);
  }

  @Test
  public void serializeDeserializeCacheEvictionEventForMultiplePlugins() {
    CacheNameAndPlugin cacheNameWithPlugin1 =
        CacheNameAndPlugin.from(PLUGIN_NAME1 + "." + CACHE_NAME_WITH_COMPLEX_KEY_TYPE);
    CacheNameAndPlugin cacheNameWithPlugin2 =
        CacheNameAndPlugin.from(PLUGIN_NAME2 + "." + CACHE_NAME_WITH_COMPLEX_KEY_TYPE);
    ComplexKey complexKeyType1 = new ComplexKey("cache-key-1");
    ComplexKey complexKeyType2 = new ComplexKey("cache-key-2");
    String jsonEvent1 = gsonParser.toJson(cacheNameWithPlugin1, complexKeyType1);
    String jsonEvent2 = gsonParser.toJson(cacheNameWithPlugin2, complexKeyType2);
    Object parsedKey1 = gsonParser.from(cacheNameWithPlugin1, jsonEvent1);
    Object parsedKey2 = gsonParser.from(cacheNameWithPlugin2, jsonEvent2);
    assertThat(parsedKey1).isEqualTo(complexKeyType1);
    assertThat(parsedKey2).isEqualTo(complexKeyType2);
  }

  @Test
  public void accountIDParse() {
    CacheNameAndPlugin cacheNameWithPlugin = CacheNameAndPlugin.from(Constants.ACCOUNTS);
    Account.Id accountId = Account.id(1);
    String json = gsonParser.toJson(cacheNameWithPlugin, accountId);
    assertThat(accountId).isEqualTo(gsonParser.from(cacheNameWithPlugin, json));
  }

  @Test
  public void accountGroupIDParse() {
    AccountGroup.Id accountGroupId = AccountGroup.id(1);
    CacheNameAndPlugin cacheNameWithPlugin = CacheNameAndPlugin.from(Constants.GROUPS);
    String json = gsonParser.toJson(cacheNameWithPlugin, accountGroupId);
    assertThat(accountGroupId).isEqualTo(gsonParser.from(cacheNameWithPlugin, json));
  }

  @Test
  public void accountGroupUUIDParse() {
    AccountGroup.UUID accountGroupUuid = AccountGroup.uuid("abc123");
    CacheNameAndPlugin cacheNameWithPlugin = CacheNameAndPlugin.from(Constants.GROUPS_BYSUBGROUP);
    String json = gsonParser.toJson(cacheNameWithPlugin, accountGroupUuid);
    assertThat(accountGroupUuid).isEqualTo(gsonParser.from(cacheNameWithPlugin, json));
  }

  @Test
  public void projectNameKeyParse() {
    String projectNameString = "foo";
    Project.NameKey projectNameKey = Project.nameKey(projectNameString);
    assertThat(projectNameKey)
        .isEqualTo(gsonParser.from(CacheNameAndPlugin.from(Constants.PROJECTS), projectNameString));
  }

  @Test
  public void stringParse() {
    String key = "key";
    assertThat(key)
        .isEqualTo(gsonParser.from(CacheNameAndPlugin.from(CACHE_NAME_WITH_SIMPLE_KEY_TYPE), key));
  }

  @Test
  public void noKeyParse() {
    Object object = new Object();
    String json =
        gsonParser.toJson(CacheNameAndPlugin.from(CACHE_NAME_WITH_SIMPLE_KEY_TYPE), object);
    assertThat(json).isEqualTo(EMPTY_JSON);
  }

  private static class TestPersistentCacheDef<K, V> implements PersistentCacheDef<K, V> {
    private final String name;
    private final TypeLiteral<K> keyType;
    private final TypeLiteral<V> valueType;
    private final CacheSerializer<K> keySerializer;

    TestPersistentCacheDef(
        String name, Class<K> keyClass, Class<V> valueClass, CacheSerializer<K> keySerializer) {
      this.name = name;
      this.keyType = TypeLiteral.get(keyClass);
      this.valueType = TypeLiteral.get(valueClass);
      this.keySerializer = keySerializer;
    }

    @Override
    public long diskLimit() {
      return 0;
    }

    @Override
    public int version() {
      return 0;
    }

    @Override
    public CacheSerializer<K> keySerializer() {
      return keySerializer;
    }

    @Override
    public CacheSerializer<V> valueSerializer() {
      return null;
    }

    @Override
    public String name() {
      return "";
    }

    @Override
    public String configKey() {
      return "";
    }

    @Override
    public TypeLiteral<K> keyType() {
      return null;
    }

    @Override
    public TypeLiteral<V> valueType() {
      return null;
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
