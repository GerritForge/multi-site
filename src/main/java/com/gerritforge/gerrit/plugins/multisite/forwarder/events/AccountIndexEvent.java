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

import com.google.common.base.Objects;

public class AccountIndexEvent extends IndexEvent {
  public static final String TYPE = "account-index";

  public int accountId;

  public AccountIndexEvent(int accountId, String instanceId) {
    super(TYPE, instanceId);
    this.accountId = accountId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccountIndexEvent that = (AccountIndexEvent) o;
    return accountId == that.accountId;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(accountId);
  }
}
