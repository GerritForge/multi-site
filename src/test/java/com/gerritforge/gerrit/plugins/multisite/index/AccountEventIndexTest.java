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

package com.gerritforge.gerrit.plugins.multisite.index;

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gson.Gson;
import org.junit.Test;

public class AccountEventIndexTest {
  private static final Gson gson = new EventGsonProvider().get();

  @Test
  public void shouldSerializeTargetSha() {
    AccountIndexEvent event =
        new AccountIndexEvent(1, "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", "instance-id");

    assertThat(gson.fromJson(gson.toJson(event), AccountIndexEvent.class)).isEqualTo(event);
  }

  @Test
  public void shouldReadEventWithoutTargetSha() {
    AccountIndexEvent event =
        gson.fromJson(
            "{\"accountId\":1,\"type\":\"account-index\",\"instanceId\":\"instance-id\"}",
            AccountIndexEvent.class);

    assertThat(event.targetSha).isNull();
  }
}
