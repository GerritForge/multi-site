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

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import java.util.Optional;

public class ProjectCheckerImpl implements ProjectChecker {
  private final ProjectCache projectCache;

  @Inject
  ProjectCheckerImpl(ProjectCache projectCache) {
    this.projectCache = projectCache;
  }

  @Override
  public boolean isUpToDate(Optional<ProjectIndexEvent> indexEvent) {
    return indexEvent
        .flatMap(event -> projectCache.get(Project.nameKey(event.projectName)))
        .isPresent();
  }
}
