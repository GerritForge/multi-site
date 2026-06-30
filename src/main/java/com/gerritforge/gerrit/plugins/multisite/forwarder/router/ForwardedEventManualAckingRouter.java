// Copyright (C) 2026 GerritForge, Inc.
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

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheNotFoundException;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import java.io.IOException;

public interface ForwardedEventManualAckingRouter<EventType extends Event> {
  void route(EventType event, MessageAcknowledgement<Event> ack)
      throws IOException, PermissionBackendException, CacheNotFoundException;

  void ack(EventType event, MessageAcknowledgement<Event> ack) throws IOException;
}
