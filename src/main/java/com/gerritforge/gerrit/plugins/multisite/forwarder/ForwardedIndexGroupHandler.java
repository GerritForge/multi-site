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

import static com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler.Operation.INDEX;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.GroupIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.IndexEvent;
import com.gerritforge.gerrit.plugins.multisite.index.ForwardedIndexExecutor;
import com.gerritforge.gerrit.plugins.multisite.index.GroupChecker;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.server.index.group.GroupIndexer;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Index a group using {@link GroupIndexer}. This class is meant to be used on the receiving side of
 * the {@link IndexEventForwarder} since it will prevent indexed group to be forwarded again causing
 * an infinite forwarding loop between the 2 nodes. It will also make sure no concurrent indexing is
 * done for the same group uuid
 */
@Singleton
public class ForwardedIndexGroupHandler
    extends ForwardedIndexingHandlerWithRetries<String, GroupIndexEvent> {
  private final GroupIndexer indexer;
  private final GroupChecker groupChecker;

  @Inject
  ForwardedIndexGroupHandler(
      GroupIndexer indexer,
      Configuration config,
      GroupChecker groupChecker,
      OneOffRequestContext oneOffRequestContext,
      @ForwardedIndexExecutor ScheduledExecutorService indexExecutor) {
    super(indexExecutor, config, oneOffRequestContext);
    this.indexer = indexer;
    this.groupChecker = groupChecker;
  }

  @Override
  public void handle(IndexEvent sourceEvent) throws IOException {
    if (sourceEvent instanceof GroupIndexEvent groupIndexEvent) {
      index(groupIndexEvent.groupUUID, INDEX, Optional.of(groupIndexEvent));
    }
  }

  @Override
  protected void doIndex(String uuid, Optional<GroupIndexEvent> event) {
    scheduleIndexing(uuid, event, this::reindex);
  }

  @Override
  protected void reindex(String id) {
    indexer.index(AccountGroup.uuid(id));
  }

  @Override
  protected String indexName() {
    return "group";
  }

  @Override
  protected void attemptToIndex(String uuid) {
    reindexAndCheckIsUpToDate(uuid, groupChecker);
  }

  @Override
  protected void doDelete(String uuid, Optional<GroupIndexEvent> event) {
    throw new UnsupportedOperationException("Delete from group index not supported");
  }
}
