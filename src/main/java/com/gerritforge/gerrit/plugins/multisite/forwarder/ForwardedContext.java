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

import static com.gerritforge.gerrit.plugins.replication.pull.api.PullReplicationEndpoints.APPLY_OBJECTS_API_ENDPOINT;
import static com.gerritforge.gerrit.plugins.replication.pull.api.PullReplicationEndpoints.APPLY_OBJECT_API_ENDPOINT;

/** Allows tagging a forwarded event to avoid infinitely looping events. */
public class ForwardedContext implements AutoCloseable {
  public static final String PULL_REPLICATION_PLUGIN_NAME = "pull-replication";
  private static final ThreadLocal<Boolean> forwardedEvent = ThreadLocal.withInitial(() -> false);

  private final boolean previousForwardedState;

  private ForwardedContext() {
    previousForwardedState = setForwardedEvent(true);
  }

  @Override
  public void close() {
    setForwardedEvent(previousForwardedState);
  }

  public static ForwardedContext open() {
    return new ForwardedContext();
  }

  public static Boolean isForwardedEvent() {
    return forwardedEvent.get()
        ||
        // When the event is a result of a pull-replication event, it is considered as
        // "forwarded" action because it did not happen on this node.
        isPullReplicationApplyObjectIndexing();
  }

  private Boolean setForwardedEvent(Boolean b) {
    Boolean previousForwardedState = forwardedEvent.get();
    forwardedEvent.set(b);
    return previousForwardedState;
  }

  private static boolean isPullReplicationApplyObjectIndexing() {
    String threadName = Thread.currentThread().getName();
    return threadName.contains(PULL_REPLICATION_PLUGIN_NAME + "~" + APPLY_OBJECT_API_ENDPOINT)
        || threadName.contains(PULL_REPLICATION_PLUGIN_NAME + "~" + APPLY_OBJECTS_API_ENDPOINT);
  }
}
