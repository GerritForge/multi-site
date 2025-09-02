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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.index.change.ChangeIndex;
import com.google.gerrit.server.index.change.ChangeIndexCollection;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.googlesource.gerrit.plugins.deleteproject.AllProjectChangesDeletedFromIndexEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedProjectEventHandlerTest {

  @Mock OneOffRequestContext oneOffCtxMock;
  private ForwardedProjectEventHandler handler;
  @Mock private ChangeIndexCollection indexes;
  @Mock private ChangeIndex changeIndex;

  @Before
  public void setUp() throws Exception {
    when(indexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    handler = new ForwardedProjectEventHandler(oneOffCtxMock, indexes);
  }

  @Test
  public void testDeleteAllProjectChangesFromIndex() {
    Project.NameKey project = Project.nameKey("foo");
    AllProjectChangesDeletedFromIndexEvent event = new AllProjectChangesDeletedFromIndexEvent();
    event.projectName = project.get();

    handler.handleAllProjectChangesDeletedFromIndexEvent(event);

    verify(changeIndex).deleteAllForProject(project);
  }
}
