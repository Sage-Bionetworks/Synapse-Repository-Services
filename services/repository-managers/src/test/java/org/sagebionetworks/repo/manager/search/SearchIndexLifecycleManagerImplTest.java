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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
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
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ObjectType;
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
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
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
	// Source index description for the source table used in DEFINING_SQL ("SELECT * FROM syn789").
	private static final IndexDescription SOURCE_INDEX_DESCRIPTION =
			new TableIndexDescription(IdAndVersion.parse("syn789"));
	private static final String DEFINING_SQL = "SELECT * FROM syn789";

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
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(0L);
		// SchemaProvider stubs so "SELECT * FROM syn789" translates in QueryTranslator.
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse("syn789")))
				.thenReturn(Collections.singletonList(nameCol));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(nameCol);
	}

	/**
	 * Stubs up through getRowCountForTable (source is AVAILABLE, row count = 0) but does NOT
	 * include the SchemaProvider stubs needed by QueryTranslator. Use for tests that bail before
	 * the translator is built (e.g., the row-count guard, table-unavailable, config error paths
	 * that still reach createIndex but not queryAsStream).
	 */
	private void stubHappyPathThroughCreateIndex() throws Exception {
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(0L);
	}

	/**
	 * Stubs the source index description resolved by buildIndex once it proceeds past the
	 * row-count check. Used in addition to stubHappyPathThroughCreateIndex for tests that
	 * reach the index-creation phase and need the SchemaProvider stubs for QueryTranslator.
	 */
	private void stubSchemaProviderForTranslator() {
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse("syn789")))
				.thenReturn(Collections.singletonList(nameCol));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(nameCol);
	}

	@Test
	public void testHandleCreateStreamsViaIndexDao() throws Exception {
		// The happy path completes buildIndex and streams rows via indexDao.queryAsStream,
		// writing ACTIVE status at the end.
		// call under test
		stubHappyPathThroughStream();
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
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
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
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
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
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
				any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
		// waitForIndexWritable runs before the translator is built, so SchemaProvider stubs are
		// not needed.
		stubHappyPathThroughCreateIndex();
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
				new SearchIndexRowHandler("test-index", columns, openSearchManager);

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
				new SearchIndexRowHandler("test-index", columns, openSearchManager);

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
				"test-index", Collections.singletonList(col), openSearchManager);

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
				"test-index", Collections.singletonList(col), openSearchManager);

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
				"test-index", columns, openSearchManager);

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
				"search-index-syn1", Collections.singletonList(col), openSearchManager);

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
	public void testRowHandlerNextRowWithViewSourceWritesBenefactorFromRow() throws IOException {
		SelectColumn col = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		// A view exposes its single benefactor through Row.getBenefactorId() and keys the
		// document by ROW_ID (it appends no positional benefactor columns).
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), openSearchManager);

		Row row = new Row().setRowId(42L).setVersionNumber(1L).setBenefactorId(99L)
				.setValues(Collections.singletonList("hello"));
		// call under test
		handler.nextRow(row);
		handler.close();

		ArgumentCaptor<List<BulkOperation>> captor = ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).bulkIndex(eq("test-index"), captor.capture());
		BulkOperation op = captor.getValue().get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> doc = (Map<String, Object>) op.index().document();
		assertEquals("hello", doc.get("100"));
		assertEquals(99L, doc.get("_benefactor_0"));
		// View document id is the stable ROW_ID.
		assertEquals("42", op.index().id());
	}

	@Test
	public void testRowHandlerNextRowWithMaterializedViewSourceReadsTrailingBenefactors() throws IOException {
		SelectColumn col = new SelectColumn().setId("100").setName("title").setColumnType(ColumnType.STRING);
		// A materialized view with two dependencies appends two benefactor columns to the
		// trailing positional values; the handler is told how many via its positional count
		// (the value QueryTranslator reports). The document is keyed by ROW_ID.
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), openSearchManager);

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
		assertEquals(11L, doc.get("_benefactor_0"));
		assertEquals(22L, doc.get("_benefactor_1"));
		assertEquals("7", op.index().id());
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
		assertNull(SearchIndexLifecycleManagerImpl.convertForDocument(null, ColumnType.STRING));
	}

	@ParameterizedTest
	@EnumSource(value = ColumnType.class, names = {"STRING", "LARGETEXT", "MEDIUMTEXT", "LINK"})
	public void testConvertForDocumentBareStringTypesPassThrough(ColumnType type) {
		// call under test — bare-string types short-circuit to raw String pass-through so
		// AOSS doesn't receive a JSON-parsed value (which would be malformed for text fields).
		assertEquals("alpha", SearchIndexLifecycleManagerImpl.convertForDocument("alpha", type));
	}

	@ParameterizedTest
	@EnumSource(value = ColumnType.class, names = {"ENTITYID", "USERID"})
	public void testConvertForDocumentKeywordIdTypesPassThrough(ColumnType type) {
		// call under test — KEYWORD-category ID types are stored as raw strings in AOSS;
		// LONG-category IDs (FILEHANDLEID, EVALUATIONID) go through the JSON parse branch.
		assertEquals("syn123", SearchIndexLifecycleManagerImpl.convertForDocument("syn123", type));
	}

	@Test
	public void testConvertForDocumentWithIntegerParsesAsLong() {
		// call under test — INTEGER serializes to JSON number; Jackson surfaces it as Integer/Long.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument("42", ColumnType.INTEGER);

		assertEquals(42, ((Number) result).intValue());
	}

	@Test
	public void testConvertForDocumentWithDoubleParsesAsDouble() {
		// call under test
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument("3.14", ColumnType.DOUBLE);

		assertEquals(3.14d, ((Number) result).doubleValue(), 1e-9);
	}

	@Test
	public void testConvertForDocumentWithBooleanParses() {
		// call under test
		assertEquals(Boolean.TRUE, SearchIndexLifecycleManagerImpl.convertForDocument("true", ColumnType.BOOLEAN));
	}

	@Test
	public void testConvertForDocumentWithStringListParsesAsJsonArray() {
		// call under test — STRING_LIST stored as a JSON array string; AOSS expects a real list.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"[\"a\",\"b\"]", ColumnType.STRING_LIST);

		assertTrue(result instanceof List, "Expected a List, got " + result.getClass());
		assertEquals(Arrays.asList("a", "b"), result);
	}

	@Test
	public void testConvertForDocumentWithEntityIdListParsesAsJsonArray() {
		// call under test — ENTITYID_LIST also goes through JSON parse despite the underlying
		// type mapping being KEYWORD (the list branch wins over the keyword short-circuit).
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"[\"syn1\",\"syn2\"]", ColumnType.ENTITYID_LIST);

		assertEquals(Arrays.asList("syn1", "syn2"), result);
	}

	@Test
	public void testConvertForDocumentWithJsonTypeParsesAsMap() {
		// call under test — JSON column round-trips as a Map; AOSS stores it as a dynamic object.
		Object result = SearchIndexLifecycleManagerImpl.convertForDocument(
				"{\"foo\":\"bar\"}", ColumnType.JSON);

		assertTrue(result instanceof Map);
		assertEquals("bar", ((Map<?, ?>) result).get("foo"));
	}

	@Test
	public void testConvertForDocumentWithMalformedJsonThrows() {
		// call under test — a malformed JSON list value must throw IllegalArgumentException
		// so the build is recorded as FAILED with a clear message (not a silent doc-level error).
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> SearchIndexLifecycleManagerImpl.convertForDocument("[not-json", ColumnType.STRING_LIST));

		assertTrue(e.getMessage().contains("STRING_LIST"),
				"Exception must mention the column type: " + e.getMessage());
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
	public void testHandleCreateThrowsWhenSchemaIsNull() throws Exception {
		// getTableSchema returns null — buildIndex throws "no bound schema", caught
		// by the outer Throwable handler and recorded as FAILED.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(any(), any()))
				.thenReturn(new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100"));
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID))).thenReturn(null);

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED
						&& s.getErrorMessage() != null
						&& s.getErrorMessage().contains("no bound schema")));
	}

	@Test
	public void testHandleCreateThrowsWhenSchemaIsEmpty() throws Exception {
		// Empty schema also flows to FAILED via the outer Throwable handler.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(entityManager.getEntityWithoutAuthorization(any(), any()))
				.thenReturn(new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100"));
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.emptyList());

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
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

		verify(openSearchManager).createIndex(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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

		verify(openSearchManager).createIndex(any(), any(), eq(defaultQname), any(), any(), anyInt(), anyInt(), anyInt());
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

	// --- registerSchema: aggregation-over-benefactor-source guard ---

	@Test
	public void testRegisterSchemaWithAggregateOverBenefactorSourceThrows() {
		IdAndVersion searchIndexId = IdAndVersion.parse("syn456");
		IdAndVersion sourceId = IdAndVersion.parse("syn789");
		// A materialized-view source with a benefactor-bearing dependency. An aggregating
		// defining SQL would collapse rows spanning different benefactors into one output
		// row, for which there is no correct per-row benefactor — so it must be rejected.
		IndexDescription mvSource = mock(IndexDescription.class);
		when(mvSource.getTableHash()).thenReturn("hash");
		when(mvSource.getTableType()).thenReturn(TableType.materializedview);
		when(mvSource.getColumnNamesToAddToSelect(any(SqlContext.class), anyBoolean(), anyBoolean()))
				.thenReturn(Collections.emptyList());
		when(mvSource.getBenefactors()).thenReturn(Collections.singletonList(
				new BenefactorDescription("ROW_BENEFACTOR_A0", ObjectType.ENTITY)));
		ColumnModel fooColumn = new ColumnModel().setId("100").setName("foo")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(tableManagerSupport.getIndexDescription(sourceId)).thenReturn(mvSource);
		when(tableManagerSupport.getTableSchema(sourceId)).thenReturn(Collections.singletonList(fooColumn));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(fooColumn);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.registerSchema(searchIndexId, "SELECT foo, COUNT(*) FROM syn789 GROUP BY foo"));
		assertTrue(ex.getMessage().contains("cannot include a group by clause"),
				"expected the aggregation guard message, got: " + ex.getMessage());
		// Guard fires before any schema is bound.
		verify(columnModelManager, never()).bindColumnsToVersionOfObject(any(), any());
	}

	@Test
	public void testRegisterSchemaWithAggregateOverBenefactorlessSourceSucceeds() {
		IdAndVersion searchIndexId = IdAndVersion.parse("syn456");
		IdAndVersion sourceId = IdAndVersion.parse("syn789");
		// A plain table source has no benefactors, so an aggregating defining SQL is allowed —
		// row-level ACL is moot and the guard must not fire.
		ColumnModel fooColumn = new ColumnModel().setId("100").setName("foo")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(tableManagerSupport.getIndexDescription(sourceId)).thenReturn(new TableIndexDescription(sourceId));
		when(tableManagerSupport.getTableSchema(sourceId)).thenReturn(Collections.singletonList(fooColumn));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(fooColumn);
		when(columnModelManager.createColumnModel(any()))
				.thenReturn(new ColumnModel().setId("200").setName("foo").setColumnType(ColumnType.STRING).setMaximumSize(50L));

		// call under test — must not throw.
		manager.registerSchema(searchIndexId, "SELECT foo, COUNT(*) FROM syn789 GROUP BY foo");

		verify(columnModelManager).bindColumnsToVersionOfObject(any(), eq(searchIndexId));
		// The source -> SearchIndex edge is recorded so a later source-availability event finds it.
		verify(definingSqlDependencyDao).setSourceTable(searchIndexId, ObjectType.SEARCH_INDEX.name(), sourceId);
	}

	@Test
	public void testRegisterSchemaWithVirtualTableSourceThrows() {
		IdAndVersion searchIndexId = IdAndVersion.parse("syn456");
		IdAndVersion sourceId = IdAndVersion.parse("syn789");
		// A virtual table is a query rewrite with no materialized index and no status events, so
		// it would never trigger a source-availability rebuild — the SearchIndex must reject it.
		IndexDescription virtualSource = mock(IndexDescription.class);
		when(virtualSource.getTableType()).thenReturn(TableType.virtualtable);
		when(tableManagerSupport.getIndexDescription(sourceId)).thenReturn(virtualSource);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.registerSchema(searchIndexId, "SELECT foo FROM syn789"));
		assertTrue(ex.getMessage().contains("cannot reference a virtual table"),
				"expected the virtual-table guard message, got: " + ex.getMessage());
		// Guard fires before any schema is bound or dependency edge recorded.
		verify(columnModelManager, never()).bindColumnsToVersionOfObject(any(), any());
		verify(definingSqlDependencyDao, never()).setSourceTable(any(), any(), any());
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

		// call under test
		TranslatedQuery query = SearchIndexLifecycleManagerImpl.buildWithBenefactorColumns(base, mvIndex);

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
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.WAITING_FOR_SOURCE));
		when(entityManager.getEntityWithoutAuthorization(eq(ENTITY_ID), eq(SearchIndex.class))).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID)).thenReturn(Optional.empty());
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(0L);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse("syn789")))
				.thenReturn(Collections.singletonList(nameCol));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(nameCol);

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
		stubBuildLock();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		ColumnModel nameCol = new ColumnModel().setId("100").setName("name")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(KeyFactory.stringToKey(ENTITY_ID)))
				.thenReturn(Optional.of(SearchIndexState.ACTIVE));
		when(connectionFactory.getConnection(IdAndVersion.parse("syn789"))).thenReturn(indexDao);
		when(entityManager.getEntityWithoutAuthorization(eq(ENTITY_ID), eq(SearchIndex.class))).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(nameCol));
		when(searchConfigurationResolver.resolve(any(), any())).thenReturn(Optional.empty());
		when(openSearchManager.getAliasTarget("search-index-" + ENTITY_ID))
				.thenReturn(Optional.of("search-index-" + ENTITY_ID + "-a"));
		when(tableManagerSupport.getIndexDescription(IdAndVersion.parse("syn789")))
				.thenReturn(SOURCE_INDEX_DESCRIPTION);
		when(tableManagerSupport.getTableStatusOrCreateIfNotExists(IdAndVersion.parse("syn789")))
				.thenReturn(new TableStatus().setState(TableState.AVAILABLE));
		when(indexDao.getRowCountForTable(IdAndVersion.parse("syn789"))).thenReturn(0L);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse("syn789")))
				.thenReturn(Collections.singletonList(nameCol));
		when(tableManagerSupport.getColumnModel("100")).thenReturn(nameCol);

		// call under test
		manager.rebuildIfStale(progressCallback, ENTITY_ID);

		verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID + "-b"),
				any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
				any(), any(), any(), any(), anyInt(), anyInt(), anyInt());
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
