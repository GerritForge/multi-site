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
import com.google.gerrit.server.util.time.TimeUtil;
import java.util.Objects;

public abstract class MultiSiteEvent extends Event {
  @Nullable public Meta meta;

  public record Meta(Retry retry) {}

  public static class Retry {
    public int retryCount;
    public long lastAttemptOn;
    public String retryingInstanceId;

    public Retry(int retryCount, long lastAttemptOn, String retryingInstanceId) {
      this.retryCount = retryCount;
      this.lastAttemptOn = lastAttemptOn;
      this.retryingInstanceId = retryingInstanceId;
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

  public void markRetry(String retryingInstanceId) {
    meta = new Meta(new Retry(getRetryCount() + 1, TimeUtil.nowMs(), retryingInstanceId));
  }

  public abstract MultiSiteEvent copy();

  protected <T extends MultiSiteEvent> T copyBaseTo(T event) {
    event.instanceId = instanceId;
    event.eventCreatedOn = eventCreatedOn;
    event.meta = copyMeta();
    return event;
  }

  @Nullable
  private Meta copyMeta() {
    Retry retry = getRetry();
    if (retry == null) {
      return null;
    }
    return new Meta(new Retry(retry.retryCount, retry.lastAttemptOn, retry.retryingInstanceId));
  }

  public boolean isRetry() {
    return getRetry() != null;
  }

  public int getRetryCount() {
    Retry retry = getRetry();
    if (retry != null) {
      return retry.retryCount;
    }
    return 0;
  }

  public long getLastAttemptOn() {
    Retry retry = getRetry();
    if (retry != null) {
      return retry.lastAttemptOn;
    }
    return 0;
  }

  public String getRetryingInstanceId() {
    Retry retry = getRetry();
    if (retry != null) {
      return retry.retryingInstanceId;
    }
    return null;
  }

  private Retry getRetry() {
    if (meta == null) {
      return null;
    }
    return meta.retry;
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
        && isRetry() == event.isRetry()
        && getRetryCount() == event.getRetryCount()
        && getLastAttemptOn() == event.getLastAttemptOn()
        && Objects.equals(type, event.type)
        && Objects.equals(instanceId, event.instanceId)
        && Objects.equals(getRetryingInstanceId(), event.getRetryingInstanceId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type,
        eventCreatedOn,
        instanceId,
        isRetry(),
        getRetryCount(),
        getLastAttemptOn(),
        getRetryingInstanceId());
  }
}
