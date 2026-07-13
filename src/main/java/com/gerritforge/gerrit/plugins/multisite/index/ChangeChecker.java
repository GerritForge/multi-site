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

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.server.notedb.ChangeNotes;
import java.io.IOException;
import java.util.Optional;

/** Encapsulates the logic of verifying the up-to-date status of a change. */
public interface ChangeChecker extends IndexEntityChecker<String, ChangeIndexEvent> {

  /**
   * Return the Change nodes read from ReviewDb or NoteDb.
   *
   * @param changeId project~changeNumber change-id
   * @return notes of the Change
   */
  Optional<ChangeNotes> getChangeNotes(String changeId);

  /**
   * Create a new index event POJO associated with the current Change.
   *
   * @param projectName change project name
   * @param changeId change number
   * @param deleted marker whether or not this event for delete or replace the change in the change
   *     index
   * @return new IndexEvent
   * @throws IOException if the current Change cannot read
   */
  Optional<ChangeIndexEvent> newIndexEvent(String projectName, int changeId, boolean deleted)
      throws IOException;
}
