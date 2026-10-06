package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.search.SearchIndexLifecycleManagerImpl.SearchIndexRowHandler;
import org.sagebionetworks.repo.manager.search.SemanticEmbeddingBootstrapper.SemanticEmbeddingModel;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.IndexAuthorizationSnapshotManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.TranslatedQuery;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.IndexDescriptionLookup;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingCallable;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.WriteLock;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;

@ExtendWith(MockitoExtension.class)
public class SearchIndexLifecycleManagerImplTest {

	private static final String ENTITY_ID = "syn456";
	// A benefactor-less source (a table) for the row-handler tests that do not exercise
	// benefactor handling.
	private static final IndexDescription TABLE_INDEX_DESCRIPTION =
			new TableIndexDescription(IdAndVersion.parse("syn123"));
	private static final String DEFINING_SQL = "SELECT * FROM syn789";
	private static final IdAndVersion SOURCE_ID = IdAndVersion.parse("syn789");
	private static final LockContext BUILD_LOCK_CONTEXT =
			new LockContext(ContextType.SearchIndexLifecycle, IdAndVersion.parse(ENTITY_ID));
	private static final ColumnModel NAME_COLUMN = new ColumnModel().setId("100").setName("name")
			.setColumnType(ColumnType.STRING).setMaximumSize(50L);
	private static final IndexAuthorizationSnapshot SOURCE_SNAPSHOT = tableSnapshot("100");

	@Mock
	private ConnectionFactory connectionFactory;
	@Mock
	private OpenSearchManager openSearchManager;
	@Mock
	private SearchConfigurationResolver searchConfigurationResolver;
	@Mock
	private EntityManager entityManager;
	@Mock
	private SynonymSetDao synonymSetDao;
	@Mock
	private ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	@Mock
	private TextAnalyzerDao textAnalyzerDao;
	@Mock
	private SearchIndexStatusDao statusDao;
	@Mock
	private ProgressCallback progressCallback;
	@Mock
	private TableManagerSupport tableManagerSupport;
	@Mock
	private ColumnModelManager columnModelManager;
	@Mock
	private WriteReadSemaphore writeReadSemaphore;
	@Mock
	private WriteLock writeLock;
	@Mock
	private TableIndexDAO indexDao;
	@Mock
	private StackConfiguration stackConfiguration;
	@Mock
	private DefiningSqlDependencyDao definingSqlDependencyDao;
	@Mock
	private IndexAuthorizationSnapshotManager indexAuthorizationSnapshotManager;
	@Mock
	private SemanticEmbeddingBootstrapper semanticEmbeddingBootstrapper;

	@InjectMocks
	private SearchIndexLifecycleManagerImpl manager;

	private static final String LOCK_KEY = "search-index-build:" + ENTITY_ID;

	private void stubBuildLock() throws Exception {
		when(progressCallback.getLockTimeoutSeconds()).thenReturn(300L);
		when(writeReadSemaphore.getWriteLock(any(WriteLockRequest.class))).thenReturn(writeLock);
	}

	private void stubLockUnavailable() throws Exception {
		when(progressCallback.getLockTimeoutSeconds()).thenReturn(300L);
		when(writeReadSemaphore.getWriteLock(any(WriteLockRequest.class)))
				.thenThrow(new LockUnavilableException(LockType.Write, LOCK_KEY, "other-worker"));
	}

	/**
	 * Stubs the full chain so buildIndex reaches indexDao.queryAsStream successfully.
	 * Use only for tests that must reach the streaming phase.
	 */
	private void stubHappyPathThroughStream() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
	}

	/**
	 * Stubs up through getRowCountForTable (source is AVAILABLE, row count = 0) but does NOT take
	 * the source lock or include the stubs needed by QueryTranslator. Use for tests that bail before
	 * the source lock is taken (e.g., the row-count guard).
	 */
	private void stubHappyPathThroughCreateIndex() throws Exception {
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(SOURCE_ID))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(0L);
	}

	/** Runs the build's callable under the non-exclusive lock on the source. */
	private void stubSourceLock() throws Exception {
		doAnswer(invocation -> invocation.<ProgressingCallable<?>>getArgument(2).call(progressCallback))
				.when(tableManagerSupport).tryRunWithTableNonExclusiveLock(eq(progressCallback), eq(BUILD_LOCK_CONTEXT),
						any(ProgressingCallable.class), eq(SOURCE_ID));
	}

	/**
	 * Stubs the source lock, the source's as-built snapshot (column "100"), the translation of the
	 * defining SQL against it, the SearchIndex snapshot built from it, and the AGGREGATE_DATA check.
	 */
	private void stubSchemaProviderForTranslator() throws Exception {
		stubSourceLock();
		stubSourceSnapshotAndTranslation();
	}

	/** {@link #stubSchemaProviderForTranslator()} without the source lock. */
	private void stubSourceSnapshotAndTranslation() throws Exception {
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID))
				.thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		// buildIndex persists each selected column through createColumnModel.
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName()))))
				.thenReturn(NAME_COLUMN);
		when(tableManagerSupport.getAggregateDataConfiguration("syn789")).thenReturn(Optional.empty());
	}

	/** The as-built snapshot of source table syn789 with the given column ids, in order. */
	private static IndexAuthorizationSnapshot tableSnapshot(String... columnIds) {
		List<ColumnLineageEntry> lineage = Arrays.stream(columnIds)
				.map(id -> new ColumnLineageEntry().setOutputColumnId(id).setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(List.of(new SourceColumnReference().setSourceObjectId("syn789").setSourceColumnId(id))))
				.collect(Collectors.toList());
		return new IndexAuthorizationSnapshot()
				.setObjectId("syn789")
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn789")
						.setTableType(TableType.table.name())
						.setBenefactors(List.of())
						.setDependencies(List.of()))
				.setColumnLineage(lineage);
	}

	@Test
	public void testHandleCreateStreamsViaIndexDao() throws Exception {
		// The happy path completes buildIndex and streams rows via indexDao.queryAsStream,
		// writing ACTIVE status at the end.
		stubHappyPathThroughStream();

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(indexDao).queryAsStream(any(), any());
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getAllValues().get(0).getState());
		assertEquals(SearchIndexState.ACTIVE, captor.getAllValues().get(1).getState());
	}

	@Test
	public void testHandleCreateOnExceptionRecordsFailedWithErrorMessage() throws Exception {
		stubHappyPathThroughStream();
		doThrow(new RuntimeException("bad SQL")).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		List<SearchIndexStatus> saved = captor.getAllValues();
		assertEquals(SearchIndexState.CREATING, saved.get(0).getState());
		assertEquals(SearchIndexState.FAILED, saved.get(1).getState());
		assertEquals("bad SQL", saved.get(1).getErrorMessage());
		// Best-effort cleanup after failure
		verify(openSearchManager, times(2)).deleteIndex("search-index-" + ENTITY_ID + "-a");
	}

	@Test
	public void testHandleCreateTruncatesLongErrorMessage() throws Exception {
		// A malformed defining-SQL error message can be arbitrarily long (stack-trace-like
		// messages from the table query layer), but the status table column caps at 3000
		// chars. Verify the manager truncates before persisting so the write succeeds.
		String longMessage = "x".repeat(5000);
		stubHappyPathThroughStream();
		doThrow(new RuntimeException(longMessage)).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		SearchIndexStatus failed = captor.getAllValues().get(1);
		assertEquals(SearchIndexState.FAILED, failed.getState());
		assertEquals(3000, failed.getErrorMessage().length(),
				"Error message should be truncated to MAX_ERROR_MESSAGE_LENGTH");
	}

	@Test
	public void testHandleCreateExceedsMaxRowsRecordsFailed() throws Exception {
		// Row count above MAX_ROWS — IllegalStateException is caught by outer handler and
		// the index is marked FAILED with the row-count message.
		stubHappyPathThroughCreateIndex();
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(1_000_000L);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.FAILED, captor.getAllValues().get(1).getState());
		assertNotNull(captor.getAllValues().get(1).getErrorMessage());
		assertTrue(captor.getAllValues().get(1).getErrorMessage().contains("exceed maximum"));
		// Stream never runs past the row-count gate
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateOnConcurrentDeleteThrowsRecoverable() throws Exception {
		// AOSS rejects deleteIndex when another worker is mid-delete on the same index.
		// The lifecycle manager must translate that into RecoverableMessageException so
		// SQS retries the message — by then the winning delete is done and the retry
		// either no-ops the delete (index_not_found) or proceeds normally.
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
		ErrorCause cause = ErrorCause.of(b -> b
				.type("status_exception")
				.reason("Deletion failed for indices [search-index-syn456] due to concurrent deletes, please try again"));
		OpenSearchException concurrentDelete = new OpenSearchException(
				ErrorResponse.of(er -> er.error(cause).status(400)));
		doThrow(concurrentDelete).when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		assertSame(concurrentDelete, thrown.getCause());
		assertEquals("Concurrent delete in progress while building search index for entity "
				+ ENTITY_ID, thrown.getMessage());
		// The state row was set CREATING upfront, but no FAILED was recorded — this
		// is transient, not a configuration failure.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		// The pre-build deleteIndex was attempted (it threw); createIndex / row stream never ran.
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateOnTableUnavailableRecordsWaitingForSource() throws Exception {
		// Source table state is PROCESSING — instead of retrying until the SQS message DLQs,
		// buildIndex records WAITING_FOR_SOURCE and consumes the message. The rebuild fires later,
		// driven by the source's TABLE_STATUS_EVENT(AVAILABLE).
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.PROCESSING));

		// call under test — must consume the message (no exception).
		manager.handleCreate(progressCallback, ENTITY_ID);

		// CREATING was written on entry, then WAITING_FOR_SOURCE; no FAILED record, no index built.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getAllValues().get(0).getState());
		assertEquals(SearchIndexState.WAITING_FOR_SOURCE, captor.getAllValues().get(1).getState());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateOnSourceTableFailedPropagates() throws Exception {
		// Source table state is PROCESSING_FAILED — buildIndex throws TableFailedException.
		// It must propagate so the worker can retry.
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.PROCESSING_FAILED));

		// call under test
		assertThrows(TableFailedException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		// Only the CREATING status was written — no FAILED record, no cleanup.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		verify(openSearchManager, never()).deleteIndex(any());
	}

	@Test
	public void testHandleCreateOnSourceTableLockUnavailablePropagates() throws Exception {
		// LockUnavilableException thrown bare from indexDao.queryAsStream with no cause chain.
		// It must propagate out of buildIndex without recording FAILED so the worker can
		// translate it to RecoverableMessageException and SQS retries.
		LockUnavilableException lockEx = new LockUnavilableException(LockType.Write, "TABLE-LOCK-789", "BuildTableIndex,syn789");
		stubHappyPathThroughStream();
		doThrow(lockEx).when(indexDao).queryAsStream(any(), any());

		// call under test — LockUnavilableException from buildIndex propagates out of the
		// inner multi-catch and is then wrapped by handleCreate's outer catch into
		// RecoverableMessageException so the worker retries rather than recording FAILED.
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		assertSame(lockEx, thrown.getCause());
		// Only CREATING was written — no FAILED
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		// The pre-build deleteIndex ran (deleteExistingFirst=true), but no second cleanup delete
		// since LockUnavilableException propagates directly from the multi-catch without cleanup.
		verify(openSearchManager, times(1)).deleteIndex("search-index-" + ENTITY_ID + "-a");
	}

	@Test
	public void testHandleDeleteWithActiveStateDeletesIndexAndStatus() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(statusDao).delete(456L);
		verify(definingSqlDependencyDao).deleteObject(IdAndVersion.parse(ENTITY_ID));
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteWithFailedStateDeletesIndexAndStatus() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.FAILED));

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(statusDao).delete(456L);
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteWithMissingStatusSkipsLockAcquireAndIsNoOp() throws Exception {
		// Migration replay delivers ENTITY changes for entities that no longer exist;
		// SearchIndexLifecycleWorker funnels those through handleDelete via NotFoundException.
		// When there is no status row to clean up, the precheck must skip the write-lock
		// acquire entirely — it was the dominant cost in the SEARCH_INDEX_LIFECYCLE worker's
		// per-message profile.
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.empty());

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager, never()).deleteIndex(any());
		verify(statusDao, never()).delete(any());
		verify(writeReadSemaphore, never()).getWriteLock(any());
	}

	@Test
	public void testHandleDeleteWithLockAlreadyHeld() throws Exception {
		// Status is present so the precheck doesn't short-circuit; then the lock-acquire fails.
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));
		stubLockUnavailable();

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.handleDelete(progressCallback, ENTITY_ID));

		verify(openSearchManager, never()).deleteIndex(any());
		verify(statusDao, never()).delete(any());
	}

	// -------- per-entity lock tests --------

	@Test
	public void testHandleCreateWithLockAlreadyHeld() throws Exception {
		stubLockUnavailable();

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		verify(statusDao, never()).createOrUpdate(any());
		verify(openSearchManager, never()).deleteIndex(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
	}

	@Test
	public void testHandleCreateReleasesLockOnSuccess() throws Exception {
		// call under test
		stubHappyPathThroughStream();
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(writeLock).close();
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.ACTIVE, captor.getAllValues().get(1).getState());
	}

	@Test
	public void testHandleCreateReleasesLockOnFailure() throws Exception {
		stubHappyPathThroughStream();
		doThrow(new RuntimeException("unexpected failure")).when(indexDao).queryAsStream(any(), any());

		// call under test — exception is swallowed by the FAILED handler, lock must still be released
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(writeLock).close();
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.FAILED, captor.getAllValues().get(1).getState());
	}

	@Test
	public void testHandleCreateCallsWaitForIndexWritableBetweenCreateAndRunQuery() throws Exception {
		// AOSS acknowledges createIndex while shards are still not writable; the readiness probe
		// must run before queryAsStream so the bulk stream does not race against
		// index_not_found_exception.
		// call under test
		stubHappyPathThroughStream();
		manager.handleCreate(progressCallback, ENTITY_ID);

		org.mockito.InOrder order = org.mockito.Mockito.inOrder(openSearchManager, indexDao);
		order.verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		order.verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-a"),
				any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), isNull());
		order.verify(openSearchManager).waitForIndexWritable("search-index-" + ENTITY_ID + "-a");
		order.verify(indexDao).queryAsStream(any(), any());
		order.verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-a"), eq(Optional.empty()));
	}

	@Test
	public void testHandleCreateWhenProbeThrowsRecoverablePropagatesWithoutRecordingFailed() throws Exception {
		// waitForIndexWritable exhausts its retry budget and throws RecoverableMessageException.
		// That must propagate out of buildIndex unchanged and NOT flip the SearchIndex to FAILED —
		// the build will succeed on a later SQS retry.
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
		RecoverableMessageException probeFailed = new RecoverableMessageException(
				"AOSS index search-index-" + ENTITY_ID + " did not accept writes within the retry budget");
		doThrow(probeFailed).when(openSearchManager).waitForIndexWritable("search-index-" + ENTITY_ID + "-a");

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));
		assertSame(probeFailed, thrown);

		// Only CREATING was recorded — probe failure is transient, not a permanent failure.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		// Streaming must not have started — the probe runs first.
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	// -------- SearchIndexRowHandler tests --------

	@Test
	public void testRowHandlerNextRowBuildsDocumentFromColumnValues() {
		SelectColumn col1 = new SelectColumn();
		col1.setId("100");
		col1.setColumnType(ColumnType.STRING);
		SelectColumn col2 = new SelectColumn();
		col2.setId("200");
		col2.setColumnType(ColumnType.STRING);
		List<SelectColumn> columns = Arrays.asList(col1, col2);
		SearchIndexRowHandler handler =
				new SearchIndexRowHandler("test-index", columns, List.of(), Set.of(), openSearchManager);

		Row row = new Row();
		row.setRowId(42L);
		row.setVersionNumber(1L);
		row.setValues(Arrays.asList("hello", "world"));

		// call under test
		handler.nextRow(row);

		// No flush yet — batch size is 1000
		verify(openSearchManager, never()).bulkIndex(any(), any());
	}

	@Test
	public void testRowHandlerNextRowSkipsNullValues() throws IOException {
		SelectColumn col1 = new SelectColumn();
		col1.setId("100");
		col1.setColumnType(ColumnType.STRING);
		SelectColumn col2 = new SelectColumn();
		col2.setId("200");
		col2.setColumnType(ColumnType.STRING);
		List<SelectColumn> columns = Arrays.asList(col1, col2);
		SearchIndexRowHandler handler =
				new SearchIndexRowHandler("test-index", columns, List.of(), Set.of(), openSearchManager);

		Row row = new Row();
		row.setRowId(42L);
		row.setVersionNumber(1L);
		row.setValues(Arrays.asList("hello", null));
		handler.nextRow(row);

		// call under test — close forces a flush of the single-row batch
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		assertEquals(1, captor.getValue().size());
		// Indirect check via the BulkOperation's index op — the exact JSON content
		// of the null-excluded field is enforced by the OpenSearch autowire test.
	}

	@Test
	public void testRowHandlerCloseFlushesPartialBatch() throws IOException {
		SelectColumn col = new SelectColumn();
		col.setId("100");
		col.setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), List.of(), Set.of(), openSearchManager);

		// 3 rows — well under the 1000 batch size
		for (long i = 1; i <= 3; i++) {
			Row row = new Row();
			row.setRowId(i);
			row.setVersionNumber(1L);
			row.setValues(Collections.singletonList("v" + i));
			handler.nextRow(row);
		}

		// call under test
		handler.close();

		verify(openSearchManager).bulkIndex(eq("test-index"), any());
	}

	@Test
	public void testRowHandlerCloseWithEmptyBatchIsNoOpForBulk() throws IOException {
		SelectColumn col = new SelectColumn();
		col.setId("100");
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), List.of(), Set.of(), openSearchManager);

		// call under test
		handler.close();

		verify(openSearchManager, never()).bulkIndex(any(), any());
	}

	// Locks in the row handler's behavior when a SelectColumn has a null id: the
	// OpenSearch document is keyed by id, so the value lands under a literal `null`
	// key — unreachable via field-name lookups. Upstream registration is responsible
	// for ensuring every column has a real id before this code runs.
	@Test
	public void testRowHandlerNextRowWithNullColumnIdWritesNullKey() throws IOException {
		SelectColumn nullIdCol = new SelectColumn();
		nullIdCol.setId(null);
		nullIdCol.setName("derived_alias");
		nullIdCol.setColumnType(ColumnType.STRING);
		SelectColumn realIdCol = new SelectColumn();
		realIdCol.setId("100");
		realIdCol.setName("real");
		realIdCol.setColumnType(ColumnType.STRING);
		List<SelectColumn> columns = Arrays.asList(nullIdCol, realIdCol);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", columns, List.of(), Set.of(), openSearchManager);

		Row row = new Row();
		row.setRowId(42L);
		row.setVersionNumber(1L);
		row.setValues(Arrays.asList("derived-value", "real-value"));
		handler.nextRow(row);

		// call under test — close to flush
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		assertEquals(1, captor.getValue().size());

		BulkOperation op = captor.getValue().get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> doc = (Map<String, Object>) op.index().document();
		assertEquals("real-value", doc.get("100"));
		assertTrue(doc.containsKey(null));
		assertEquals("derived-value", doc.get(null));
	}

	@Test
	public void testRowHandlerFlushesEveryBatchSize() throws Exception {
		// 1500 rows → BATCH_SIZE is 1000 → first flush happens at row 1000, second on close().
		SelectColumn col = new SelectColumn().setId("col-1").setName("title").setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"search-index-syn1", Collections.singletonList(col), List.of(), Set.of(), openSearchManager);

		for (int i = 0; i < 1500; i++) {
			Row row = new Row().setRowId((long) i).setVersionNumber(1L)
					.setValues(Collections.singletonList("title-" + i));
			handler.nextRow(row);
		}
		// One flush already (1000); a second flush triggers via close() (remaining 500).
		verify(openSearchManager, times(1)).bulkIndex(eq("search-index-syn1"), any());
		// call under test — closing flushes the trailing partial batch.
		handler.close();
		verify(openSearchManager, times(2)).bulkIndex(eq("search-index-syn1"), any());
	}

	@Test
	public void testRowHandlerNextRowWithViewSourceReadsTrailingBenefactor() throws IOException {
		SelectColumn col = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		// A view's single ROW_BENEFACTOR is spliced in as one trailing value. The by-name
		// Row.benefactorId is not consulted, so a differing value there must not reach the document.
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), List.of("ROW_BENEFACTOR"), Set.of(), openSearchManager);

		Row row = new Row().setRowId(42L).setVersionNumber(1L).setBenefactorId(55L)
				.setValues(Arrays.asList("hello", "99"));
		// call under test
		handler.nextRow(row);
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		BulkOperation op = captor.getValue().get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> doc = (Map<String, Object>) op.index().document();
		assertEquals("hello", doc.get("100"));
		assertEquals(99L, doc.get("_benefactor_ROW_BENEFACTOR"));
		// View document id is the stable ROW_ID.
		assertEquals("42", op.index().id());
	}

	@Test
	public void testRowHandlerNextRowWithMaterializedViewSourceReadsTrailingBenefactors() throws IOException {
		SelectColumn col = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		// A materialized view with two dependencies appends two benefactor columns to the
		// trailing positional values, in the order of the snapshot's benefactor column names.
		// The document is keyed by ROW_ID.
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), List.of("ROW_BENEFACTOR__A0", "ROW_BENEFACTOR__A1"), Set.of(), openSearchManager);

		// values = [ title, benefactor_0, benefactor_1 ]
		Row row = new Row().setRowId(7L).setVersionNumber(1L)
				.setValues(Arrays.asList("hello", "11", "22"));
		// call under test
		handler.nextRow(row);
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		BulkOperation op = captor.getValue().get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> doc = (Map<String, Object>) op.index().document();
		assertEquals("hello", doc.get("100"));
		assertEquals(11L, doc.get("_benefactor_ROW_BENEFACTOR__A0"));
		assertEquals(22L, doc.get("_benefactor_ROW_BENEFACTOR__A1"));
		assertEquals("7", op.index().id());
	}

	@Test
	public void testRowHandlerNextRowWithTooManyValuesThrows() throws IOException {
		// PLFM-9714: a row wider than the declared document + benefactor columns means the
		// document schema and the streamed values disagree. Reading the surplus value as a
		// trailing benefactor would index every row under an ACL benefactor it does not belong
		// to whenever that value happens to parse as a long, so the handler fails closed.
		SelectColumn col = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), List.of(), Set.of(), openSearchManager);

		Row row = new Row().setRowId(7L).setVersionNumber(1L)
				.setValues(Arrays.asList("hello", "1500"));

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> handler.nextRow(row));

		assertEquals("Expected 1 values per row (1 document columns and 0 benefactor columns)"
				+ " but the source query returned 2.", e.getMessage());
		handler.close();
		verify(openSearchManager, never()).bulkIndex(any(), any());
	}

	@Test
	public void testRowHandlerNextRowWithTooFewValuesThrows() throws IOException {
		SelectColumn title = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		SelectColumn tags = new SelectColumn().setId("101").setName("tags").setColumnType(ColumnType.STRING_LIST);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Arrays.asList(title, tags), List.of("ROW_BENEFACTOR"), Set.of(), openSearchManager);

		Row row = new Row().setRowId(7L).setVersionNumber(1L)
				.setValues(Arrays.asList("hello", "[\"a\"]"));

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> handler.nextRow(row));

		assertEquals("Expected 3 values per row (2 document columns and 1 benefactor columns)"
				+ " but the source query returned 2.", e.getMessage());
		handler.close();
		verify(openSearchManager, never()).bulkIndex(any(), any());
	}

	@Test
	public void testRowHandlerNextRowWithSemanticColumnsWritesJoinedText() throws IOException {
		SelectColumn title = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		SelectColumn tags = new SelectColumn().setId("101").setName("tags").setColumnType(ColumnType.STRING_LIST);
		SelectColumn count = new SelectColumn().setId("102").setName("count").setColumnType(ColumnType.INTEGER);
		SelectColumn notes = new SelectColumn().setId("103").setName("notes").setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler("test-index", Arrays.asList(title, tags, count, notes),
				List.of(), Set.of("100", "101", "103"), openSearchManager);

		// call under test
		handler.nextRow(new Row().setRowId(7L).setVersionNumber(1L)
				.setValues(Arrays.asList("hello", "[\"a\",\" \",\"b\"]", "5", " ")));
		// call under test
		handler.nextRow(new Row().setRowId(8L).setVersionNumber(1L)
				.setValues(Arrays.asList(null, null, "6", null)));
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		Map<String, Object> withText = new HashMap<>();
		withText.put("_row_id", 7L);
		withText.put("_row_version", 1L);
		withText.put("100", "hello");
		withText.put("101", List.of("a", " ", "b"));
		withText.put("102", 5);
		withText.put("103", " ");
		withText.put(OpenSearchManagerImpl.SEMANTIC_TEXT_FIELD, "title: hello\ntags: a, b");
		assertEquals(withText, captor.getValue().get(0).index().document());
		assertEquals(Map.of("_row_id", 8L, "_row_version", 1L, "102", 6), captor.getValue().get(1).index().document());
	}

	@Test
	public void testTruncateSemanticTextWithTextOverTokenLimit() {
		Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
		String text = "word ".repeat(10_000);

		// call under test
		String truncated = SearchIndexLifecycleManagerImpl.truncateSemanticText(text);

		assertTrue(text.startsWith(truncated));
		assertEquals(SearchIndexLifecycleManagerImpl.MAX_SEMANTIC_TEXT_TOKENS, encoding.countTokens(truncated));
	}

	@Test
	public void testTruncateSemanticTextWithTextUnderTokenLimit() {
		String text = "title: hello";

		// call under test
		assertSame(text, SearchIndexLifecycleManagerImpl.truncateSemanticText(text));
	}

	// -------- buildIndex — semantic columns --------

	private static final SemanticEmbeddingModel SEMANTIC_MODEL =
			new SemanticEmbeddingModel("model-1", "amazon.titan-embed-text-v2:0", 1024);

	/** A SearchConfiguration flagging the source's "name" column semantic. */
	private void stubSemanticNameColumn() throws Exception {
		SearchConfiguration config = new SearchConfiguration().setColumnAnalyzerOverrides(List.of(
				Map.of("overrides", List.of(Map.of("columnName", "name", "semantic", true)))));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.of(config));
	}

	private SearchIndexStatus captureLastStatus() {
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		return captor.getValue();
	}

	@Test
	public void testHandleCreateWithSemanticColumnCreatesIndexWithModel() throws Exception {
		stubHappyPathThroughStream();
		stubSemanticNameColumn();
		when(semanticEmbeddingBootstrapper.getModel()).thenReturn(Optional.of(SEMANTIC_MODEL));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-a"), eq(List.of(NAME_COLUMN)),
				any(), any(), any(), eq(List.of()), anyInt(), anyInt(), eq(SOURCE_SNAPSHOT), eq(SEMANTIC_MODEL));
		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.ACTIVE),
				captureLastStatus());
		verify(semanticEmbeddingBootstrapper, never()).bootstrapSemanticEmbedding();
	}

	@Test
	public void testHandleCreateWithSemanticColumnOfUnsupportedTypeMarksFailed() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		stubSemanticNameColumn();
		ColumnModel integerName = new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.INTEGER);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(integerName);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName()))))
				.thenReturn(integerName);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("Column 'name' is of type INTEGER and cannot be flagged 'semantic'; only "
						+ "[STRING, LINK, MEDIUMTEXT, LARGETEXT, STRING_LIST] columns can."),
				captureLastStatus());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verifyNoInteractions(semanticEmbeddingBootstrapper);
	}

	@Test
	public void testHandleCreateWithSemanticColumnAboveRowCeilingMarksFailed() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		stubSemanticNameColumn();
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(SearchIndexLifecycleManagerImpl.SEMANTIC_MAX_ROWS + 1);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName()))))
				.thenReturn(NAME_COLUMN);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("Search index with semantic columns would exceed maximum of 50000 rows. Row count: 50001"),
				captureLastStatus());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verifyNoInteractions(semanticEmbeddingBootstrapper);
	}

	@Test
	public void testHandleCreateWithSemanticColumnAndSourceGrownPastRowCeilingUnderLockMarksFailed() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		stubSemanticNameColumn();
		// The pre-lock read sees the source before a rebuild that lands before the lock is taken.
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(0L, SearchIndexLifecycleManagerImpl.SEMANTIC_MAX_ROWS + 1);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName()))))
				.thenReturn(NAME_COLUMN);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("Search index with semantic columns would exceed maximum of 50000 rows. Row count: 50001"),
				captureLastStatus());
		verify(indexDao, times(2)).getRowCountForTable(SOURCE_ID);
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verifyNoInteractions(semanticEmbeddingBootstrapper);
	}

	@Test
	public void testHandleCreateWithSemanticColumnAndNoModelThrowsRecoverable() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		stubSemanticNameColumn();
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName()))))
				.thenReturn(NAME_COLUMN);
		when(semanticEmbeddingBootstrapper.getModel()).thenReturn(Optional.empty());

		// call under test
		RecoverableMessageException e = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		assertEquals("No semantic embedding model is deployed yet for search index null", e.getMessage());
		assertEquals(SearchIndexState.CREATING, captureLastStatus().getState());
		verify(openSearchManager, never()).deleteIndex(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(semanticEmbeddingBootstrapper, never()).bootstrapSemanticEmbedding();
	}

	@Test
	public void testHandleCreateWithSemanticModelReplacedMidBuildThrowsRecoverable() throws Exception {
		stubHappyPathThroughStream();
		stubSemanticNameColumn();
		SemanticEmbeddingModel reRegistered = new SemanticEmbeddingModel("model-2", "amazon.titan-embed-text-v2:0", 1024);
		when(semanticEmbeddingBootstrapper.getModel())
				.thenReturn(Optional.of(SEMANTIC_MODEL))
				.thenReturn(Optional.of(reRegistered));
		RuntimeException writeRejected = new RuntimeException("model-1 not found");
		doThrow(writeRejected).when(indexDao).queryAsStream(any(), any());

		// call under test
		RecoverableMessageException e = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));

		assertSame(writeRejected, e.getCause());
		assertEquals(SearchIndexState.CREATING, captureLastStatus().getState());
		verify(openSearchManager, never()).swapAlias(any(), any(), any());
		verify(semanticEmbeddingBootstrapper, never()).bootstrapSemanticEmbedding();
	}

	@Test
	public void testHandleCreateWithSemanticModelUnchangedMidBuildFailureMarksFailed() throws Exception {
		stubHappyPathThroughStream();
		stubSemanticNameColumn();
		when(semanticEmbeddingBootstrapper.getModel()).thenReturn(Optional.of(SEMANTIC_MODEL));
		doThrow(new RuntimeException("mapper_parsing_exception")).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("mapper_parsing_exception"), captureLastStatus());
		verify(openSearchManager, never()).swapAlias(any(), any(), any());
		verify(semanticEmbeddingBootstrapper, never()).bootstrapSemanticEmbedding();
	}

	// -------- resolveAnalyzers --------

	@Test
	public void testResolveAnalyzersWithoutRefsDoesNotTouchSynonymSetDao() {
		String settings = "{\"analyzer\":{\"default\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}}}";
		TextAnalyzer ta = new TextAnalyzer().setId("1").setOrganizationName("org").setName("noop")
				.setSettings(settings);

		// call under test
		Map<String, org.opensearch.client.opensearch.indices.IndexSettingsAnalysis> resolved =
				manager.resolveAnalyzers(Collections.singletonMap("org-noop", ta));

		assertEquals(1, resolved.size());
		verifyNoMoreInteractions(synonymSetDao);
	}

	@Test
	public void testResolveAnalyzersResolvesRefAgainstSynonymSetDao() {
		String settings = "{\"filter\":{\"med\":{\"$ref\":\"biomed-medical_terms\"}}}";
		TextAnalyzer ta = new TextAnalyzer().setId("1").setOrganizationName("biomed").setName("publications")
				.setSettings(settings);
		SynonymSet ss = new SynonymSet().setId("100").setOrganizationName("biomed").setName("medical_terms")
				.setDefinition("{\"type\":\"synonym_graph\",\"synonyms\":[\"a, b\"]}");
		when(synonymSetDao.getByQualifiedNames(Collections.singletonList("biomed-medical_terms")))
				.thenReturn(Collections.singletonMap("biomed-medical_terms", ss));

		// call under test
		Map<String, org.opensearch.client.opensearch.indices.IndexSettingsAnalysis> resolved =
				manager.resolveAnalyzers(Collections.singletonMap("biomed-publications", ta));

		// The substituted SynonymSet definition lands as the typed synonym_graph variant.
		assertTrue(resolved.get("biomed-publications").filter().get("med").definition().isSynonymGraph());
	}

	@Test
	public void testResolveAnalyzersThrowsOnMissingRef() {
		String settings = "{\"filter\":{\"ghost\":{\"$ref\":\"biomed-ghost\"}}}";
		TextAnalyzer ta = new TextAnalyzer().setId("1").setOrganizationName("biomed").setName("publications")
				.setSettings(settings);
		when(synonymSetDao.getByQualifiedNames(Collections.singletonList("biomed-ghost")))
				.thenReturn(Collections.emptyMap());

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> manager.resolveAnalyzers(Collections.singletonMap("biomed-publications", ta)));

		assertTrue(e.getMessage().contains("Unresolved $ref"));
		assertTrue(e.getMessage().contains("biomed-ghost"));
	}

	// -------- convertForDocument (parameterized over every ColumnType branch) --------

	@Test
	public void testConvertForDocumentWithNullReturnsNull() {
		// call under test
		assertNull(SearchIndexLifecycleManagerImpl.convertForDocument("aCol", null, ColumnType.STRING));
	}

	@ParameterizedTest
	@EnumSource(value = ColumnType.class, names = {"STRING", "LARGETEXT", "MEDIUMTEXT", "LINK"})
	public void testConvertForDocumentBareStringTypesPassThrough(ColumnType type) {
		// call under test — bare-string types short-circuit to raw String pass-through so
		// AOSS doesn't receive a JSON-parsed value (which would be malformed for text fields).
		assertEquals("alpha", SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "alpha", type));
	}

	@ParameterizedTest
	@EnumSource(value = ColumnType.class, names = {"ENTITYID", "USERID"})
	public void testConvertForDocumentKeywordIdTypesPassThrough(ColumnType type) {
		// call under test — KEYWORD-category ID types are stored as raw strings in AOSS;
		// LONG-category IDs (FILEHANDLEID, EVALUATIONID) go through the JSON parse branch.
		assertEquals("syn123", SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "syn123", type));
	}

	@Test
	public void testConvertForDocumentWithIntegerParsesAsLong() {
		// call under test — INTEGER serializes to JSON number; Jackson surfaces it as Integer/Long.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "42", ColumnType.INTEGER);

		assertEquals(42, ((Number) result).intValue());
	}

	@Test
	public void testConvertForDocumentWithDoubleParsesAsDouble() {
		// call under test
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "3.14", ColumnType.DOUBLE);

		assertEquals(3.14d, ((Number) result).doubleValue(), 1e-9);
	}

	@ParameterizedTest
	@ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
	public void testConvertForDocumentWithNonFiniteDoubleReturnsNull(String value) {
		// call under test — OpenSearch double fields reject non-finite values, so the field is left out.
		assertNull(SearchIndexLifecycleManagerImpl.convertForDocument("aCol", value, ColumnType.DOUBLE));
	}

	@Test
	public void testConvertForDocumentWithBooleanParses() {
		// call under test
		assertEquals(Boolean.TRUE, SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "true", ColumnType.BOOLEAN));
	}

	@Test
	public void testConvertForDocumentWithStringListParsesAsJsonArray() {
		// call under test — STRING_LIST stored as a JSON array string; AOSS expects a real list.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"aCol", "[\"a\",\"b\"]", ColumnType.STRING_LIST);

		assertTrue(result instanceof List, "Expected a List, got " + result.getClass());
		assertEquals(Arrays.asList("a", "b"), result);
	}

	@Test
	public void testConvertForDocumentWithEntityIdListParsesAsJsonArray() {
		// call under test — ENTITYID_LIST also goes through JSON parse despite the underlying
		// type mapping being KEYWORD (the list branch wins over the keyword short-circuit).
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"aCol", "[\"syn1\",\"syn2\"]", ColumnType.ENTITYID_LIST);

		assertEquals(Arrays.asList("syn1", "syn2"), result);
	}

	@Test
	public void testConvertForDocumentWithJsonTypeParsesAsMap() {
		// call under test — JSON column round-trips as a Map; AOSS stores it as a dynamic object.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"aCol", "{\"foo\":\"bar\"}", ColumnType.JSON);

		assertTrue(result instanceof Map);
		assertEquals("bar", ((Map<?, ?>) result).get("foo"));
	}

	@Test
	public void testConvertForDocumentWithMalformedJsonThrows() {
		// call under test — a malformed JSON list value must throw IllegalArgumentException
		// so the build is recorded as FAILED with a clear message (not a silent doc-level error).
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchIndexLifecycleManagerImpl.convertForDocument("aCol", "[not-json", ColumnType.STRING_LIST));

		// The message is surfaced verbatim to users through SearchIndexStatus.errorMessage, so it
		// must name the column — the type alone leaves an operator unable to tell which column
		// produced a value of the wrong shape.
		assertEquals("Failed to convert value of column 'aCol' for type STRING_LIST: [not-json",
				e.getMessage());
	}

	// -------- collectAndLoadAnalyzers (package-private) --------

	@Test
	public void testCollectAndLoadAnalyzersWithNoOverridesOrConfigUsesColumnDefaults() {
		ColumnModel stringCol = new ColumnModel().setId("col-1").setName("title").setColumnType(ColumnType.STRING);
		ColumnModel intCol = new ColumnModel().setId("col-2").setName("count").setColumnType(ColumnType.INTEGER);

		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		Map<String, TextAnalyzer> result = manager.collectAndLoadAnalyzers(
				null, null, Arrays.asList(stringCol, intCol));

		assertNotNull(result);
		// Capture what was passed to the DAO and assert it included the STRING column's
		// platform-default analyzer qname (SCIENTIFIC) as a hard requirement, regardless of
		// the input config.
		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(textAnalyzerDao).getByQualifiedNames(captor.capture());
		assertTrue(captor.getValue().contains(
				ColumnTypeToOpenSearchMapping.getDefaultAnalyzerQualifiedName(ColumnType.STRING)));
	}

	@Test
	public void testCollectAndLoadAnalyzersIncludesConfigDefault() {
		ColumnModel stringCol = new ColumnModel().setId("col-1").setName("title").setColumnType(ColumnType.STRING);
		// defaultAnalyzer is a $ref to a TextAnalyzer; the lifecycle pipeline extracts the
		// qname via SearchOpaqueJsonUtil.readRef.
		SearchConfiguration config = new SearchConfiguration()
				.setDefaultAnalyzer(new org.json.JSONObject().put("$ref", "org-biomed-DEFAULT_ANALYZER"));
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(config, null, Collections.singletonList(stringCol));

		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(textAnalyzerDao).getByQualifiedNames(captor.capture());
		assertTrue(captor.getValue().contains("org-biomed-DEFAULT_ANALYZER"));
	}

	@Test
	public void testCollectAndLoadAnalyzersIncludesOverrideAnalyzers() {
		ColumnModel stringCol = new ColumnModel().setId("col-1").setName("title").setColumnType(ColumnType.STRING);
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setOverrides(Collections.singletonList(new ColumnAnalyzerOverrideEntry()
						.setColumnName("title")
						.setAnalyzer(new org.json.JSONObject().put("$ref", "biomed-CUSTOM"))));
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(null, Collections.singletonList(override),
				Collections.singletonList(stringCol));

		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(textAnalyzerDao).getByQualifiedNames(captor.capture());
		assertTrue(captor.getValue().contains("biomed-CUSTOM"));
	}

	/** Stub the minimum chain that lets buildIndex reach the row-stream phase. */
	private void stubHappyPathThroughCreateIndexWithConfig() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
	}

	// -------- buildIndex — additional branch coverage --------

	@Test
	public void testHandleCreateWithSelectStarExpandedFromSourceSnapshot() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		ColumnModel ageColumn = new ColumnModel().setId("101").setName("age").setColumnType(ColumnType.INTEGER);
		IndexAuthorizationSnapshot sourceSnapshot = tableSnapshot("100", "101");
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(sourceSnapshot));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		when(tableManagerSupport.getColumnModel("101")).thenReturn(ageColumn);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName())))).thenReturn(NAME_COLUMN);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "age".equals(cm.getName())))).thenReturn(ageColumn);
		when(tableManagerSupport.getAggregateDataConfiguration("syn789")).thenReturn(Optional.empty());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-a"), eq(List.of(NAME_COLUMN, ageColumn)),
				any(), any(), any(), eq(List.of()), anyInt(), anyInt(), eq(sourceSnapshot), isNull());
		verify(tableManagerSupport, never()).getTableSchema(any());
	}

	@Test
	public void testHandleCreateWithSnapshotColumnTypeDifferentFromLiveMapsSnapshotType() throws Exception {
		// The source's live column "name" is a STRING, but the source was built with "name" as an
		// INTEGER column (id 102); the index must be mapped for the rows actually streamed.
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		ColumnModel builtNameColumn = new ColumnModel().setId("102").setName("name").setColumnType(ColumnType.INTEGER);
		IndexAuthorizationSnapshot sourceSnapshot = tableSnapshot("102");
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(sourceSnapshot));
		when(tableManagerSupport.getColumnModel("102")).thenReturn(builtNameColumn);
		when(columnModelManager.createColumnModel(argThat(cm -> ColumnType.INTEGER.equals(cm.getColumnType()))))
				.thenReturn(builtNameColumn);
		when(tableManagerSupport.getAggregateDataConfiguration("syn789")).thenReturn(Optional.empty());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-a"), eq(List.of(builtNameColumn)),
				any(), any(), any(), eq(List.of()), anyInt(), anyInt(), eq(sourceSnapshot), isNull());
		verify(tableManagerSupport, never()).getTableSchema(any());
	}

	@Test
	public void testHandleCreateWithSnapshotWrittenToIdleSlotBeforeSwapOutsideSourceLock() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceSnapshotAndTranslation();
		// The swap must not have happened by the time the locked callable returns.
		doAnswer(invocation -> {
			Object result = invocation.<ProgressingCallable<?>>getArgument(2).call(progressCallback);
			verify(openSearchManager, never()).swapAlias(any(), any(), any());
			return result;
		}).when(tableManagerSupport).tryRunWithTableNonExclusiveLock(eq(progressCallback), eq(BUILD_LOCK_CONTEXT),
				any(ProgressingCallable.class), eq(SOURCE_ID));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		InOrder order = inOrder(openSearchManager, indexDao);
		order.verify(openSearchManager).createIndex("search-index-" + ENTITY_ID + "-a", List.of(NAME_COLUMN), null,
				Collections.emptyList(), Collections.emptyMap(), List.of(), 1, 0, SOURCE_SNAPSHOT, null);
		order.verify(indexDao).queryAsStream(any(), any());
		order.verify(openSearchManager).swapAlias("search-index-" + ENTITY_ID, "search-index-" + ENTITY_ID + "-a",
				Optional.empty());
		verify(columnModelManager, never()).bindColumnsToVersionOfObject(anyList(), any());
	}

	@Test
	public void testHandleCreateWithLockOnSourceOnly() throws Exception {
		stubHappyPathThroughStream();

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(tableManagerSupport).tryRunWithTableNonExclusiveLock(eq(progressCallback), eq(BUILD_LOCK_CONTEXT),
				any(ProgressingCallable.class), eq(SOURCE_ID));
		verify(tableManagerSupport, never()).tryRunWithTableNonExclusiveLock(any(), any(), any(), any(String[].class));
	}

	@Test
	public void testHandleCreateWithSourceWithoutSnapshotRecordsWaitingForSource() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.empty());

		// call under test — must consume the message (no exception).
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getAllValues().get(0).getState());
		assertEquals(SearchIndexState.WAITING_FOR_SOURCE, captor.getAllValues().get(1).getState());
		verify(openSearchManager, never()).deleteIndex(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
		verify(openSearchManager, never()).swapAlias(any(), any(), any());
	}

	@Test
	public void testHandleUpdateWithLiveIndexAndSourceWithoutSnapshotKeepsLiveIndex() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		String liveSlot = "search-index-" + ENTITY_ID + "-a";
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.of(liveSlot));
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.empty());

		// call under test
		manager.handleUpdate(progressCallback, ENTITY_ID);

		// No CREATING on a rebuild; the live slot keeps serving under WAITING_FOR_SOURCE.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.WAITING_FOR_SOURCE, captor.getValue().getState());
		verify(openSearchManager, never()).deleteIndex(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(openSearchManager, never()).swapAlias(any(), any(), any());
	}

	@Test
	public void testHandleCreateWithAggregateDataDependencyRecordsFailed() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		IndexAuthorizationSnapshot sourceSnapshot = tableSnapshot("100");
		sourceSnapshot.getIndexDescription().setDependencies(List.of(
				new SourceDependency().setObjectId("syn800").setTableType(TableType.entityview.name())));
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(sourceSnapshot));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);
		when(columnModelManager.createColumnModel(argThat(cm -> cm != null && "name".equals(cm.getName())))).thenReturn(NAME_COLUMN);
		when(tableManagerSupport.getAggregateDataConfiguration("syn789")).thenReturn(Optional.empty());
		when(tableManagerSupport.getAggregateDataConfiguration("syn800"))
				.thenReturn(Optional.of(new AggregateDataConfiguration().setSuppressionThreshold(5L)));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("Search index source syn789 depends on AGGREGATE_DATA object syn800"),
				captor.getAllValues().get(1));
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateWithMaterializedViewSourceSplicesSnapshotBenefactors() throws Exception {
		stubHappyPathThroughCreateIndex();
		stubSourceLock();
		// The SQL is generated from, and the benefactor columns spliced into it come from, the
		// source's as-built snapshot.
		ColumnModel studyColumn = TableModelTestUtils.createColumn(703L, "studyId", ColumnType.INTEGER);
		IndexDescriptionSnapshot mvDescription = new IndexDescriptionSnapshot()
				.setObjectId("syn789")
				.setTableType(TableType.materializedview.name())
				.setBenefactors(List.of(
						new BenefactorColumn().setBenefactorColumnName("SNAPSHOT_BENEFACTOR_0").setBenefactorType("ENTITY"),
						new BenefactorColumn().setBenefactorColumnName("SNAPSHOT_BENEFACTOR_1").setBenefactorType("ENTITY")))
				.setDependencies(List.of(new SourceDependency().setObjectId("syn10").setTableType(TableType.table.name())))
				.setDefiningSql("SELECT * FROM syn10");
		IndexAuthorizationSnapshot sourceSnapshot = tableSnapshot("703").setIndexDescription(mvDescription);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(sourceSnapshot));
		when(tableManagerSupport.getColumnModel("703")).thenReturn(studyColumn);
		when(columnModelManager.createColumnModel(argThat(cm -> "studyId".equals(cm.getName())))).thenReturn(studyColumn);
		when(tableManagerSupport.getAggregateDataConfiguration("syn789")).thenReturn(Optional.empty());
		doAnswer(invocation -> {
			RowHandler handler = invocation.getArgument(1);
			handler.nextRow(new Row().setRowId(7L).setVersionNumber(1L).setValues(List.of("42", "11", "22")));
			return null;
		}).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-a"), eq(List.of(studyColumn)),
				any(), any(), any(), eq(List.of("SNAPSHOT_BENEFACTOR_0", "SNAPSHOT_BENEFACTOR_1")), anyInt(), anyInt(), eq(sourceSnapshot), isNull());
		ArgumentCaptor<TranslatedQuery> queryCaptor = ArgumentCaptor.forClass(TranslatedQuery.class);
		verify(indexDao).queryAsStream(queryCaptor.capture(), any());
		assertEquals("SELECT _C703_, SNAPSHOT_BENEFACTOR_0, SNAPSHOT_BENEFACTOR_1, ROW_ID, ROW_VERSION FROM T789",
				queryCaptor.getValue().getOutputSQL());
		ArgumentCaptor<List<BulkOperation>> bulkCaptor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("search-index-" + ENTITY_ID + "-a"), bulkCaptor.capture());
		@SuppressWarnings("unchecked")
		Map<String, Object> doc = (Map<String, Object>) bulkCaptor.getValue().get(0).index().document();
		assertEquals(Map.of("_row_id", 7L, "_row_version", 1L, "703", 42, "_benefactor_SNAPSHOT_BENEFACTOR_0", 11L, "_benefactor_SNAPSHOT_BENEFACTOR_1", 22L), doc);
	}

	@Test
	public void testHandleCreateRejectsRowCountAboveMax() throws Exception {
		// rowCount > MAX_ROWS — IllegalStateException is caught by outer handler and
		// the index is marked FAILED with the row-count message.
		stubHappyPathThroughCreateIndex();
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(1_000_000L);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED
						&& s.getErrorMessage() != null
						&& s.getErrorMessage().contains("exceed maximum")));
	}

	@Test
	public void testHandleCreateAcceptsNullRowCount() throws Exception {
		// rowCount == null — short-circuits the > MAX_ROWS guard and proceeds.
		stubHappyPathThroughCreateIndex();
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(null);
		stubSchemaProviderForTranslator();

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), isNull());
	}

	@Test
	public void testHandleCreateWithConfigSetsDefaultAnalyzer() throws Exception {
		// config != null branch — readRef extracts the qname and forwards it to createIndex.
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
		String defaultQname = "org.sagebionetworks-SCIENTIFIC";
		SearchConfiguration config = new SearchConfiguration()
				.setDefaultAnalyzer(new org.json.JSONObject().put("$ref", defaultQname));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.of(config));
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(
				Collections.singletonMap(defaultQname,
						new TextAnalyzer().setName("SCIENTIFIC").setSettings(
								new org.json.JSONObject().put("analyzer",
										new org.json.JSONObject().put("default",
												new org.json.JSONObject().put("type", "custom")
														.put("tokenizer", "standard"))))));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(any(), any(), eq(defaultQname), any(), any(), any(), anyInt(), anyInt(), any(), isNull());
	}

	@Test
	public void testHandleCreateWithIOExceptionWithoutRecoverableCauseMarksFailed() throws Exception {
		// An IOException from the stream is a genuine build failure — falls through to
		// the FAILED-marking path. The IOException itself is swallowed.
		stubHappyPathThroughStream();
		doThrow(new RuntimeException(new IOException("disk full"))).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
	}

	@Test
	public void testHandleCreateWithOpenSearchExceptionNotConcurrentDeleteMarksFailed() throws Exception {
		// OpenSearchException that ISN'T a concurrent-delete falls through to the
		// FAILED-marking path.
		stubHappyPathThroughStream();
		org.opensearch.client.opensearch._types.OpenSearchException opensearchEx =
				new org.opensearch.client.opensearch._types.OpenSearchException(
						new org.opensearch.client.opensearch._types.ErrorResponse.Builder()
								.status(500)
								.error(new org.opensearch.client.opensearch._types.ErrorCause.Builder()
										.type("internal_server_error")
										.reason("not a concurrent delete")
										.build())
								.build());
		doThrow(opensearchEx).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
	}

	@Test
	public void testHandleCreateWithNullErrorMessageStillMarksFailed() throws Exception {
		// e.getMessage() == null — truncate guard short-circuits cleanly and the
		// FAILED status carries a null errorMessage.
		stubHappyPathThroughStream();
		doThrow(new RuntimeException((String) null)).when(indexDao).queryAsStream(any(), any());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		SearchIndexStatus failed = captor.getAllValues().stream()
				.filter(s -> s.getState() == SearchIndexState.FAILED)
				.findFirst().orElseThrow();
		assertNull(failed.getErrorMessage());
	}

	// -------- collectAndLoadAnalyzers — null/empty branches --------

	@Test
	public void testCollectAndLoadAnalyzersWithNullOverridesAndConfig() {
		// Both overrides and config null — only the source columns' system defaults plus the
		// always-loaded STRING default should be requested.
		ColumnModel intCol = new ColumnModel().setId("c").setName("c").setColumnType(ColumnType.INTEGER);
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(null, null, Collections.singletonList(intCol));

		verify(textAnalyzerDao).getByQualifiedNames(anyList());
	}

	@Test
	public void testCollectAndLoadAnalyzersIgnoresOverrideWithNullEntries() {
		// A ColumnAnalyzerOverride whose getOverrides() is null must not NPE — the loop guards
		// it. The qname-collection should still pull in the column-type defaults.
		ColumnModel stringCol = new ColumnModel().setId("c").setName("c").setColumnType(ColumnType.STRING);
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride();
		// override.getOverrides() == null
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(null, Collections.singletonList(override),
				Collections.singletonList(stringCol));

		verify(textAnalyzerDao).getByQualifiedNames(anyList());
	}

	@Test
	public void testCollectAndLoadAnalyzersIgnoresEntryWithNoRefAnalyzer() {
		// An override entry whose analyzer slot isn't a {"$ref": ...} (e.g. inline literal,
		// or simply absent) should NOT be added to the qualified-name set.
		ColumnModel stringCol = new ColumnModel().setId("c").setName("c").setColumnType(ColumnType.STRING);
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setOverrides(Collections.singletonList(new ColumnAnalyzerOverrideEntry()
						.setColumnName("c")
						.setAnalyzer(null))); // null analyzer — readRef returns null
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(null, Collections.singletonList(override),
				Collections.singletonList(stringCol));

		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(textAnalyzerDao).getByQualifiedNames(captor.capture());
		// Only the column-type defaults; no "no-ref" qname leaked through.
		assertTrue(captor.getValue().stream().noneMatch(q -> q == null));
	}

	@Test
	public void testCollectAndLoadAnalyzersWithConfigButNoDefaultAnalyzer() {
		// Config present but defaultAnalyzer is null/inline — the lifecycle skips the
		// branch that would add a defaultAnalyzer qname.
		ColumnModel stringCol = new ColumnModel().setId("c").setName("c").setColumnType(ColumnType.STRING);
		SearchConfiguration config = new SearchConfiguration(); // defaultAnalyzer is null
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(Collections.emptyMap());

		// call under test
		manager.collectAndLoadAnalyzers(config, null, Collections.singletonList(stringCol));

		verify(textAnalyzerDao).getByQualifiedNames(anyList());
	}

	// -------- loadColumnAnalyzerOverrides --------

	@Test
	public void testLoadColumnAnalyzerOverridesWithNullConfigReturnsEmpty() {
		assertTrue(manager.loadColumnAnalyzerOverrides(null).isEmpty());
		verify(columnAnalyzerOverrideDao, never()).getByQualifiedNames(any());
	}

	@Test
	public void testLoadColumnAnalyzerOverridesWithNullListReturnsEmpty() {
		// config.getColumnAnalyzerOverrides() == null
		assertTrue(manager.loadColumnAnalyzerOverrides(new SearchConfiguration()).isEmpty());
		verify(columnAnalyzerOverrideDao, never()).getByQualifiedNames(any());
	}

	@Test
	public void testLoadColumnAnalyzerOverridesWithEmptyListReturnsEmpty() {
		SearchConfiguration config = new SearchConfiguration()
				.setColumnAnalyzerOverrides(Collections.emptyList());

		assertTrue(manager.loadColumnAnalyzerOverrides(config).isEmpty());
		verify(columnAnalyzerOverrideDao, never()).getByQualifiedNames(any());
	}

	@Test
	public void testLoadColumnAnalyzerOverridesWithOnlyInlineElementsSkipsDao() {
		// Schema permits inline ColumnAnalyzerOverride literals in the list; with no $ref
		// elements the DAO is not hit, but each inline literal is materialized into a typed
		// ColumnAnalyzerOverride POJO so the build path can walk its entries uniformly.
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("title")
				.setAnalyzer(java.util.Map.of(
						"analyzer", java.util.Map.of("default",
								java.util.Map.of("type", "custom", "tokenizer", "standard"))));
		java.util.Map<String, Object> inlineLiteral = java.util.Map.of(
				"overrides", java.util.List.of(java.util.Map.of(
						"columnName", "title",
						"analyzer", entry.getAnalyzer())));
		SearchConfiguration config = new SearchConfiguration()
				.setColumnAnalyzerOverrides(Collections.singletonList(inlineLiteral));

		List<ColumnAnalyzerOverride> result = manager.loadColumnAnalyzerOverrides(config);

		assertEquals(1, result.size());
		assertEquals(1, result.get(0).getOverrides().size());
		assertEquals("title", result.get(0).getOverrides().get(0).getColumnName());
		verify(columnAnalyzerOverrideDao, never()).getByQualifiedNames(any());
	}

	@Test
	public void testLoadColumnAnalyzerOverridesMixesRefAndInline() {
		// One $ref + one inline literal: the ref's qname goes to the DAO, the inline literal
		// is materialized in-memory, and the union is returned (DAO-loaded first, inline last).
		java.util.Map<String, Object> inlineLiteral = java.util.Map.of(
				"overrides", java.util.List.of(java.util.Map.of(
						"columnName", "abstract",
						"analyzer", java.util.Map.of(
								"analyzer", java.util.Map.of("default",
										java.util.Map.of("type", "custom", "tokenizer", "standard"))))));
		SearchConfiguration config = new SearchConfiguration()
				.setColumnAnalyzerOverrides(Arrays.asList(
						new org.json.JSONObject().put("$ref", "biomed-pubs"),
						inlineLiteral));
		ColumnAnalyzerOverride loaded = new ColumnAnalyzerOverride().setName("pubs");
		Map<String, ColumnAnalyzerOverride> daoResult = new java.util.LinkedHashMap<>();
		daoResult.put("biomed-pubs", loaded);
		when(columnAnalyzerOverrideDao.getByQualifiedNames(Arrays.asList("biomed-pubs")))
				.thenReturn(daoResult);

		List<ColumnAnalyzerOverride> result = manager.loadColumnAnalyzerOverrides(config);

		assertEquals(2, result.size());
		assertEquals("pubs", result.get(0).getName());
		assertEquals("abstract", result.get(1).getOverrides().get(0).getColumnName());
	}

	// -------- materializeInlineAnalyzerSlots --------

	@Test
	public void testMaterializeInlineAnalyzerSlotsWithInlineDefault() {
		// Inline defaultAnalyzer — gets a synthetic qname, the slot is rewritten in place
		// to a $ref Map carrying that qname, and the synthetic TextAnalyzer is returned.
		java.util.Map<String, Object> inlineDefault = java.util.Map.of(
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "standard")));
		SearchConfiguration config = new SearchConfiguration().setDefaultAnalyzer(inlineDefault);

		Map<String, TextAnalyzer> result = manager.materializeInlineAnalyzerSlots(
				config, Collections.emptyList());

		assertEquals(1, result.size());
		assertTrue(result.containsKey("synapse-inline_default"));
		assertEquals("synapse-inline_default", SearchOpaqueJsonUtil.readRef(config.getDefaultAnalyzer()));
		// The synthetic TextAnalyzer carries the original inline JSON as its settings, so
		// resolveAnalyzers can re-parse it uniformly with DAO-loaded analyzers.
		assertEquals(inlineDefault, result.get("synapse-inline_default").getSettings());
	}

	@Test
	public void testMaterializeInlineAnalyzerSlotsWithRefDefaultIsNoop() {
		// Default is a $ref; the helper does not touch it, no synthetic entries returned.
		java.util.Map<String, String> refDefault = java.util.Map.of("$ref", "biomed-PRIMARY");
		SearchConfiguration config = new SearchConfiguration().setDefaultAnalyzer(refDefault);

		Map<String, TextAnalyzer> result = manager.materializeInlineAnalyzerSlots(
				config, Collections.emptyList());

		assertTrue(result.isEmpty());
		assertEquals("biomed-PRIMARY", SearchOpaqueJsonUtil.readRef(config.getDefaultAnalyzer()));
	}

	@Test
	public void testMaterializeInlineAnalyzerSlotsWithInlineOverrideEntries() {
		// Two override entries — both inline. Each gets its own synthetic qname; the slot is
		// rewritten in place and a synthetic TextAnalyzer is added per entry.
		java.util.Map<String, Object> inlineA = java.util.Map.of(
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "standard")));
		java.util.Map<String, Object> inlineB = java.util.Map.of(
				"analyzer", java.util.Map.of("default",
						java.util.Map.of("type", "custom", "tokenizer", "keyword")));
		ColumnAnalyzerOverrideEntry e1 = new ColumnAnalyzerOverrideEntry()
				.setColumnName("title").setAnalyzer(inlineA);
		ColumnAnalyzerOverrideEntry e2 = new ColumnAnalyzerOverrideEntry()
				.setColumnName("body").setAnalyzer(inlineB);
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setName("pubs").setOverrides(Arrays.asList(e1, e2));

		Map<String, TextAnalyzer> result = manager.materializeInlineAnalyzerSlots(
				null, Collections.singletonList(override));

		assertEquals(2, result.size());
		assertTrue(result.containsKey("synapse-inline_override_0"));
		assertTrue(result.containsKey("synapse-inline_override_1"));
		assertEquals("synapse-inline_override_0", SearchOpaqueJsonUtil.readRef(e1.getAnalyzer()));
		assertEquals("synapse-inline_override_1", SearchOpaqueJsonUtil.readRef(e2.getAnalyzer()));
	}

	@Test
	public void testMaterializeInlineAnalyzerSlotsLeavesRefEntriesAlone() {
		// $ref override entry passes through; only the inline entry is rewritten.
		ColumnAnalyzerOverrideEntry refEntry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c1").setAnalyzer(java.util.Map.of("$ref", "biomed-FOO"));
		ColumnAnalyzerOverrideEntry inlineEntry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("c2").setAnalyzer(java.util.Map.of(
						"analyzer", java.util.Map.of("default",
								java.util.Map.of("type", "custom", "tokenizer", "standard"))));
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setName("o").setOverrides(Arrays.asList(refEntry, inlineEntry));

		Map<String, TextAnalyzer> result = manager.materializeInlineAnalyzerSlots(
				null, Collections.singletonList(override));

		assertEquals(1, result.size());
		assertEquals("biomed-FOO", SearchOpaqueJsonUtil.readRef(refEntry.getAnalyzer()));
		assertEquals("synapse-inline_override_0", SearchOpaqueJsonUtil.readRef(inlineEntry.getAnalyzer()));
	}

	@Test
	public void testMaterializeInlineAnalyzerSlotsWithNullInputsReturnsEmpty() {
		Map<String, TextAnalyzer> result = manager.materializeInlineAnalyzerSlots(
				null, Collections.emptyList());

		assertTrue(result.isEmpty());
	}

	// -------- validateReferencedResources / assertAnalyzerExists --------

	@Test
	public void testValidateReferencedResourcesWithNullDefaultAndNullOverridesIsNoop() {
		// Both inputs null — both early-return paths fire; no validation is run.
		manager.validateReferencedResources(null, null, Collections.emptyMap());
	}

	@Test
	public void testValidateReferencedResourcesPassesWhenDefaultExistsInLoadedMap() {
		Map<String, TextAnalyzer> analyzers = new java.util.HashMap<>();
		analyzers.put("biomed-PRIMARY", new TextAnalyzer().setName("PRIMARY"));

		manager.validateReferencedResources("biomed-PRIMARY", null, analyzers);
	}

	@Test
	public void testValidateReferencedResourcesThrowsWhenDefaultMissing() {
		// Default analyzer qname provided but not present in the loaded analyzers map.
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> manager.validateReferencedResources(
						"biomed-MISSING", null, Collections.emptyMap()));
		assertTrue(e.getMessage().contains("biomed-MISSING"));
		assertTrue(e.getMessage().contains("defaultAnalyzer"));
	}

	@Test
	public void testValidateReferencedResourcesIgnoresOverrideWithNullEntries() {
		// ColumnAnalyzerOverride whose getOverrides() is null is skipped — the inner-list
		// guard prevents NPE.
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride().setName("o");
		// no inner entries
		manager.validateReferencedResources(
				null, Collections.singletonList(override), Collections.emptyMap());
	}

	@Test
	public void testValidateReferencedResourcesThrowsWhenOverrideAnalyzerMissing() {
		// Override entry references a TextAnalyzer that wasn't loaded.
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("title")
				.setAnalyzer(new org.json.JSONObject().put("$ref", "biomed-MISSING"));
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setName("pubs")
				.setOverrides(Collections.singletonList(entry));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> manager.validateReferencedResources(
						null, Collections.singletonList(override), Collections.emptyMap()));
		assertTrue(e.getMessage().contains("biomed-MISSING"));
		assertTrue(e.getMessage().contains("override 'pubs'"));
		assertTrue(e.getMessage().contains("'title'"));
	}

	@Test
	public void testValidateReferencedResourcesIgnoresOverrideEntryWithNoRefAnalyzer() {
		// An entry whose analyzer slot is null (or inline-not-ref) yields readRef==null;
		// assertAnalyzerExists's null-qname branch returns without checking the map.
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry()
				.setColumnName("title")
				.setAnalyzer(null);
		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride()
				.setName("pubs")
				.setOverrides(Collections.singletonList(entry));

		manager.validateReferencedResources(
				null, Collections.singletonList(override), Collections.emptyMap());
	}

	// -------- handleDelete: deleteIndex throws but status row is also cleared --------

	@Test
	public void testHandleDeleteWhenSlotADeleteThrowsStillAttemptsSlotBAndSkipsStatusCleanup() throws Exception {
		// A failure deleting slot A must not skip slot B's delete — each slot is isolated —
		// and since not every slot was confirmed deleted, the status/source-edge rows are
		// left in place so a retry can find and finish the cleanup.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));
		doThrow(new RuntimeException("AOSS unavailable"))
				.when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");

		// call under test — must not throw
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(statusDao, never()).delete(any());
		verify(definingSqlDependencyDao, never()).deleteObject(any());
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteWhenSlotBDeleteThrowsStillAttemptsSlotAAndSkipsStatusCleanup() throws Exception {
		// Same isolation guarantee for the opposite slot: slot A's delete already ran before
		// slot B throws, and the status/source-edge rows are still left in place.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));
		doThrow(new RuntimeException("AOSS unavailable"))
				.when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");

		// call under test — must not throw
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(statusDao, never()).delete(any());
		verify(definingSqlDependencyDao, never()).deleteObject(any());
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteOnConcurrentDeleteThrowsRecoverableAndStillAttemptsOtherSlot() throws Exception {
		// AOSS rejects deleteIndex when another worker is mid-delete on the same index; this
		// must translate into a RecoverableMessageException so the caller retries. Slot A is
		// attempted (and succeeds) before slot B's concurrent-delete failure aborts the rest.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));
		ErrorCause cause = ErrorCause.of(b -> b
				.type("status_exception")
				.reason("Deletion failed for indices [search-index-syn456] due to concurrent deletes, please try again"));
		OpenSearchException concurrentDelete = new OpenSearchException(
				ErrorResponse.of(er -> er.error(cause).status(400)));
		doThrow(concurrentDelete).when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleDelete(progressCallback, ENTITY_ID));

		assertSame(concurrentDelete, thrown.getCause());
		assertEquals("Concurrent delete in progress while deleting search index for entity "
				+ ENTITY_ID, thrown.getMessage());
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(statusDao, never()).delete(any());
		verify(definingSqlDependencyDao, never()).deleteObject(any());
	}

	@Test
	public void testHandleDeleteWithStatusClearedBetweenPrecheckAndLockIsNoop() throws Exception {
		// First getState returns present (precheck passes), second returns empty (a concurrent
		// delete already cleaned up under the lock). The lock-protected path early-returns.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L))
				.thenReturn(Optional.of(SearchIndexState.ACTIVE)) // precheck
				.thenReturn(Optional.empty()); // recheck under lock

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager, never()).deleteIndex(any());
		verify(statusDao, never()).delete(any());
		verify(writeLock).close();
	}

	// -------- buildIndex error-unwrap branches --------

	@Test
	public void testHandleCreateUnwrapsLockUnavailableNestedInsideAnotherException() throws Exception {
		// LockUnavilableException wrapped inside another exception is still a transient
		// writer-contention signal — surface the wrapped lock exception so the worker re-queues.
		LockUnavilableException nestedLock = new LockUnavilableException(
				LockType.Write, "k", "other-worker");
		RuntimeException wrapper = new RuntimeException("wrapped", nestedLock);
		stubHappyPathThroughStream();
		doThrow(wrapper).when(indexDao).queryAsStream(any(), any());

		// buildIndex's inner cause-cause unwrap surfaces the LockUnavilableException; the
		// outer handleCreate wrapper then converts it to RecoverableMessageException. The
		// branch under test is the LockUnavilableException unwrap path inside buildIndex —
		// the resulting RecoverableMessageException's cause must be the original lock ex.
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID));
		assertTrue(thrown.getCause() instanceof LockUnavilableException,
				"the recoverable wrapper must carry the LockUnavilableException as its cause");

		// Not marked FAILED — transient.
		ArgumentCaptor<SearchIndexStatus> nestedLockCaptor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(nestedLockCaptor.capture());
		assertTrue(nestedLockCaptor.getAllValues().stream()
				.noneMatch(s -> s.getState() == SearchIndexState.FAILED),
				"nested lock-unavailable must not mark the index FAILED");
	}

	// --- registerSource ---

	@Test
	public void testRegisterSourceWithTableSourceRecordsEdge() {
		IdAndVersion searchIndexId = IdAndVersion.parse("syn456");
		when(tableManagerSupport.getTableType(SOURCE_ID)).thenReturn(TableType.table);

		// call under test
		manager.registerSource(searchIndexId, "SELECT name, COUNT(*) FROM syn789 GROUP BY name");

		verify(definingSqlDependencyDao).setSourceTable(searchIndexId, ObjectType.SEARCH_INDEX.name(), SOURCE_ID);
		verifyNoInteractions(columnModelManager, indexAuthorizationSnapshotManager);
	}

	@Test
	public void testRegisterSourceWithVirtualTableSourceThrows() {
		IdAndVersion searchIndexId = IdAndVersion.parse("syn456");
		// A virtual table is a query rewrite with no materialized index, snapshot, or status events,
		// so a SearchIndex over one could never build.
		when(tableManagerSupport.getTableType(SOURCE_ID)).thenReturn(TableType.virtualtable);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> manager.registerSource(searchIndexId, "SELECT foo FROM syn789"));
		assertEquals("The defining SQL of a search index cannot reference a virtual table.", ex.getMessage());
		verifyNoInteractions(definingSqlDependencyDao);
	}

	@Test
	public void testHandleCreateWithAggregateOverBenefactorSourceRecordsFailed() throws Exception {
		stubBuildLock();
		stubSourceLock();
		SearchIndex searchIndex = new SearchIndex()
				.setDefiningSQL("SELECT name, COUNT(*) FROM syn789 GROUP BY name").setParentId("syn100");
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(SOURCE_ID))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(0L);
		// An aggregating defining SQL would collapse rows spanning different benefactors into one
		// output row, for which there is no correct per-row benefactor.
		IndexAuthorizationSnapshot mvSnapshot = tableSnapshot("100").setIndexDescription(new IndexDescriptionSnapshot()
				.setObjectId("syn789")
				.setTableType(TableType.materializedview.name())
				.setBenefactors(List.of(new BenefactorColumn().setBenefactorColumnName("ROW_BENEFACTOR_A0")
						.setBenefactorType(ObjectType.ENTITY.name())))
				.setDependencies(List.of(new SourceDependency().setObjectId("syn10").setTableType(TableType.table.name())))
				.setDefiningSql("SELECT * FROM syn10"));
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(mvSnapshot));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("The defining SQL of a search index over an access-controlled source cannot include a group by clause."),
				captor.getAllValues().get(1));
		verify(columnModelManager, never()).createColumnModel(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateWithDefiningSqlReferencingSecondTableRecordsFailed() throws Exception {
		stubBuildLock();
		stubSourceLock();
		SearchIndex searchIndex = new SearchIndex().setId(ENTITY_ID)
				.setDefiningSQL("SELECT name FROM syn789 UNION SELECT name FROM syn790").setParentId("syn100");
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(SOURCE_ID))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(0L);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(new SearchIndexStatus().setSearchIndexId(ENTITY_ID).setState(SearchIndexState.FAILED)
				.setErrorMessage("Search index " + ENTITY_ID + " references syn790 beyond its single source syn789"),
				captor.getAllValues().get(1));
		verify(tableManagerSupport, never()).getTableSchema(any());
		verify(tableManagerSupport, never()).getTableType(any());
		verify(columnModelManager, never()).createColumnModel(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testHandleCreateWithUnknownColumnRecordsFailed() throws Exception {
		stubBuildLock();
		stubSourceLock();
		// A bare double-quoted string parses as a SQL identifier; "tag" is absent from the source's
		// schema (syn789 only has "name"), so translation must fail the build rather than silently
		// dropping it or treating it as a literal.
		SearchIndex searchIndex = new SearchIndex()
				.setDefiningSQL("SELECT name, \"tag\" FROM syn789").setParentId("syn100");
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(SOURCE_ID))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(SOURCE_ID)).thenReturn(0L);
		when(indexAuthorizationSnapshotManager.getAuthorizationSnapshot(SOURCE_ID)).thenReturn(Optional.of(SOURCE_SNAPSHOT));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(NAME_COLUMN);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		SearchIndexStatus failed = captor.getAllValues().get(1);
		assertEquals(SearchIndexState.FAILED, failed.getState());
		assertTrue(failed.getErrorMessage().contains("Unknown column"),
				"expected the unknown-column message, got: " + failed.getErrorMessage());
		assertTrue(failed.getErrorMessage().contains("tag"),
				"expected the unknown-column message to name 'tag', got: " + failed.getErrorMessage());
		verify(columnModelManager, never()).createColumnModel(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	// -------- buildWithBenefactorColumns (package-private) --------

	@Test
	public void testBuildWithBenefactorColumnsWithMaterializedViewOverTwoViews() throws ParseException {
		// Two entity views joined on a shared "studyId" column, and a materialized view over them. Ids
		// and column ids match the lib-table-cluster TwoViewMaterializedView fixture so the translated
		// names are stable (T801/T802/T803, _C701_/_C702_/_C703_, ROW_BENEFACTOR__A0/__A1).
		IdAndVersion leftViewId = IdAndVersion.parse("syn801");
		IdAndVersion rightViewId = IdAndVersion.parse("syn802");
		IdAndVersion mvId = IdAndVersion.parse("syn803");
		IndexDescription leftView = new ViewIndexDescription(leftViewId, TableType.entityview, -1L);
		IndexDescription rightView = new ViewIndexDescription(rightViewId, TableType.entityview, -1L);
		ColumnModel leftStudy = TableModelTestUtils.createColumn(701L, "studyId", ColumnType.INTEGER);
		ColumnModel rightStudy = TableModelTestUtils.createColumn(702L, "studyId", ColumnType.INTEGER);
		ColumnModel mvStudy = TableModelTestUtils.createColumn(703L, "studyId", ColumnType.INTEGER);
		String mvDefiningSql = "select " + leftViewId + ".studyId from " + leftViewId + " join " + rightViewId
				+ " on (" + leftViewId + ".studyId = " + rightViewId + ".studyId) order by " + leftViewId + ".studyId";

		SchemaProvider schemaProvider = new SchemaProvider() {
			@Override
			public TableType getTableType(IdAndVersion id) {
				return TableType.entityview;
			}

			@Override
			public List<ColumnModel> getTableSchema(IdAndVersion id) {
				if (leftViewId.equals(id)) {
					return List.of(leftStudy);
				}
				if (rightViewId.equals(id)) {
					return List.of(rightStudy);
				}
				return List.of(mvStudy);
			}

			@Override
			public ColumnModel getColumnModel(String id) {
				if (leftStudy.getId().equals(id)) {
					return leftStudy;
				}
				if (rightStudy.getId().equals(id)) {
					return rightStudy;
				}
				return mvStudy;
			}
		};

		IndexDescriptionLookup lookup = id -> leftViewId.equals(id) ? leftView : rightView;
		MaterializedViewIndexDescription mvIndex = new MaterializedViewIndexDescription(mvId, mvDefiningSql, lookup);

		QueryTranslator base = QueryTranslator.builder().sql("select * from " + mvId).schemaProvider(schemaProvider)
				.sqlContext(SqlContext.query).indexDescription(mvIndex).userId(1L).build();
		IndexDescriptionSnapshot mvSnapshot = new IndexDescriptionSnapshot().setObjectId(mvId.toString())
				.setTableType(TableType.materializedview.name())
				.setBenefactors(mvIndex.getBenefactors().stream()
						.map(b -> new BenefactorColumn().setBenefactorColumnName(b.getBenefactorColumnName())
								.setBenefactorType(b.getBenefactorType().name()))
						.collect(Collectors.toList()));

		// call under test
		TranslatedQuery query = SearchIndexLifecycleManagerImpl.buildWithBenefactorColumns(base, mvSnapshot);

		// The two physical benefactor columns are spliced in after the document column and ahead of the
		// by-name ROW_ID/ROW_VERSION metadata.
		assertEquals("SELECT _C703_, ROW_BENEFACTOR__A0, ROW_BENEFACTOR__A1, ROW_ID, ROW_VERSION FROM T803",
				query.getOutputSQL());
		// The benefactor columns are mirrored into the result headers as INTEGER columns, in
		// getBenefactors() order, after the document column. ROW_ID/ROW_VERSION are read by name and are
		// not select headers.
		assertEquals(List.of(
				new SelectColumn().setName("studyId").setColumnType(ColumnType.INTEGER).setId("703"),
				new SelectColumn().setName("ROW_BENEFACTOR__A0").setColumnType(ColumnType.INTEGER),
				new SelectColumn().setName("ROW_BENEFACTOR__A1").setColumnType(ColumnType.INTEGER)),
				query.getSelectColumns());
	}

	@Test
	public void testBuildWithBenefactorColumnsWithEntityViewSource() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn801");
		ViewIndexDescription view = new ViewIndexDescription(viewId, TableType.entityview, -1L);
		ColumnModel study = TableModelTestUtils.createColumn(701L, "studyId", ColumnType.INTEGER);
		QueryTranslator base = QueryTranslator.builder().sql("select studyId from " + viewId)
				.schemaProvider(singleTableSchemaProvider(TableType.entityview, study))
				.sqlContext(SqlContext.query).indexDescription(view).build();
		IndexDescriptionSnapshot viewSnapshot = new IndexDescriptionSnapshot().setObjectId(viewId.toString())
				.setTableType(TableType.entityview.name())
				.setBenefactors(List.of(new BenefactorColumn().setBenefactorColumnName("ROW_BENEFACTOR")
						.setBenefactorType(ObjectType.ENTITY.name())));

		// call under test
		TranslatedQuery query = SearchIndexLifecycleManagerImpl.buildWithBenefactorColumns(base, viewSnapshot);

		// The spliced ROW_BENEFACTOR is read positionally; the trailing by-name copy feeds readRow.
		assertEquals("SELECT _C701_, ROW_BENEFACTOR, ROW_ID, ROW_VERSION, ROW_BENEFACTOR FROM T801",
				query.getOutputSQL());
		assertEquals(List.of(
				new SelectColumn().setName("studyId").setColumnType(ColumnType.INTEGER).setId("701"),
				new SelectColumn().setName("ROW_BENEFACTOR").setColumnType(ColumnType.INTEGER)),
				query.getSelectColumns());
	}

	@Test
	public void testBuildWithBenefactorColumnsWithTableSource() throws ParseException {
		IdAndVersion tableId = IdAndVersion.parse("syn801");
		ColumnModel study = TableModelTestUtils.createColumn(701L, "studyId", ColumnType.INTEGER);
		QueryTranslator base = QueryTranslator.builder().sql("select studyId from " + tableId)
				.schemaProvider(singleTableSchemaProvider(TableType.table, study))
				.sqlContext(SqlContext.query).indexDescription(new TableIndexDescription(tableId)).build();
		IndexDescriptionSnapshot tableSnapshot = new IndexDescriptionSnapshot().setObjectId(tableId.toString())
				.setTableType(TableType.table.name()).setBenefactors(List.of());

		// call under test
		TranslatedQuery query = SearchIndexLifecycleManagerImpl.buildWithBenefactorColumns(base, tableSnapshot);

		assertEquals(base.getOutputSQL(), query.getOutputSQL());
		assertEquals(base.getSelectColumns(), query.getSelectColumns());
	}

	private static SchemaProvider singleTableSchemaProvider(TableType tableType, ColumnModel column) {
		return new SchemaProvider() {
			@Override
			public TableType getTableType(IdAndVersion id) {
				return tableType;
			}

			@Override
			public List<ColumnModel> getTableSchema(IdAndVersion id) {
				return List.of(column);
			}

			@Override
			public ColumnModel getColumnModel(String id) {
				return column;
			}
		};
	}

	// -------- computeShardCount boundary tests --------

	@Test
	public void testComputeShardCountWithNull() {
		// call under test
		assertEquals(1, SearchIndexLifecycleManagerImpl.computeShardCount(null));
	}

	@Test
	public void testComputeShardCountWithZero() {
		// call under test
		assertEquals(1, SearchIndexLifecycleManagerImpl.computeShardCount(0L));
	}

	@Test
	public void testComputeShardCountWithNegative() {
		// call under test
		assertEquals(1, SearchIndexLifecycleManagerImpl.computeShardCount(-5L));
	}

	@Test
	public void testComputeShardCountWithOneByteYieldsSingleShard() {
		// 1 byte << TARGET_SHARD_BYTES — always 1 shard
		// call under test
		assertEquals(1, SearchIndexLifecycleManagerImpl.computeShardCount(1L));
	}

	@Test
	public void testComputeShardCountWithExactMultiple() {
		// Exactly one TARGET_SHARD_BYTES = ceil(1) = 1 shard
		// call under test
		assertEquals(1, SearchIndexLifecycleManagerImpl.computeShardCount(
				SearchIndexLifecycleManagerImpl.TARGET_SHARD_BYTES));
	}

	@Test
	public void testComputeShardCountWithOneByteOverTarget() {
		// TARGET_SHARD_BYTES + 1 = ceil(>1.0) = 2 shards
		// call under test
		assertEquals(2, SearchIndexLifecycleManagerImpl.computeShardCount(
				SearchIndexLifecycleManagerImpl.TARGET_SHARD_BYTES + 1));
	}

	@Test
	public void testComputeShardCountWithClampToMax() {
		// A size that would bucket into MAX_SHARDS + 1 shards must be clamped down to
		// MAX_SHARDS. Expressed as a multiple of TARGET_SHARD_BYTES so it stays well clear
		// of the ceiling-arithmetic overflow that Long.MAX_VALUE would cause.
		long overMaxBytes = SearchIndexLifecycleManagerImpl.TARGET_SHARD_BYTES
				* (SearchIndexLifecycleManagerImpl.MAX_SHARDS + 1);
		// call under test
		assertEquals(SearchIndexLifecycleManagerImpl.MAX_SHARDS,
				SearchIndexLifecycleManagerImpl.computeShardCount(overMaxBytes));
	}

	// -------- rebuildIfStale --------

	@Test
	public void testRebuildIfStaleRebuildsWhenWaitingForSource() throws Exception {
		// State is WAITING_FOR_SOURCE under the lock → a full rebuild runs.
		stubHappyPathThroughStream();
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.WAITING_FOR_SOURCE));

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(indexDao).queryAsStream(any(), any());
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getAllValues().get(0).getState());
		assertEquals(SearchIndexState.ACTIVE, captor.getAllValues().get(1).getState());
		verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-a"), eq(Optional.empty()));
	}

	@Test
	public void testRebuildIfStaleNoOpsWhenNoStatusRow() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID))).thenReturn(Optional.empty());

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(entityManager, never()).getEntityWithoutAuthorization(any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testRebuildIfStaleWithCreatingStateNoOps() throws Exception {
		// A CREATING index is mid-build elsewhere (or the lost-wakeup case) — no-op.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.CREATING));

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(entityManager, never()).getEntityWithoutAuthorization(any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testRebuildIfStaleWithFailedStateNoOps() throws Exception {
		// FAILED is terminal until a manual rebuild — no build.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.FAILED));

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(entityManager, never()).getEntityWithoutAuthorization(any(), any());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	@Test
	public void testRebuildIfStaleWithActiveRebuilds() throws Exception {
		// ACTIVE always rebuilds — a source's status version is not a reliable content fingerprint
		// for a view/materialized-view source, so there is no version-compare skip. The rebuild
		// streams into the idle slot (the alias points at slot -a, so this build targets slot -b)
		// and swaps.
		stubHappyPathThroughStream();
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.ACTIVE));
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID))
				.thenReturn(Optional.of("search-index-" + ENTITY_ID + "-a"));

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-b"),
				any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), isNull());
		verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-b"), eq(Optional.of("search-index-" + ENTITY_ID + "-a")));
		// A rebuild does not write CREATING — the live index stays ACTIVE-visible until the swap.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.ACTIVE, captor.getValue().getState());
	}

	@Test
	public void testRebuildIfStaleOnLockContentionThrowsRecoverable() throws Exception {
		// Another worker holds the per-entity lock (an in-flight build). rebuildIfStale requeues the
		// message for retry via RecoverableMessageException, same as the materialized view path.
		stubLockUnavailable();

		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				// call under test
				() -> manager.rebuildIfStale(progressCallback, ENTITY_ID));

		assertEquals("Search index " + ENTITY_ID + " is locked by another worker", thrown.getMessage());
		verify(indexDao, never()).queryAsStream(any(), any());
	}

	// -------- buildIndex — blue-green rebuild-path coverage --------

	@Test
	public void testHandleUpdateOnRebuildKeepsActiveStateAndSwapsToOtherSlot() throws Exception {
		// getAliasTarget returns the alias's current physical target (slot -a) — this is a
		// rebuild, not a first build. No CREATING write; the build streams into slot -b and
		// swaps the alias to it on success. The demoted slot -a is deleted right after the swap
		// so it does not sit around consuming space until the next rebuild.
		stubHappyPathThroughStream();
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID))
				.thenReturn(Optional.of("search-index-" + ENTITY_ID + "-a"));

		// call under test
		manager.handleUpdate(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-b"),
				any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), isNull());
		verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-b"), eq(Optional.of("search-index-" + ENTITY_ID + "-a")));
		org.mockito.InOrder order = org.mockito.Mockito.inOrder(openSearchManager);
		order.verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-b"), eq(Optional.of("search-index-" + ENTITY_ID + "-a")));
		order.verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		SearchIndexStatus active = captor.getValue();
		assertEquals(SearchIndexState.ACTIVE, active.getState());
		// No CREATING write on a rebuild.
		verify(statusDao, never()).createOrUpdate(argThat(s -> s.getState() == SearchIndexState.CREATING));
	}

	@Test
	public void testHandleCreateOnFirstBuildDoesNotDeleteOldTarget() throws Exception {
		// First build: getAliasTarget is empty, so there is no demoted index to delete after
		// the swap — only the pre-build idle-slot delete (a no-op, slot -a never existed) runs.
		stubHappyPathThroughStream();

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-a"), eq(Optional.empty()));
		verify(openSearchManager, times(1)).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager, never()).deleteIndex("search-index-" + ENTITY_ID + "-b");
	}

	@Test
	public void testHandleUpdateOnRebuildSwallowsPostSwapDeleteFailure() throws Exception {
		// The post-swap delete of the demoted slot is best-effort: if it throws, the build must
		// still complete and record ACTIVE — the swap already succeeded and the new index is
		// live and correct, only the old index's cleanup failed. The next rebuild's pre-build
		// idle-slot delete retries it.
		stubHappyPathThroughStream();
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID))
				.thenReturn(Optional.of("search-index-" + ENTITY_ID + "-a"));
		// Explicit no-op stub for the pre-build idle-slot delete on -b, since the -a stub below
		// makes strict stubbing require every deleteIndex argument to be stubbed individually.
		doNothing().when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-b");
		doThrow(new RuntimeException("delete failed"))
				.when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID + "-a");

		// call under test
		manager.handleUpdate(progressCallback, ENTITY_ID);

		verify(openSearchManager).swapAlias(eq("search-index-" + ENTITY_ID),
				eq("search-index-" + ENTITY_ID + "-b"), eq(Optional.of("search-index-" + ENTITY_ID + "-a")));
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.ACTIVE, captor.getValue().getState());
	}

	@Test
	public void testHandleCreateRebuildFailureLeavesFailedAndCleansSlot() throws Exception {
		// A rebuild (getAliasTarget present) that fails after slot selection still writes FAILED
		// uniformly, cleans up only the slot it was building into, and never swaps the alias.
		stubHappyPathThroughCreateIndex();
		stubSchemaProviderForTranslator();
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID))
				.thenReturn(Optional.of("search-index-" + ENTITY_ID + "-a"));
		doThrow(new RuntimeException("shard allocation failed"))
				.when(openSearchManager).waitForIndexWritable("search-index-" + ENTITY_ID + "-b");

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.FAILED, captor.getValue().getState());
		// Cleanup only touches the idle slot being built — the live slot -a is untouched.
		verify(openSearchManager, times(2)).deleteIndex("search-index-" + ENTITY_ID + "-b");
		verify(openSearchManager, never()).deleteIndex("search-index-" + ENTITY_ID + "-a");
		verify(openSearchManager, never()).swapAlias(any(), any(), any());
	}

}
