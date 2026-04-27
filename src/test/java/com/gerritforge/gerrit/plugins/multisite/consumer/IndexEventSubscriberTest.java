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

package com.gerritforge.gerrit.plugins.multisite.consumer;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheNotFoundException;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ForwardedEventRouter;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.IndexEventRouter;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.change.ChangeFinder;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

public class IndexEventSubscriberTest extends AbstractSubscriberTestBase {
  private static final boolean DELETED = false;
  private static final int CHANGE_ID = 1;
  private static final String EMPTY_PROJECT_NAME = "";

  @Mock protected ChangeFinder changeFinderMock;

  @SuppressWarnings("unchecked")
  @Test
  public void shouldConsumeNonProjectAndNonChangeIndexingEventsTypes()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    IndexEvent event = new AccountIndexEvent(1, INSTANCE_ID);

    objectUnderTest.getConsumer().accept(event);

    verify(projectsFilter, never()).matches(PROJECT_NAME);
    verify(eventRouter, times(1)).route(event);
    verify(droppedEventListeners, never()).onEventDropped(event);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldThrowProjectNameNotFoundExceptionForDeletedChangeWithEmptyProjectName()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    ChangeIndexEvent event = new ChangeIndexEvent(EMPTY_PROJECT_NAME, CHANGE_ID, true, INSTANCE_ID);

    assertThrows(
        ProjectNameNotFoundException.class, () -> objectUnderTest.getConsumer().accept(event));

    verify(projectsFilter, never()).matches(any(String.class));
    verify(eventRouter, never()).route(event);
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected ForwardedEventRouter eventRouter() {
    return mock(IndexEventRouter.class);
  }

  @Override
  protected List<Event> events() {
    return ImmutableList.of(
        new ProjectIndexEvent(PROJECT_NAME, INSTANCE_ID),
        new ChangeIndexEvent(PROJECT_NAME, CHANGE_ID, DELETED, INSTANCE_ID));
  }

  @Override
  protected AbstractSubcriber objectUnderTest() {
    return new IndexEventSubscriber(
        (IndexEventRouter) eventRouter,
        asDynamicSet(droppedEventListeners),
        NODE_INSTANCE_ID,
        msgLog,
        subscriberMetrics,
        cfg,
        projectsFilter,
        changeFinderMock);
  }
}
