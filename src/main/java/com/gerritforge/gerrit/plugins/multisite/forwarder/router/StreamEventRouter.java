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

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedEventDispatcher;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.replication.events.RefReplicationDoneEvent;
import java.io.IOException;

public class StreamEventRouter implements ForwardedEventRouter<Event> {
  private final ForwardedEventDispatcher forwardedEventDispatcher;
  private final IndexEventRouter indexEventRouter;

  @Inject
  public StreamEventRouter(
      ForwardedEventDispatcher forwardedEventDispatcher, IndexEventRouter indexEventRouter) {
    this.forwardedEventDispatcher = forwardedEventDispatcher;
    this.indexEventRouter = indexEventRouter;
  }

  @Override
  public void route(Event sourceEvent) throws PermissionBackendException, IOException {
    if (RefReplicationDoneEvent.TYPE.equals(sourceEvent.getType())) {
      /* TODO: We currently explicitly ignore the status and result of the replication
       * event because there isn't a reliable way to understand if the current node was
       * the replication target and was successful or not.
       *
       * It is better to risk to reindex once more rather than missing a reindexing event.
       */
      indexEventRouter.onRefReplicated((RefReplicationDoneEvent) sourceEvent);
    }

    forwardedEventDispatcher.dispatch(sourceEvent);
  }
}
