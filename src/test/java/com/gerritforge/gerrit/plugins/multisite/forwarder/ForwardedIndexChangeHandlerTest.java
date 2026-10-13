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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.entities.RefNames.changeMetaRef;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandler.Operation;
import com.gerritforge.gerrit.plugins.multisite.forwarder.ForwardedIndexingHandlerWithRetries.IndexingResult;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.gerritforge.gerrit.plugins.multisite.index.ChangeChecker;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedIndexChangeHandlerTest {

  private static final int TEST_CHANGE_NUMBER = 123;
  private static final int MAX_INDEX_TRIES = 1;
  private static final int RETRY_INTERVAL = 5000;
  private static final int RETRY_POLL_INTERVAL = 1000;
  private static final int EVENT_CHANGE_NUMBER = 1;
  private static String TEST_PROJECT = "test/project";
  private static String TEST_CHANGE_ID = TEST_PROJECT + "~" + TEST_CHANGE_NUMBER;
  private static final boolean CHANGE_EXISTS = true;
  private static final boolean CHANGE_DOES_NOT_EXIST = false;
  private static final boolean DO_NOT_THROW_STORAGE_EXCEPTION = false;
  private static final boolean THROW_STORAGE_EXCEPTION = true;
  private static final boolean CHANGE_UP_TO_DATE = true;
  private static final boolean CHANGE_OUTDATED = false;
  private static final boolean CHANGE_CONSISTENT = true;
  private static final boolean CHANGE_INCONSISTENT = false;
  private static final Optional<String> DELETED_META_REF = Optional.of(ObjectId.zeroId().name());
  private static final Optional<String> UNKNOWN_META_REF = Optional.empty();

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock private ChangeIndexer indexerMock;
  @Mock private OneOffRequestContext ctxMock;
  @Mock private ManualRequestContext manualRequestContextMock;
  @Mock private ChangeNotes changeNotes;
  @Mock private Configuration configurationMock;
  @Mock private Configuration.Index index;
  @Mock private ScheduledExecutorService indexExecutorMock;
  @Mock private GitRepositoryManager gitRepoMgrMock;
  @Mock private ChangeChecker changeCheckerPresentMock;
  @Mock private Provider<InternalChangeQuery> queryProviderMock;
  @Mock private InternalChangeQuery queryMock;
  @Mock private ChangeData changeDataMock;
  @Mock private GlobalRefDatabase globalRefDb;
  private ForwardedIndexChangeHandler handler;
  private DynamicItem<GlobalRefDatabase> globalRefDbItem;
  private Change.Id id;
  private final ChangeIndexEvent deleteEvent =
      new ChangeIndexEvent(TEST_PROJECT, TEST_CHANGE_NUMBER, true, "instance-id");
  private final ChangeIndexEvent changeIndexEvent =
      new ChangeIndexEvent(TEST_PROJECT, TEST_CHANGE_NUMBER, false, "instance-id");

  @Before
  public void setUp() throws Exception {
    when(ctxMock.open()).thenReturn(manualRequestContextMock);
    id = Change.id(TEST_CHANGE_NUMBER);
    when(configurationMock.index()).thenReturn(index);
    when(index.maxTries()).thenReturn(MAX_INDEX_TRIES);
    when(index.retryPollInterval()).thenReturn(RETRY_POLL_INTERVAL);
    globalRefDbItem = DynamicItem.itemOf(GlobalRefDatabase.class, globalRefDb);
    handler = newHandler();
  }

  @Test
  public void changeIsIndexedWhenUpToDate() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_UP_TO_DATE, CHANGE_CONSISTENT);
    handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty());
    verify(indexerMock, times(1)).index(any(ChangeNotes.class));
  }

  @Test
  public void shouldIndexSynchronouslyWhenChangeBecomesReady() throws Exception {
    setupChangeAccessRelatedMocks(
        CHANGE_EXISTS,
        DO_NOT_THROW_STORAGE_EXCEPTION,
        CHANGE_UP_TO_DATE,
        CHANGE_INCONSISTENT,
        CHANGE_CONSISTENT);

    for (int i = 0; i < MAX_INDEX_TRIES; i++) {
      assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.RETRY);
    }
    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.SUCCESS);

    verify(indexerMock).index(changeNotes);
  }

  @Test
  public void shouldNotIndexSynchronouslyWhenChangeIsNotUpToDate() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_OUTDATED, CHANGE_CONSISTENT);

    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.RETRY);

    verify(indexerMock, never()).index(any(ChangeNotes.class));
    verifyNoInteractions(globalRefDb);
    verify(configurationMock, never()).getSharedRefDbConfiguration();
  }

  @Test
  public void shouldIgnoreSynchronousUpdateWhenChangeIsDeleted() throws Exception {
    setGlobalMetaRef(DELETED_META_REF);
    when(changeCheckerPresentMock.getChangeNotes(TEST_CHANGE_ID)).thenReturn(Optional.empty());

    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.IGNORED);

    verifyNoInteractions(indexExecutorMock, indexerMock);
    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);
  }

  @Test
  public void shouldNotCheckGlobalRefDbForAsynchronousRetriesOfMissingChange() throws Exception {
    when(changeCheckerPresentMock.getChangeNotes(TEST_CHANGE_ID)).thenReturn(Optional.empty());

    handler.handle(changeIndexEvent);
    handler.attemptToIndex(TEST_CHANGE_ID);

    verify(indexExecutorMock).schedule(any(Runnable.class), anyLong(), any());
    verifyNoInteractions(indexerMock, globalRefDb);
    verify(configurationMock, never()).getSharedRefDbConfiguration();
  }

  @Test
  public void shouldIgnoreRequeuedUpdateAfterChangeDeletion() throws Exception {
    setupChangeAbsentFromIndex();
    setGlobalMetaRef(UNKNOWN_META_REF);
    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.RETRY);
    assertThat(handler.indexingRetryTaskMap).containsKey(TEST_CHANGE_ID);

    assertThat(handler.handleSync(deleteEvent)).isEqualTo(IndexingResult.SUCCESS);
    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);

    setGlobalMetaRef(DELETED_META_REF);
    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.IGNORED);

    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);
  }

  @Test
  public void shouldRetrySynchronousUpdateWhenGlobalRefDbLookupFails() throws Exception {
    enableGlobalRefDb();
    when(globalRefDb.get(Project.nameKey(TEST_PROJECT), changeMetaRef(id), String.class))
        .thenThrow(new GlobalRefDbSystemError("Cannot read meta ref", new IOException()));

    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.RETRY);

    assertThat(handler.indexingRetryTaskMap).containsKey(TEST_CHANGE_ID);
    verifyNoInteractions(indexExecutorMock, indexerMock);
  }

  @Test
  public void shouldFailSynchronousIndexingWhenChangeIsNotReady() throws Exception {
    setupChangeAccessRelatedMocks(
        CHANGE_EXISTS,
        DO_NOT_THROW_STORAGE_EXCEPTION,
        CHANGE_UP_TO_DATE,
        CHANGE_INCONSISTENT,
        CHANGE_INCONSISTENT);

    for (int i = 0; i < MAX_INDEX_TRIES; i++) {
      assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.RETRY);
    }
    assertThat(handler.handleSync(changeIndexEvent)).isEqualTo(IndexingResult.FAILURE);
    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);

    verify(indexerMock, never()).index(any(ChangeNotes.class));
  }

  @Test
  public void shouldNotAttemptSynchronousIndexingAgainBeforeRetryInterval() throws Exception {
    when(index.retryInterval()).thenReturn(RETRY_INTERVAL);
    handler = newHandler();
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_UP_TO_DATE, CHANGE_INCONSISTENT);

    IndexingResult initialAttempt = handler.handleSync(changeIndexEvent);
    IndexingResult earlyRedelivery = handler.handleSync(changeIndexEvent);

    assertThat(initialAttempt).isEqualTo(IndexingResult.RETRY);
    assertThat(earlyRedelivery).isEqualTo(IndexingResult.RETRY);
  }

  @Test
  public void shouldDeleteAllProjectChangesSynchronously() throws Exception {
    handler.handleSync(ChangeIndexEvent.allChangesDeletedForProject(TEST_PROJECT, "instance-id"));

    verify(indexerMock).deleteAllForProject(Project.nameKey(TEST_PROJECT));
  }

  @Test
  public void changeIsNotReindexedWhenShaIsNeverVisible() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_OUTDATED, CHANGE_CONSISTENT);
    handler.index(
        TEST_CHANGE_ID,
        Operation.INDEX,
        Optional.of(new ChangeIndexEvent("foo", EVENT_CHANGE_NUMBER, false, "instance-id")));
    verify(indexerMock, never()).index(any(ChangeNotes.class));
  }

  @Test
  public void changeIsIndexeAtFirstRetryWhenInitiallyInconsistent() throws Exception {
    setupChangeAccessRelatedMocks(
        CHANGE_EXISTS,
        DO_NOT_THROW_STORAGE_EXCEPTION,
        CHANGE_UP_TO_DATE,
        CHANGE_INCONSISTENT,
        CHANGE_CONSISTENT);
    handler.index(
        TEST_CHANGE_ID,
        Operation.INDEX,
        Optional.of(new ChangeIndexEvent("foo", EVENT_CHANGE_NUMBER, false, "instance-id")));
    verify(indexerMock, never()).index(any(ChangeNotes.class));
    verify(indexExecutorMock, times(1)).schedule(any(Runnable.class), anyLong(), any());

    handler.index(
        TEST_CHANGE_ID,
        Operation.INDEX,
        Optional.of(new ChangeIndexEvent("foo", EVENT_CHANGE_NUMBER, false, "instance-id")));
    verify(indexerMock, times(1)).index(any(ChangeNotes.class));
  }

  @Test
  public void changeToIndexDoesNotExist() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_DOES_NOT_EXIST, CHANGE_OUTDATED);
    handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty());
    verify(indexerMock, never()).delete(Project.NameKey.parse(TEST_PROJECT), id);
    verify(indexerMock, never()).index(any(Project.NameKey.class), any(Change.Id.class));
  }

  @Test
  public void shouldStopPendingRetryWhenLocallyPresentChangeIsDeleted() throws Exception {
    setupChangePresentInIndex();

    scheduleIndexingRetryForOutdatedChange();
    handleChangeDeletion();
    handler.attemptToIndex(TEST_CHANGE_ID);

    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);
    verify(indexerMock).delete(Project.nameKey(TEST_PROJECT), id);
    verify(indexExecutorMock).schedule(any(Runnable.class), anyLong(), any());
    verify(changeCheckerPresentMock).getChangeNotes(TEST_CHANGE_ID);
    verifyNoInteractions(globalRefDb);
  }

  @Test
  public void shouldStopPendingRetryWhenDeletedChangeIsAlreadyAbsentFromIndex() throws Exception {
    setupChangeAbsentFromIndex();

    scheduleIndexingRetryForMissingChange();
    handleChangeDeletion();
    handler.attemptToIndex(TEST_CHANGE_ID);

    assertThat(handler.indexingRetryTaskMap).doesNotContainKey(TEST_CHANGE_ID);
    verify(indexExecutorMock).schedule(any(Runnable.class), anyLong(), any());
    verify(changeCheckerPresentMock).getChangeNotes(TEST_CHANGE_ID);
    verifyNoInteractions(globalRefDb);
  }

  @Test
  public void indexerThrowsStorageExceptionTryingToIndexChange() throws Exception {
    setupChangeAccessRelatedMocks(
        CHANGE_EXISTS, THROW_STORAGE_EXCEPTION, CHANGE_UP_TO_DATE, CHANGE_CONSISTENT);
    assertThrows(
        StorageException.class,
        () -> handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty()));
  }

  @Test
  public void shouldSetAndUnsetForwardedContext() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_UP_TO_DATE);
    // this doAnswer is to allow to assert that context is set to forwarded
    // while cache eviction is called.
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  assertThat(ForwardedContext.isForwardedEvent()).isTrue();
                  return null;
                })
        .when(indexerMock)
        .index(any(ChangeNotes.class));

    assertThat(ForwardedContext.isForwardedEvent()).isFalse();
    handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty());
    assertThat(ForwardedContext.isForwardedEvent()).isFalse();

    verify(indexerMock, times(1)).index(any(ChangeNotes.class));
  }

  @Test
  public void shouldSetAndUnsetForwardedContextEvenIfExceptionIsThrown() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_UP_TO_DATE);
    doAnswer(
            (Answer<Void>)
                invocation -> {
                  assertThat(ForwardedContext.isForwardedEvent()).isTrue();
                  throw new IOException("someMessage");
                })
        .when(indexerMock)
        .index(any(ChangeNotes.class));

    assertThat(ForwardedContext.isForwardedEvent()).isFalse();
    IOException thrown =
        assertThrows(
            IOException.class,
            () -> handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty()));
    assertThat(thrown).hasMessageThat().isEqualTo("someMessage");
    assertThat(ForwardedContext.isForwardedEvent()).isFalse();

    verify(indexerMock, times(1)).index(any(ChangeNotes.class));
  }

  @Test
  public void deleteWithoutEventThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.index(TEST_CHANGE_ID, Operation.DELETE, Optional.empty()));
    verify(indexerMock, never()).delete(any(), any());
  }

  private void scheduleIndexingRetryForOutdatedChange() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_EXISTS, CHANGE_OUTDATED);
    handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty());
    assertThat(handler.indexingRetryTaskMap).containsKey(TEST_CHANGE_ID);
  }

  private void scheduleIndexingRetryForMissingChange() throws Exception {
    setupChangeAccessRelatedMocks(CHANGE_DOES_NOT_EXIST, CHANGE_OUTDATED);
    handler.index(TEST_CHANGE_ID, Operation.INDEX, Optional.empty());
    assertThat(handler.indexingRetryTaskMap).containsKey(TEST_CHANGE_ID);
  }

  private void handleChangeDeletion() throws IOException {
    handler.index(TEST_CHANGE_ID, Operation.DELETE, Optional.of(deleteEvent));
  }

  private void setupChangePresentInIndex() {
    when(queryProviderMock.get()).thenReturn(queryMock);
    when(queryMock.byProjectChangeNumber(Project.nameKey(TEST_PROJECT), id))
        .thenReturn(List.of(changeDataMock));
    when(changeDataMock.getId()).thenReturn(id);
  }

  private void setupChangeAbsentFromIndex() {
    when(queryProviderMock.get()).thenReturn(queryMock);
    when(queryMock.byProjectChangeNumber(Project.nameKey(TEST_PROJECT), id)).thenReturn(List.of());
  }

  private ForwardedIndexChangeHandler newHandler() {
    return new ForwardedIndexChangeHandler(
        indexerMock,
        configurationMock,
        indexExecutorMock,
        ctxMock,
        changeCheckerPresentMock,
        queryProviderMock,
        globalRefDbItem);
  }

  private void enableGlobalRefDb() {
    Config config = new Config();
    config.setBoolean("ref-database", null, "enabled", true);
    when(configurationMock.getSharedRefDbConfiguration())
        .thenReturn(new SharedRefDbConfiguration(config, "multi-site"));
  }

  private void setGlobalMetaRef(Optional<String> metaRef) {
    enableGlobalRefDb();
    when(globalRefDb.get(Project.nameKey(TEST_PROJECT), changeMetaRef(id), String.class))
        .thenReturn(metaRef);
  }

  private void setupChangeAccessRelatedMocks(boolean changeExist, boolean changeUpToDate)
      throws Exception {
    setupChangeAccessRelatedMocks(
        changeExist,
        DO_NOT_THROW_STORAGE_EXCEPTION,
        changeUpToDate,
        CHANGE_CONSISTENT && changeExist);
  }

  private void setupChangeAccessRelatedMocks(
      boolean changeExist, boolean changeUpToDate, boolean changeConsistent) throws Exception {
    setupChangeAccessRelatedMocks(
        changeExist, DO_NOT_THROW_STORAGE_EXCEPTION, changeUpToDate, changeConsistent);
  }

  private void setupChangeAccessRelatedMocks(
      boolean changeExists,
      boolean storageException,
      boolean changeIsUpToDate,
      boolean... changeConsistentReturnValues)
      throws StorageException {
    if (changeExists) {
      when(changeCheckerPresentMock.getChangeNotes(TEST_CHANGE_ID))
          .thenReturn(Optional.of(changeNotes));
      if (storageException) {
        doThrow(new StorageException("io-error")).when(indexerMock).index(any(ChangeNotes.class));
      }
    }

    when(changeCheckerPresentMock.isUpToDate(any())).thenReturn(changeIsUpToDate);

    OngoingStubbing<Boolean> changeConsistentCall =
        when(changeCheckerPresentMock.isConsistent(any()));
    for (boolean changeConsistent : changeConsistentReturnValues) {
      changeConsistentCall = changeConsistentCall.thenReturn(changeConsistent);
    }
  }
}
