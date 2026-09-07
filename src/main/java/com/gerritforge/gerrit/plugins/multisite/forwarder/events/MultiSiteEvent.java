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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.events.Event;
import java.time.Instant;
import java.util.Objects;

public abstract class MultiSiteEvent extends Event {
  @Nullable public Meta meta;

  public record Meta(Requeue requeue) {}

  public static class Requeue {
    public int retryCount;
    public long requeuedOn;
    public String requeuedByInstanceId;

    public Requeue(int retryCount, long requeuedOn, String requeuedByInstanceId) {
      this.retryCount = retryCount;
      this.requeuedOn = requeuedOn;
      this.requeuedByInstanceId = requeuedByInstanceId;
    }
  }

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

  public void markRequeued(String requeuedByInstanceId) {
    meta =
        new Meta(
            new Requeue(getRetryCount() + 1, Instant.now().getEpochSecond(), requeuedByInstanceId));
  }

  public boolean isRequeued() {
    return getRequeue() != null;
  }

  public int getRetryCount() {
    Requeue requeue = getRequeue();
    if (requeue != null) {
      return requeue.retryCount;
    }
    return 0;
  }

  public long getRequeuedOn() {
    Requeue requeue = getRequeue();
    if (requeue != null) {
      return requeue.requeuedOn;
    }
    return 0;
  }

  public String getRequeuedByInstanceId() {
    Requeue requeue = getRequeue();
    if (requeue != null) {
      return requeue.requeuedByInstanceId;
    }
    return null;
  }

  private Requeue getRequeue() {
    if (meta == null) {
      return null;
    }
    return meta.requeue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MultiSiteEvent)) {
      return false;
    }
    MultiSiteEvent event = (MultiSiteEvent) o;
    return eventCreatedOn == event.eventCreatedOn
        && isRequeued() == event.isRequeued()
        && getRetryCount() == event.getRetryCount()
        && getRequeuedOn() == event.getRequeuedOn()
        && Objects.equals(type, event.type)
        && Objects.equals(instanceId, event.instanceId)
        && Objects.equals(getRequeuedByInstanceId(), event.getRequeuedByInstanceId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type,
        eventCreatedOn,
        instanceId,
        isRequeued(),
        getRetryCount(),
        getRequeuedOn(),
        getRequeuedByInstanceId());
  }
}
