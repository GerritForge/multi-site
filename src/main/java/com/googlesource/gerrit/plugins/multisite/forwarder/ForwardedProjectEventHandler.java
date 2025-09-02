// Copyright (C) 2025 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.multisite.forwarder;

import com.google.gerrit.server.index.change.ChangeIndexCollection;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.deleteproject.AllProjectChangesDeletedFromIndexEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gerrit.server.events.ProjectEvent;

/**
 * Handle consumed {@link ProjectEvent} events.
 */
@Singleton
public class ForwardedProjectEventHandler {
  private static final Logger log = LoggerFactory.getLogger(ForwardedProjectEventHandler.class);

  private final OneOffRequestContext oneOffCtx;
  private final ChangeIndexCollection indexes;

  @Inject
  public ForwardedProjectEventHandler(OneOffRequestContext oneOffCtx, ChangeIndexCollection indexes) {
    this.oneOffCtx = oneOffCtx;
    this.indexes = indexes;
  }

  /**
   * Handles {@link AllProjectChangesDeletedFromIndexEvent} events by deleting all project changes from the index
   *
   * @param event the event to handle
   */
  public void handleAllProjectChangesDeletedFromIndexEvent(AllProjectChangesDeletedFromIndexEvent event) {
    try (ManualRequestContext ctx = oneOffCtx.open()) {
      Context.setForwardedEvent(true);
      log.warn("Deleting all changes for project {}", event.projectName);
      indexes.getWriteIndexes().forEach(i -> i.deleteAllForProject(event.getProjectNameKey()));
    } finally {
      Context.unsetForwardedEvent();
    }
  }
}
