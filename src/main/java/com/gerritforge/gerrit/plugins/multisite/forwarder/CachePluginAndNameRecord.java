package com.gerritforge.gerrit.plugins.multisite.forwarder;

import com.gerritforge.gerrit.plugins.multisite.cache.Constants;

public record CachePluginAndNameRecord (String plugin, String name) {
  public static CachePluginAndNameRecord from (String cacheName) {
    int dot = cacheName.indexOf('.');
    if (dot > 0) {
      return new CachePluginAndNameRecord(cacheName.substring(0, dot), cacheName.substring(dot + 1));
    }
    return new CachePluginAndNameRecord(Constants.GERRIT, cacheName);
  }
}
