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

package com.gerritforge.gerrit.plugins.multisite.event;

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.eventbroker.EventDeserializer;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.MultiSiteEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiSiteEventDeserializerTest {

  private static final String TEST_INSTANCE_ID = "test-instance-id";
  private static final int TEST_ACCOUNT_ID = 1000000;
  private static final String TEST_SHA1 = "326eca95ad32aa5b65a576db03aa221a545050cd";
  private Gson gson;
  private EventDeserializer deserializer;

  @BeforeClass
  public static void setupClass() {
    MultiSiteEvent.registerEventTypes();
  }

  @Before
  public void setUp() {
    gson = new EventGsonProvider().get();
    deserializer = new EventDeserializer(gson);
  }

  @Test
  public void eventDeserializerShouldParseAccountIndexEvent() {
    AccountIndexEvent testAccountIndexEvent =
        new AccountIndexEvent(TEST_ACCOUNT_ID, TEST_SHA1, TEST_INSTANCE_ID, false);
    String eventJson =
        String.format(
            "{"
                + "\"type\": \"%s\","
                + "\"instanceId\":\"%s\","
                + "\"eventCreatedOn\":%d,"
                + "\"accountId\":%d,"
                + "\"targetSha\":\"%s\""
                + "}",
            AccountIndexEvent.TYPE,
            TEST_INSTANCE_ID,
            testAccountIndexEvent.eventCreatedOn,
            TEST_ACCOUNT_ID,
            TEST_SHA1);

    MultiSiteEvent event = (MultiSiteEvent) deserializer.deserialize(eventJson);
    assertAccountIndexEventEquals(event, testAccountIndexEvent);
    assertThat(event.meta).isNull();
  }

  @Test
  public void eventDeserializerShouldParseAccountIndexDeletedEvent() {
    AccountIndexEvent testAccountIndexEvent =
        new AccountIndexEvent(TEST_ACCOUNT_ID, null, TEST_INSTANCE_ID, true);
    String eventJson =
        String.format(
            "{"
                + "\"type\": \"%s\","
                + "\"instanceId\":\"%s\","
                + "\"eventCreatedOn\":%d,"
                + "\"accountId\":%d,"
                + "\"deleted\":true"
                + "}",
            AccountIndexEvent.TYPE,
            TEST_INSTANCE_ID,
            testAccountIndexEvent.eventCreatedOn,
            TEST_ACCOUNT_ID);

    assertAccountIndexEventEquals(deserializer.deserialize(eventJson), testAccountIndexEvent);
  }

  @Test
  public void eventDeserializerShouldParseLegacyAccountIndexEvent() {
    AccountIndexEvent testAccountIndexEvent =
        new AccountIndexEvent(TEST_ACCOUNT_ID, null, TEST_INSTANCE_ID, false);
    String eventJson =
        String.format(
            "{"
                + "\"type\": \"%s\","
                + "\"instanceId\":\"%s\","
                + "\"eventCreatedOn\":%d,"
                + "\"accountId\":%d"
                + "}",
            AccountIndexEvent.TYPE,
            TEST_INSTANCE_ID,
            testAccountIndexEvent.eventCreatedOn,
            TEST_ACCOUNT_ID);

    assertAccountIndexEventEquals(deserializer.deserialize(eventJson), testAccountIndexEvent);
  }

  @Test
  public void eventDeserializerShouldParseRequeuedAccountIndexEvent() {
    AccountIndexEvent testAccountIndexEvent =
        new AccountIndexEvent(TEST_ACCOUNT_ID, TEST_SHA1, TEST_INSTANCE_ID, false);
    MultiSiteEvent.Requeue requeue =
        new MultiSiteEvent.Requeue(2, 123456789L, "requeuing-instance-id");
    String eventJson =
        String.format(
            "{"
                + "\"type\": \"%s\","
                + "\"instanceId\":\"%s\","
                + "\"eventCreatedOn\":%d,"
                + "\"accountId\":%d,"
                + "\"targetSha\":\"%s\","
                + "\"meta\":{"
                + "\"requeue\":{"
                + "\"retryCount\":2,"
                + "\"requeuedOn\":123456789,"
                + "\"requeuedByInstanceId\":\"requeuing-instance-id\""
                + "}"
                + "}"
                + "}",
            AccountIndexEvent.TYPE,
            TEST_INSTANCE_ID,
            testAccountIndexEvent.eventCreatedOn,
            TEST_ACCOUNT_ID,
            TEST_SHA1);
    testAccountIndexEvent.meta = new MultiSiteEvent.Meta(requeue);
    assertAccountIndexEventEquals(deserializer.deserialize(eventJson), testAccountIndexEvent);
  }

  @Test
  public void eventSerializerShouldOmitMetaWhenEventWasNotRequeued() {
    AccountIndexEvent accountIndexEvent =
        new AccountIndexEvent(TEST_ACCOUNT_ID, TEST_SHA1, TEST_INSTANCE_ID, false);

    String json = gson.toJson(accountIndexEvent, Event.class);

    assertThat(json).doesNotContain("\"meta\"");
    assertThat(json).doesNotContain("\"requeued\"");
    assertThat(json).doesNotContain("\"retryCount\"");
    assertThat(json).doesNotContain("\"requeuedOn\"");
    assertThat(json).doesNotContain("\"requeuedByInstanceId\"");
  }

  private static void assertAccountIndexEventEquals(
      Event event, AccountIndexEvent expectedAccountIndexEvent) {
    assertThat(event).isInstanceOf(AccountIndexEvent.class);
    AccountIndexEvent accountIndexEvent = (AccountIndexEvent) event;
    assertThat(accountIndexEvent).isEqualTo(expectedAccountIndexEvent);
  }
}
