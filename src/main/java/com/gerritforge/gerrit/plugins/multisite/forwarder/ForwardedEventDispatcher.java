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

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventDispatcher;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.StreamEventRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatch event to the {@link EventDispatcher}. This class is meant to be used on the receiving
 * side of the {@link StreamEventRouter} since it will prevent dispatched event to be forwarded
 * again causing an infinite forwarding loop between the 2 nodes.
 */
@Singleton
public class ForwardedEventDispatcher {
  private static final Logger log = LoggerFactory.getLogger(ForwardedEventDispatcher.class);

  private final DynamicItem<EventDispatcher> dispatcher;
  private final OneOffRequestContext oneOffCtx;

  @Inject
  public ForwardedEventDispatcher(
      DynamicItem<EventDispatcher> dispatcher, OneOffRequestContext oneOffCtx) {
    this.dispatcher = dispatcher;
    this.oneOffCtx = oneOffCtx;
  }

  /**
   * Dispatch an event in the local node, event will not be forwarded to the other node.
   *
   * @param event The event to dispatch
   */
  public void dispatch(Event event) throws PermissionBackendException {
    try (ManualRequestContext ctx = oneOffCtx.open()) {
      log.debug("dispatching event {}", event.getType());
      dispatcher.get().postEvent(event);
    }
  }
}
