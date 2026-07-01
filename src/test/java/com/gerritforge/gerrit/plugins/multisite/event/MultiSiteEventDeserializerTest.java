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
  private EventDeserializer deserializer;

  @BeforeClass
  public static void setupClass() {
    MultiSiteEvent.registerEventTypes();
  }

  @Before
  public void setUp() {
    Gson gson = new EventGsonProvider().get();
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

  private static void assertAccountIndexEventEquals(
      Event event, AccountIndexEvent expectedAccountIndexEvent) {
    assertThat(event).isInstanceOf(AccountIndexEvent.class);
    AccountIndexEvent accountIndexEvent = (AccountIndexEvent) event;
    assertThat(accountIndexEvent).isEqualTo(expectedAccountIndexEvent);
  }
}
