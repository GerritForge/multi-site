package com.gerritforge.gerrit.plugins.multisite;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.Weigher;
import com.google.gerrit.server.cache.CacheDef;
import com.google.inject.TypeLiteral;

import java.time.Duration;

public class NoOpCacheKeyDef<K, V> implements CacheDef<K, V> {
  private final String name;
  private final TypeLiteral<K> keyType;
  private final TypeLiteral<V> valueType;

  public NoOpCacheKeyDef(String name, Class<K> keyClass, Class<V> valueClass) {
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
