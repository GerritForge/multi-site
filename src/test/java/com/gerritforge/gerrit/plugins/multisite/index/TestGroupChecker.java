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

package com.gerritforge.gerrit.plugins.multisite.index;

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Ignore;

@Ignore
public class TestGroupChecker implements GroupChecker {

  private final boolean isUpToDate;

  public TestGroupChecker(boolean isUpToDate) {
    this.isUpToDate = isUpToDate;
  }

  private static final String someObjectId = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";

  @Override
  public boolean isUpToDate(Optional<GroupIndexEvent> groupIndexEvent) {
    return isUpToDate;
  }

  @Override
  public ObjectId getGroupHead(String groupUUID) {
    return ObjectId.fromString(someObjectId);
  }
}
