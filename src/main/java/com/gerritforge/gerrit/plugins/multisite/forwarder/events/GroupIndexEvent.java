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
import org.eclipse.jgit.lib.ObjectId;

public class GroupIndexEvent extends IndexEvent {
  public static final String TYPE = "group-index";

  public final String groupUUID;
  public final ObjectId sha1;

  public GroupIndexEvent(String groupUUID, @Nullable ObjectId sha1, String instanceId) {
    super(TYPE, instanceId);
    this.groupUUID = groupUUID;
    this.sha1 = sha1;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GroupIndexEvent that = (GroupIndexEvent) o;
    return Objects.equal(groupUUID, that.groupUUID) && Objects.equal(sha1, that.sha1);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(groupUUID, sha1);
  }
}
