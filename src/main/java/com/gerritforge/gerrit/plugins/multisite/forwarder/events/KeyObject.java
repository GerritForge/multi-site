package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

public class KeyObject {

  public Object key;
  public String keyType;

  public KeyObject(Object key) {
    this.key = key;
    this.keyType = key.getClass().getName();
  }
}
