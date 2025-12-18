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

package com.gerritforge.gerrit.plugins.multisite.event;

import static com.google.gerrit.extensions.registration.PluginName.GERRIT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedEventDispatcher;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexAccountHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexChangeHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexGroupHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexProjectHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.IndexEventRouter;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.StreamEventRouter;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PrivateInternals_DynamicMapImpl;
import com.google.gerrit.server.config.AllUsersName;
import com.google.inject.util.Providers;
import com.googlesource.gerrit.plugins.replication.events.RefReplicationDoneEvent;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventRouterTest {
  private static final String INSTANCE_ID = "instance-id";
  private IndexEventRouter router;
  @Mock private ForwardedIndexAccountHandler indexAccountHandler;
  @Mock private ForwardedIndexChangeHandler indexChangeHandler;
  @Mock private ForwardedIndexGroupHandler indexGroupHandler;
  @Mock private ForwardedIndexProjectHandler indexProjectHandler;
  @Mock private ForwardedEventDispatcher forwardedEventDispatcher;
  private AllUsersName allUsersName = new AllUsersName("All-Users");
  PrivateInternals_DynamicMapImpl<ForwardedIndexingHandler<?, ? extends IndexEvent>> indexHandlers;

  @Before
  public void setUp() {
    indexHandlers = (PrivateInternals_DynamicMapImpl) DynamicMap.emptyMap();
    indexHandlers.put(GERRIT, AccountIndexEvent.TYPE, Providers.of(indexAccountHandler));
    indexHandlers.put(GERRIT, GroupIndexEvent.TYPE, Providers.of(indexGroupHandler));
    indexHandlers.put(GERRIT, ChangeIndexEvent.TYPE, Providers.of(indexChangeHandler));
    indexHandlers.put(GERRIT, ProjectIndexEvent.TYPE, Providers.of(indexProjectHandler));
    router = new IndexEventRouter(indexAccountHandler, indexHandlers, allUsersName, INSTANCE_ID);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_AccountIndex() throws Exception {
    final AccountIndexEvent event = new AccountIndexEvent(1, INSTANCE_ID);
    router.route(event);

    verify(indexAccountHandler).handle(event);

    verifyNoInteractions(indexChangeHandler, indexGroupHandler, indexProjectHandler);
  }

  @Test
  public void streamEventRouterShouldTriggerAccountIndexFlush() throws Exception {
    StreamEventRouter streamEventRouter = new StreamEventRouter(forwardedEventDispatcher, router);

    final AccountIndexEvent event = new AccountIndexEvent(1, INSTANCE_ID);
    router.route(event);

    verify(indexAccountHandler).handle(event);

    verifyNoInteractions(indexChangeHandler, indexGroupHandler, indexProjectHandler);

    streamEventRouter.route(new RefReplicationDoneEvent(allUsersName.get(), "refs/any", 1));

    verify(indexAccountHandler).doAsyncIndex();
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_GroupIndex() throws Exception {
    final String groupId = "12";
    final GroupIndexEvent event = new GroupIndexEvent(groupId, ObjectId.zeroId(), INSTANCE_ID);
    router.route(event);

    verify(indexGroupHandler).handle(event);

    verifyNoInteractions(indexAccountHandler, indexChangeHandler, indexProjectHandler);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ProjectIndex() throws Exception {
    final String projectName = "projectName";
    final ProjectIndexEvent event = new ProjectIndexEvent(projectName, INSTANCE_ID);
    router.route(event);

    verify(indexProjectHandler).handle(event);

    verifyNoInteractions(indexAccountHandler, indexChangeHandler, indexGroupHandler);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ChangeIndex() throws Exception {
    final ChangeIndexEvent event = new ChangeIndexEvent("projectName", 3, false, INSTANCE_ID);
    router.route(event);

    verify(indexChangeHandler).handle(event);

    verifyNoInteractions(indexAccountHandler, indexGroupHandler, indexProjectHandler);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ChangeIndexDelete() throws Exception {
    final ChangeIndexEvent event = new ChangeIndexEvent("projectName", 3, true, INSTANCE_ID);
    router.route(event);

    verify(indexChangeHandler).handle(event);

    verifyNoInteractions(indexAccountHandler, indexGroupHandler, indexProjectHandler);
  }

  @Test
  public void routerShouldIgnoreNotRecognisedEvents() throws Exception {
    final IndexEvent newEventType = new IndexEvent("new-type", INSTANCE_ID) {};

    router.route(newEventType);
    verifyNoInteractions(
        indexAccountHandler, indexChangeHandler, indexGroupHandler, indexProjectHandler);
  }
}
