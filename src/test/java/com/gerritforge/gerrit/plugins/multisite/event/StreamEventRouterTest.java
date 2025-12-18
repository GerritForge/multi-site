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

import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedEventDispatcher;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.IndexEventRouter;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.StreamEventRouter;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.util.time.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamEventRouterTest {

  private StreamEventRouter router;
  @Mock private ForwardedEventDispatcher forwardedEventDispatcher;
  @Mock private IndexEventRouter indexEventRouter;

  @Before
  public void setUp() {
    router = new StreamEventRouter(forwardedEventDispatcher, indexEventRouter);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_StreamEvent() throws Exception {
    final CommentAddedEvent event = new CommentAddedEvent(aChange());
    router.route(event);
    verify(forwardedEventDispatcher).dispatch(event);
  }

  private Change aChange() {
    return new Change(
        Change.key("Iabcd1234abcd1234abcd1234abcd1234abcd1234"),
        Change.id(1),
        Account.id(1),
        BranchNameKey.create("proj", "refs/heads/master"),
        TimeUtil.now());
  }
}
