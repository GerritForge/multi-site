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

package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

/**
 * Object used to encapsulate cache keys and their type so that when
 * deserializing cache eviction events we can be sure of their type.
 */
public class KeyObject {

  public Object key;
  public String keyType;

  public KeyObject(Object key) {
    this.key = key;
    this.keyType = key.getClass().getName();
  }
}
