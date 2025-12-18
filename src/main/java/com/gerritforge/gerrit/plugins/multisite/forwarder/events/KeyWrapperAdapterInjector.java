package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.cache.CacheDef;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class KeyWrapperAdapterInjector {
  @Inject
  public KeyWrapperAdapterInjector(DynamicMap<CacheDef<?, ?>> cacheMap) {
    KeyWrapperAdapter.setCacheMap(cacheMap);
  }
}
