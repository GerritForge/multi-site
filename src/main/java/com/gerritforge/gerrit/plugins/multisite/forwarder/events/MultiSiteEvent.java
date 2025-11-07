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

import static com.google.gerrit.server.events.EventTypes.register;

import com.google.gerrit.server.events.Event;

public abstract class MultiSiteEvent extends Event {

  public static void registerEventTypes() {
    register(ChangeIndexEvent.TYPE, ChangeIndexEvent.class);
    register(AccountIndexEvent.TYPE, AccountIndexEvent.class);
    register(GroupIndexEvent.TYPE, GroupIndexEvent.class);
    register(ProjectIndexEvent.TYPE, ProjectIndexEvent.class);
    register(CacheEvictionEvent.TYPE, CacheEvictionEvent.class);
    register(ProjectListUpdateEvent.TYPE, ProjectListUpdateEvent.class);
  }

  protected MultiSiteEvent(String type, String instanceId) {
    super(type);
    this.instanceId = instanceId;
  }
}
