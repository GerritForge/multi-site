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
import com.google.gerrit.common.Nullable;

public class AccountIndexEvent extends IndexEvent {
  public static final String TYPE = "account-index";

  public int accountId;
  @Nullable public String targetSha;
  public boolean deleted;

  public AccountIndexEvent(
      int accountId, @Nullable String targetSha, String instanceId, boolean deleted) {
    super(TYPE, instanceId);
    this.accountId = accountId;
    this.targetSha = targetSha;
    this.deleted = deleted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccountIndexEvent that = (AccountIndexEvent) o;
    return accountId == that.accountId
        && Objects.equal(targetSha, that.targetSha)
        && deleted == that.deleted;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(accountId, targetSha, deleted);
  }
}
