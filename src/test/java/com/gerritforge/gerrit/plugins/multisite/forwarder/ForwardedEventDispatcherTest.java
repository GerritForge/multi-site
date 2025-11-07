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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventDispatcher;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gerrit.server.util.OneOffRequestContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedEventDispatcherTest {

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock private DynamicItem<EventDispatcher> dispatcherMockItem;
  @Mock private EventDispatcher dispatcherMock;
  @Mock OneOffRequestContext oneOffCtxMock;
  private ForwardedEventDispatcher forwardedEventDispatcher;

  @Before
  public void setUp() throws Exception {
    when(dispatcherMockItem.get()).thenReturn(dispatcherMock);
    forwardedEventDispatcher = new ForwardedEventDispatcher(dispatcherMockItem, oneOffCtxMock);
  }

  @Test
  public void testSuccessfulDispatching() throws Exception {
    Event event = new ProjectCreatedEvent();
    forwardedEventDispatcher.dispatch(event);
    verify(dispatcherMock).postEvent(event);
  }
}
