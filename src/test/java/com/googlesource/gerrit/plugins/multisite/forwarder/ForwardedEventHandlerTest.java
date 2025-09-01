// Copyright (C) 2018 The Android Open Source Project
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
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventDispatcher;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gerrit.server.index.change.ChangeIndex;
import com.google.gerrit.server.index.change.ChangeIndexCollection;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.googlesource.gerrit.plugins.deleteproject.AllProjectChangesDeletedFromIndexEvent;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedEventHandlerTest {

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock private DynamicItem<EventDispatcher> dispatcherMockItem;
  @Mock private EventDispatcher dispatcherMock;
  @Mock OneOffRequestContext oneOffCtxMock;
  @Mock private ChangeIndexCollection indexes;
  @Mock private ChangeIndex changeIndex;
  private ForwardedEventHandler handler;

  @Before
  public void setUp() throws Exception {
    when(dispatcherMockItem.get()).thenReturn(dispatcherMock);
    when(indexes.getWriteIndexes()).thenReturn(List.of(changeIndex));
    handler = new ForwardedEventHandler(dispatcherMockItem, oneOffCtxMock, indexes);
  }

  @Test
  public void testSuccessfulDispatching() throws Exception {
    Event event = new ProjectCreatedEvent();
    handler.dispatch(event);
    verify(dispatcherMock).postEvent(event);
  }

  @Test
  public void testDeleteAllProjectChangesFromIndex() {
    Project.NameKey project = Project.nameKey("foo");
    AllProjectChangesDeletedFromIndexEvent event = new AllProjectChangesDeletedFromIndexEvent();
    event.projectName = project.get();

    handler.handleAllProjectChangesDeletedFromIndexEvent(project);

    verify(changeIndex).deleteAllForProject(project);
  }
}
