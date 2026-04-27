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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.entities.RefNames.changeMetaRef;

import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler.Operation;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.index.ChangeCheckerImpl;
import com.gerritforge.gerrit.plugins.multisite.index.ForwardedIndexExecutor;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.entities.Change;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.gerrit.server.update.context.RefUpdateContext;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Ignore;
import org.junit.Test;

@TestPlugin(
    name = "multi-site",
    sysModule =
        "com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexChangeHandlerIT$TestModule")
public class ForwardedIndexChangeHandlerIT extends LightweightPluginDaemonTest {

  private static final String IMPORTED_SERVER_ID = "imported-server-id";

  private ForwardedIndexChangeHandler objectUnderTest;
  @Inject private Provider<InternalChangeQuery> queryProvider;

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();
    objectUnderTest = plugin.getSysInjector().getInstance(ForwardedIndexChangeHandler.class);
  }

  public static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new ForwarderModule());
      install(ChangeCheckerImpl.module());
      bind(ScheduledExecutorService.class)
          .annotatedWith(ForwardedIndexExecutor.class)
          .toInstance(new ScheduledThreadPoolExecutor(1));
      bind(SharedRefDbConfiguration.class)
          .toInstance(new SharedRefDbConfiguration(new Config(), "multi-site"));
    }
  }

  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId")
  public void deleteChangesFromIndex() throws Exception {
    PushOneCommit.Result change1 = createChange();
    int changeNum1 = change1.getChange().getId().get();
    Change.Id changeId1 = Change.id(changeNum1);

    PushOneCommit.Result change2 = createChange();
    int changeNum2 = change2.getChange().getId().get();
    Change.Id changeId2 = Change.id(changeNum2);

    assertThat(queryProvider.get().byChangeNumber(changeId1)).isNotEmpty();
    assertThat(queryProvider.get().byChangeNumber(changeId2)).isNotEmpty();

    objectUnderTest.index(
        "~" + changeNum1,
        Operation.DELETE,
        Optional.of(new ChangeIndexEvent("", changeNum1, true, "testInstanceId")));

    objectUnderTest.index(
        project.get() + "~" + changeNum2,
        Operation.DELETE,
        Optional.of(new ChangeIndexEvent(project.get(), changeNum2, true, "testInstanceId")));

    assertThat(queryProvider.get().byChangeNumber(changeId1)).isEmpty();
    assertThat(queryProvider.get().byChangeNumber(changeId2)).isEmpty();
  }

  // Ignoring this IT test end up using the AbstractFakeIndex anyway, which doesn't behave the same
  // as the real Change Index when it comes to imported changes. and therefore it's not representative.
  @Ignore
  @Test
  @GerritConfig(name = "gerrit.instanceId", value = "testInstanceId")
  @GerritConfig(name = "gerrit.importedServerId", value = IMPORTED_SERVER_ID)
  public void deleteImportedChangesFromIndex() throws Exception {
    PushOneCommit.Result change1 = createImportedChange();
    int changeNum1 = change1.getChange().getId().get();
    Change.Id changeId1 = Change.id(changeNum1);

    PushOneCommit.Result change2 = createImportedChange();
    int changeNum2 = change2.getChange().getId().get();
    Change.Id changeId2 = Change.id(changeNum2);

    assertThat(queryProvider.get().byChangeNumber(changeId1)).isNotEmpty();
    assertThat(queryProvider.get().byChangeNumber(changeId2)).isNotEmpty();

    objectUnderTest.index(
        "~" + changeNum1,
        Operation.DELETE,
        Optional.of(new ChangeIndexEvent(project.get(), changeNum1, true, "testInstanceId")));

    objectUnderTest.index(
        project.get() + "~" + changeNum2,
        Operation.DELETE,
        Optional.of(new ChangeIndexEvent("", changeNum2, true, "testInstanceId")));

    assertThat(queryProvider.get().byChangeNumber(changeId1)).isEmpty();
    assertThat(queryProvider.get().byChangeNumber(changeId2)).isEmpty();
  }

  /**
   * Creates a change whose NoteDb server ID is set to {@value IMPORTED_SERVER_ID}, causing the
   * index to store a virtual ID that differs from the physical change number. Mirrors the pattern
   * used in Gerrit's {@code ChangeIT.createImportedChange}.
   */
  private PushOneCommit.Result createImportedChange() throws Exception {
    PushOneCommit.Result change = createChange();
    Change.Id changeId = change.getChange().getId();

    indexer.delete(change.getChange().project(), changeId);

    try (Repository repo = repoManager.openRepository(project);
        ObjectInserter inserter = repo.newObjectInserter();
        ObjectReader reader = repo.newObjectReader();
        RevWalk revWalk = new RevWalk(reader);
        RefUpdateContext ctx =
            RefUpdateContext.openDirectPush(Optional.of("Create imported change"))) {

      Ref ref = repo.getRefDatabase().exactRef(changeMetaRef(changeId));
      RevCommit tip = revWalk.parseCommit(ref.getObjectId());

      CommitBuilder commit = new CommitBuilder();
      commit.setTreeId(tip.getTree());
      commit.setAuthor(
          new PersonIdent("Gerrit User " + admin.id(), admin.id() + "@" + IMPORTED_SERVER_ID));
      commit.setCommitter(new PersonIdent("Gerrit Code Review", admin.email()));
      commit.setMessage(tip.getFullMessage());

      ObjectId commitId = inserter.insert(commit);
      inserter.flush();

      var refUpdate = repo.updateRef(changeMetaRef(changeId));
      refUpdate.setNewObjectId(commitId);
      refUpdate.forceUpdate();

      indexer.index(project, changeId);
    }

    return change;
  }
}
