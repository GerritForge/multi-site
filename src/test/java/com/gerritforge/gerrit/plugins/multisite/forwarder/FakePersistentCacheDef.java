// Copyright (C) 2026 GerritForge, Inc.
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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.Weigher;
import com.google.gerrit.server.cache.PersistentCacheDef;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.inject.TypeLiteral;
import java.time.Duration;
import org.junit.Ignore;

@Ignore
public class FakePersistentCacheDef<K, V> implements PersistentCacheDef<K, V> {
  private final String name;
  private final TypeLiteral<K> keyType;
  private final TypeLiteral<V> valueType;
  private final CacheSerializer<K> keySerializer;

  FakePersistentCacheDef(
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
