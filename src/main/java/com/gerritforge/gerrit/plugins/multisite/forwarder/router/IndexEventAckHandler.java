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

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ProjectIndexEvent;
import com.google.gerrit.index.Index;
import com.google.gerrit.index.project.ProjectIndexCollection;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.index.account.AccountIndexCollection;
import com.google.gerrit.server.index.change.ChangeIndexCollection;
import com.google.gerrit.server.index.group.GroupIndexCollection;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;

public class IndexEventAckHandler {
  private enum IndexType {
    ACCOUNT,
    CHANGE,
    GROUP,
    PROJECT
  }

  private final AccountIndexCollection accountIndexes;
  private final ChangeIndexCollection changeIndexes;
  private final GroupIndexCollection groupIndexes;
  private final ProjectIndexCollection projectIndexes;
  private final long ackIntervalMs;
  private final Object ackLock = new Object();
  private final EnumSet<IndexType> dirtyIndexes = EnumSet.noneOf(IndexType.class);
  private long lastAckTs;

  @Inject
  public IndexEventAckHandler(
      AccountIndexCollection accountIndexes,
      ChangeIndexCollection changeIndexes,
      GroupIndexCollection groupIndexes,
      ProjectIndexCollection projectIndexes,
      Configuration cfg) {
    this.accountIndexes = accountIndexes;
    this.changeIndexes = changeIndexes;
    this.groupIndexes = groupIndexes;
    this.projectIndexes = projectIndexes;
    this.ackIntervalMs = cfg.index().ackIntervalMs();
    this.lastAckTs = TimeUtil.nowMs();
  }

  public void ackIfDue(IndexEvent sourceEvent, MessageAcknowledgement<Event> ack)
      throws IOException {
    synchronized (ackLock) {
      markDirty(sourceEvent);
      long now = TimeUtil.nowMs();
      if (ackIntervalMs == 0 || now - lastAckTs >= ackIntervalMs) {
        flushDirtyIndexes();
        ack.ack(sourceEvent);
        dirtyIndexes.clear();
        lastAckTs = now;
      }
    }
  }

  private void markDirty(IndexEvent sourceEvent) {
    switch (sourceEvent.getType()) {
      case AccountIndexEvent.TYPE:
        dirtyIndexes.add(IndexType.ACCOUNT);
        break;
      case ChangeIndexEvent.TYPE:
        dirtyIndexes.add(IndexType.CHANGE);
        break;
      case GroupIndexEvent.TYPE:
        dirtyIndexes.add(IndexType.GROUP);
        break;
      case ProjectIndexEvent.TYPE:
        dirtyIndexes.add(IndexType.PROJECT);
        break;
      default:
        throw new IllegalStateException(
            String.format("No index type to flush for event %s", sourceEvent.getType()));
    }
  }

  private void flushDirtyIndexes() throws IOException {
    for (IndexType indexType : dirtyIndexes) {
      flushAndCommit(indexesFor(indexType));
    }
  }

  private Collection<? extends Index<?, ?>> indexesFor(IndexType indexType) {
    return switch (indexType) {
      case ACCOUNT -> accountIndexes.getWriteIndexes();
      case CHANGE -> changeIndexes.getWriteIndexes();
      case GROUP -> groupIndexes.getWriteIndexes();
      case PROJECT -> projectIndexes.getWriteIndexes();
      default ->
          throw new IllegalStateException(String.format("Unexpected index type %s", indexType));
    };
  }

  private void flushAndCommit(Collection<? extends Index<?, ?>> indexes) throws IOException {
    for (Index<?, ?> index : indexes) {
      index.flushAndCommit();
    }
  }
}
