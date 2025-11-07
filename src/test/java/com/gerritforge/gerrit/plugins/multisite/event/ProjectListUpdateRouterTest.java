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

import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedProjectListUpdateHandler;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectListUpdateEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ProjectListUpdateRouter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectListUpdateRouterTest {

  private ProjectListUpdateRouter router;
  @Mock private ForwardedProjectListUpdateHandler projectListUpdateHandler;

  @Before
  public void setUp() {
    router = new ProjectListUpdateRouter(projectListUpdateHandler);
  }

  @Test
  public void routerShouldSendEventsToTheAppropriateHandler_ProjectListUpdate() throws Exception {
    String instanceId = "instance-id";
    final ProjectListUpdateEvent event = new ProjectListUpdateEvent("project", false, instanceId);
    router.route(event);

    verify(projectListUpdateHandler).update(event);
  }
}
