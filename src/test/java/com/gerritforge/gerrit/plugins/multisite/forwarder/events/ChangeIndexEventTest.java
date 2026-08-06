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

package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ChangeIndexEventTest {
  private static final String INSTANCE_ID = "instance-id";
  private static final String PROJECT_NAME = "test-project";
  private static final int CHANGE_ID = 1;
  private static final String TARGET_SHA = "abcd1234";

  @Test
  public void shouldBeEqualWhenEventCreatedOnIsTheSame() {
    ChangeIndexEvent event = newChangeIndexEvent(INSTANCE_ID, 1000L);
    ChangeIndexEvent sameEvent = newChangeIndexEvent(INSTANCE_ID, 1000L);

    assertThat(event).isEqualTo(sameEvent);
    assertThat(event.hashCode()).isEqualTo(sameEvent.hashCode());
  }

  @Test
  public void shouldNotBeEqualWhenEventCreatedOnIsDifferent() {
    ChangeIndexEvent event = newChangeIndexEvent(INSTANCE_ID, 1000L);
    ChangeIndexEvent eventWithDifferentTimestamp = newChangeIndexEvent(INSTANCE_ID, 1001L);

    assertThat(event).isNotEqualTo(eventWithDifferentTimestamp);
  }

  @Test
  public void shouldBeEqualWhenInstanceIdIsTheSame() {
    long eventCreatedOn = 1000L;
    ChangeIndexEvent event = newChangeIndexEvent(INSTANCE_ID, eventCreatedOn);
    ChangeIndexEvent sameEvent = newChangeIndexEvent(INSTANCE_ID, eventCreatedOn);

    assertThat(event).isEqualTo(sameEvent);
    assertThat(event.hashCode()).isEqualTo(sameEvent.hashCode());
  }

  @Test
  public void shouldNotBeEqualWhenInstanceIdIsDifferent() {
    long eventCreatedOn = 1000L;
    ChangeIndexEvent event = newChangeIndexEvent(INSTANCE_ID, eventCreatedOn);
    ChangeIndexEvent eventWithDifferentInstanceId =
        newChangeIndexEvent("anotherInstanceId", eventCreatedOn);

    assertThat(event).isNotEqualTo(eventWithDifferentInstanceId);
  }

  private static ChangeIndexEvent newChangeIndexEvent(String instanceId, long eventCreatedOn) {
    ChangeIndexEvent event = new ChangeIndexEvent(PROJECT_NAME, CHANGE_ID, false, instanceId);
    event.targetSha = TARGET_SHA;
    event.eventCreatedOn = eventCreatedOn;

    return event;
  }
}
