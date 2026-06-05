package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchIndexLifecycleManagerImpl.SearchIndexRowHandler;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
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
	private static final Long USER_ID = 123L;
	private static final Long ANON_ID = 273950L;
	private static final String DEFINING_SQL = "SELECT * FROM syn789";

	@Mock
	private ConnectionFactory connectionFactory;
	@Mock
	private OpenSearchManager openSearchManager;
	@Mock
	private SearchConfigurationResolver searchConfigurationResolver;
	@Mock
	private TableQueryManager tableQueryManager;
	@Mock
	private UserManager userManager;
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

	private UserInfo triggeringUser() {
		UserInfo user = new UserInfo(false, USER_ID, null);
		user.setRealmAnonymousUserId(ANON_ID);
		return user;
	}

	private UserInfo anonymousUser() {
		UserInfo user = new UserInfo(false, ANON_ID, null);
		user.setRealmAnonymousUserId(ANON_ID);
		return user;
	}

	@Test
	public void testHandleCreatePassesAnonymousUserToQueryManager() throws Exception {
		UserInfo triggering = triggeringUser();
		UserInfo anon = anonymousUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anon);
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(eq(triggering), any(), eq("syn100")))
				.thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(eq(progressCallback), any(UserInfo.class), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		// Verify the anonymous user — not the triggering user — was passed to both query calls.
		ArgumentCaptor<UserInfo> countUser = ArgumentCaptor.forClass(UserInfo.class);
		verify(tableQueryManager).querySinglePage(eq(progressCallback), countUser.capture(), any(), any());
		assertSame(anon, countUser.getValue());

		ArgumentCaptor<UserInfo> streamUser = ArgumentCaptor.forClass(UserInfo.class);
		verify(tableQueryManager).runQueryAsStream(eq(progressCallback), streamUser.capture(), any(), any(), any());
		assertSame(anon, streamUser.getValue());
	}

	@Test
	public void testHandleCreateOnExceptionRecordsFailedWithErrorMessage() throws Exception {
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new RuntimeException("bad SQL"));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		List<SearchIndexStatus> saved = captor.getAllValues();
		assertEquals(SearchIndexState.CREATING, saved.get(0).getState());
		assertEquals(SearchIndexState.FAILED, saved.get(1).getState());
		assertEquals("bad SQL", saved.get(1).getErrorMessage());
		// Best-effort cleanup after failure
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);
	}

	@Test
	public void testHandleCreateTruncatesLongErrorMessage() throws Exception {
		// A malformed defining-SQL error message can be arbitrarily long (stack-trace-like
		// messages from the table query layer), but the status table column caps at 3000
		// chars. Verify the manager truncates before persisting so the write succeeds.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		String longMessage = "x".repeat(5000);

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new RuntimeException(longMessage));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		SearchIndexStatus failed = captor.getAllValues().get(1);
		assertEquals(SearchIndexState.FAILED, failed.getState());
		assertEquals(3000, failed.getErrorMessage().length(),
				"Error message should be truncated to MAX_ERROR_MESSAGE_LENGTH");
	}

	@Test
	public void testHandleCreateExceedsMaxRowsRecordsFailed() throws Exception {
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(500_001L));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.FAILED, captor.getAllValues().get(1).getState());
		assertNotNull(captor.getAllValues().get(1).getErrorMessage());
		// Stream query never runs past the row-count gate
		verify(tableQueryManager, never()).runQueryAsStream(any(), any(), any(), any(), any());
	}

	@Test
	public void testHandleCreateOnConcurrentDeleteThrowsRecoverable() throws Exception {
		// AOSS rejects deleteIndex when another worker is mid-delete on the same index.
		// The lifecycle manager must translate that into RecoverableMessageException so
		// SQS retries the message — by then the winning delete is done and the retry
		// either no-ops the delete (index_not_found) or proceeds normally.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		ErrorCause cause = ErrorCause.of(b -> b
				.type("status_exception")
				.reason("Deletion failed for indices [search-index-syn456] due to concurrent deletes, please try again"));
		OpenSearchException concurrentDelete = new OpenSearchException(
				ErrorResponse.of(er -> er.error(cause).status(400)));

		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));
		doThrow(concurrentDelete)
				.when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));

		assertSame(concurrentDelete, thrown.getCause());
		assertEquals("Concurrent delete in progress while building search index for entity "
				+ ENTITY_ID, thrown.getMessage());
		// The state row was set CREATING upfront, but no FAILED was recorded — this
		// is transient, not a configuration failure.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		// The pre-build deleteIndex was attempted (it threw); createIndex / row stream never ran.
		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any());
		verify(tableQueryManager, never()).runQueryAsStream(any(), any(), any(), any(), any());
	}

	@Test
	public void testHandleCreateOnTableUnavailablePropagates() throws Exception {
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new TableUnavailableException(new TableStatus()));

		// call under test — TableUnavailableException must propagate so the worker can retry.
		assertThrows(TableUnavailableException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));

		// Only the CREATING status was written — no FAILED record, no cleanup.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		verify(openSearchManager, never()).deleteIndex(any());
	}

	@Test
	public void testHandleCreateOnSourceTableLockUnavailablePropagates() throws Exception {
		// The source table's read lock is unavailable (a writer holds it — e.g. BuildTableIndex
		// is running). LockUnavilableException is thrown bare from querySinglePage with no cause
		// chain. It must propagate out of buildIndex without recording FAILED so the worker can
		// translate it to RecoverableMessageException and SQS retries.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(eq(triggering), any(), eq("syn100"))).thenReturn(Optional.empty());
		LockUnavilableException lockEx = new LockUnavilableException(LockType.Write, "TABLE-LOCK-789", "BuildTableIndex,syn789");
		when(tableQueryManager.querySinglePage(eq(progressCallback), any(UserInfo.class), any(), any()))
				.thenThrow(lockEx);

		// call under test — LockUnavilableException from buildIndex propagates out of the
		// inner multi-catch and is then wrapped by handleCreate's outer catch into
		// RecoverableMessageException so the worker retries rather than recording FAILED.
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));

		assertSame(lockEx, thrown.getCause());
		// Only CREATING was written — no FAILED, no cleanup
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		verify(openSearchManager, never()).deleteIndex(any());
	}

	@Test
	public void testHandleDeleteWithActiveStateDeletesIndexAndStatus() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);
		verify(statusDao).delete(456L);
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteWithFailedStateDeletesIndexAndStatus() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.FAILED));

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);
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
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));

		verify(statusDao, never()).createOrUpdate(any());
		verify(openSearchManager, never()).deleteIndex(any());
		verify(openSearchManager, never()).createIndex(any(), any(), any(), any(), any(), any());
	}

	@Test
	public void testHandleCreateReleasesLockOnSuccess() throws Exception {
		stubBuildLock();
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		verify(writeLock).close();
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.ACTIVE, captor.getAllValues().get(1).getState());
	}

	@Test
	public void testHandleCreateReleasesLockOnFailure() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggeringUser());
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");
		when(entityManager.getEntity(triggeringUser(), ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new RuntimeException("unexpected failure"));

		// call under test — exception is swallowed by the FAILED handler, lock must still be released
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		verify(writeLock).close();
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, times(2)).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.FAILED, captor.getAllValues().get(1).getState());
	}

	@Test
	public void testHandleCreateCallsWaitForIndexWritableBetweenCreateAndRunQuery() throws Exception {
		// AOSS acknowledges createIndex while shards are still not writable; the readiness probe
		// must run before runQueryAsStream so the bulk stream does not race against
		// index_not_found_exception.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));

		// call under test
		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		org.mockito.InOrder order = org.mockito.Mockito.inOrder(openSearchManager, tableQueryManager);
		order.verify(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);
		order.verify(openSearchManager).createIndex(eq("search-index-" + ENTITY_ID),
				any(), any(), any(), any(), any());
		order.verify(openSearchManager).waitForIndexWritable("search-index-" + ENTITY_ID);
		order.verify(tableQueryManager).runQueryAsStream(eq(progressCallback), any(UserInfo.class),
				any(), any(), any());
	}

	@Test
	public void testHandleCreateWhenProbeThrowsRecoverablePropagatesWithoutRecordingFailed() throws Exception {
		// waitForIndexWritable exhausts its retry budget and throws RecoverableMessageException.
		// That must propagate out of buildIndex unchanged and NOT flip the SearchIndex to FAILED —
		// the build will succeed on a later SQS retry.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));
		RecoverableMessageException probeFailed = new RecoverableMessageException(
				"AOSS index search-index-" + ENTITY_ID + " did not accept writes within the retry budget");
		doThrow(probeFailed).when(openSearchManager).waitForIndexWritable("search-index-" + ENTITY_ID);

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));
		assertSame(probeFailed, thrown);

		// Only CREATING was recorded — probe failure is transient, not a permanent failure.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
		// Streaming must not have started — the probe runs first.
		verify(tableQueryManager, never()).runQueryAsStream(any(), any(), any(), any(), any());
	}

	@Test
	public void testHandleCreateUnwrapsConvergenceProbeRecoverableFromIOException() throws Exception {
		// SearchIndexRowHandler.close() tunnels RecoverableMessageException through IOException
		// because RowHandler.close() can't declare anything else. The build path must unwrap
		// before the FAILED branch — convergence-timeout is transient and re-running the
		// SQS message will rebuild from scratch.
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setDefiningSQL(DEFINING_SQL);
		searchIndex.setParentId("syn100");

		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));
		RecoverableMessageException convergenceFailed = new RecoverableMessageException(
				"AOSS index search-index-" + ENTITY_ID + " did not converge to expected count (3 of 5) within the retry budget");
		doThrow(new IOException(convergenceFailed))
				.when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));
		assertSame(convergenceFailed, thrown);

		// Only CREATING was recorded; the index is not flipped to FAILED.
		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao).createOrUpdate(captor.capture());
		assertEquals(SearchIndexState.CREATING, captor.getValue().getState());
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
		// The convergence probe is still called (with 0L); the short-circuit lives inside
		// OpenSearchManagerImpl.waitForDocumentCount so it can no-op without a round-trip,
		// but the row handler doesn't try to second-guess that decision.
		verify(openSearchManager).waitForDocumentCount("test-index", 0L);
	}

	@Test
	public void testRowHandlerCloseProbesDocumentCountWithStreamedRows() throws IOException {
		// AOSS bulk writes acknowledge before documents are visible. After the final flush,
		// close() must wait for _count to converge to the streamed row total before returning,
		// otherwise the lifecycle manager would flip the SearchIndex to ACTIVE while a query
		// would still under-return.
		SelectColumn col = new SelectColumn();
		col.setId("100");
		col.setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), openSearchManager);

		for (long i = 1; i <= 3; i++) {
			Row row = new Row();
			row.setRowId(i);
			row.setVersionNumber(1L);
			row.setValues(Collections.singletonList("v" + i));
			handler.nextRow(row);
		}

		// call under test
		handler.close();

		org.mockito.InOrder order = org.mockito.Mockito.inOrder(openSearchManager);
		order.verify(openSearchManager).bulkIndex(eq("test-index"), any());
		order.verify(openSearchManager).waitForDocumentCount("test-index", 3L);
	}

	@Test
	public void testRowHandlerCloseTunnelsRecoverableThroughIOException() throws Exception {
		// A convergence-probe timeout returns a RecoverableMessageException. RowHandler.close()
		// can only declare IOException, so the recoverable signal is wrapped — the lifecycle
		// build path unwraps before its FAILED branch.
		SelectColumn col = new SelectColumn();
		col.setId("100");
		col.setColumnType(ColumnType.STRING);
		SearchIndexRowHandler handler = new SearchIndexRowHandler(
				"test-index", Collections.singletonList(col), openSearchManager);

		Row row = new Row();
		row.setRowId(1L);
		row.setVersionNumber(1L);
		row.setValues(Collections.singletonList("v"));
		handler.nextRow(row);

		RecoverableMessageException probeFailed = new RecoverableMessageException(
				"AOSS index test-index did not converge to expected count (0 of 1) within the retry budget");
		doThrow(probeFailed).when(openSearchManager).waitForDocumentCount("test-index", 1L);

		// call under test
		IOException thrown = assertThrows(IOException.class, handler::close);
		assertSame(probeFailed, thrown.getCause());
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
		verifyZeroInteractions(synonymSetDao);
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

	@SuppressWarnings("unchecked")
	private static List<String> anyList() {
		return org.mockito.ArgumentMatchers.anyList();
	}

	/** Stub the minimum chain that lets buildIndex reach the row-stream phase. */
	private void stubHappyPathThroughCreateIndex() throws Exception {
		stubBuildLock();
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));
	}

	// -------- buildIndex — additional branch coverage --------

	@Test
	public void testHandleCreateThrowsWhenSchemaIsNull() throws Exception {
		// L235: getTableSchema returns null — buildIndex throws "no bound schema", caught
		// by the outer Throwable handler and recorded as FAILED.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggeringUser());
		when(entityManager.getEntity(any(), any(), any()))
				.thenReturn(new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100"));
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID))).thenReturn(null);

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED
						&& s.getErrorMessage() != null
						&& s.getErrorMessage().contains("no bound schema")));
	}

	@Test
	public void testHandleCreateThrowsWhenSchemaIsEmpty() throws Exception {
		// L235: empty schema also flows to FAILED via the outer Throwable handler.
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggeringUser());
		when(entityManager.getEntity(any(), any(), any()))
				.thenReturn(new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100"));
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.emptyList());

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
	}

	@Test
	public void testHandleCreateRejectsRowCountAboveMax() throws Exception {
		// L257: rowCount > MAX_ROWS — IllegalStateException is caught by outer handler and
		// the index is marked FAILED with the row-count message.
		stubHappyPathThroughCreateIndex();
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(1_000_000L));

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED
						&& s.getErrorMessage() != null
						&& s.getErrorMessage().contains("exceed maximum")));
	}

	@Test
	public void testHandleCreateAcceptsNullRowCount() throws Exception {
		// L257: rowCount == null — short-circuits the > MAX_ROWS guard and proceeds.
		stubHappyPathThroughCreateIndex();
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle()); // no queryCount set

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		verify(openSearchManager).createIndex(any(), any(), any(), any(), any(), any());
	}

	@Test
	public void testHandleCreateWithConfigSetsDefaultAnalyzer() throws Exception {
		// L265: config != null branch — readRef extracts the qname and forwards it to createIndex.
		stubHappyPathThroughCreateIndex();
		String defaultQname = "org.sagebionetworks-SCIENTIFIC";
		SearchConfiguration config = new SearchConfiguration()
				.setDefaultAnalyzer(new org.json.JSONObject().put("$ref", defaultQname));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.of(config));
		when(textAnalyzerDao.getByQualifiedNames(anyList())).thenReturn(
				Collections.singletonMap(defaultQname,
						new TextAnalyzer().setName("SCIENTIFIC").setSettings(
								new org.json.JSONObject().put("analyzer",
										new org.json.JSONObject().put("default",
												new org.json.JSONObject().put("type", "custom")
														.put("tokenizer", "standard"))))));

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		verify(openSearchManager).createIndex(any(), any(), eq(defaultQname), any(), any(), any());
	}

	@Test
	public void testHandleCreateWithIOExceptionWithoutRecoverableCauseMarksFailed() throws Exception {
		// L311: instanceof IOException but cause is NOT RecoverableMessageException — falls
		// through to the FAILED-marking path. The IOException itself is swallowed.
		stubHappyPathThroughCreateIndex();
		doThrow(new IOException("disk full"))
				.when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
	}

	@Test
	public void testHandleCreateWithOpenSearchExceptionNotConcurrentDeleteMarksFailed() throws Exception {
		// L319: OpenSearchException that ISN'T a concurrent-delete falls through to the
		// FAILED-marking path.
		stubHappyPathThroughCreateIndex();
		org.opensearch.client.opensearch._types.OpenSearchException opensearchEx =
				new org.opensearch.client.opensearch._types.OpenSearchException(
						new org.opensearch.client.opensearch._types.ErrorResponse.Builder()
								.status(500)
								.error(new org.opensearch.client.opensearch._types.ErrorCause.Builder()
										.type("internal_server_error")
										.reason("not a concurrent delete")
										.build())
								.build());
		doThrow(opensearchEx)
				.when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

		ArgumentCaptor<SearchIndexStatus> captor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(captor.capture());
		assertTrue(captor.getAllValues().stream()
				.anyMatch(s -> s.getState() == SearchIndexState.FAILED));
	}

	@Test
	public void testHandleCreateWithNullErrorMessageStillMarksFailed() throws Exception {
		// L335: e.getMessage() == null — truncate guard short-circuits cleanly and the
		// FAILED status carries a null errorMessage.
		stubHappyPathThroughCreateIndex();
		doThrow(new RuntimeException((String) null))
				.when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		manager.handleCreate(progressCallback, ENTITY_ID, USER_ID);

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
	public void testHandleDeleteWhenDeleteIndexThrowsLogsAndContinues() throws Exception {
		// deleteIndex failure under the lock must not leak — handleDelete swallows the
		// throwable, logs, and the status row is left in place (since the delete failed,
		// statusDao.delete is never called).
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.of(SearchIndexState.ACTIVE));
		doThrow(new RuntimeException("AOSS unavailable"))
				.when(openSearchManager).deleteIndex("search-index-" + ENTITY_ID);

		// call under test — must not throw
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(statusDao, never()).delete(any());
		verify(writeLock).close();
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

		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager, never()).deleteIndex(any());
		verify(statusDao, never()).delete(any());
		verify(writeLock).close();
	}

	// -------- buildIndex error-unwrap branches --------

	@Test
	public void testHandleCreateUnwrapsIOExceptionWrappingRecoverableMessageException() throws Exception {
		// SearchIndexRowHandler tunnels RecoverableMessageException through IOException because
		// RowHandler.close() can't declare anything else. handleCreate must unwrap to keep the
		// retry instead of permanently FAILING the index.
		stubBuildLock();
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");

		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));
		// Stream throws IOException wrapping a RecoverableMessageException — handleCreate
		// must rethrow the inner cause.
		IOException tunnelled = new IOException("io",
				new RecoverableMessageException("convergence probe timed out"));
		doThrow(tunnelled).when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));

		// Status must NOT be marked FAILED — this is transient. Only the initial CREATING
		// status was written (no second createOrUpdate for FAILED).
		ArgumentCaptor<SearchIndexStatus> statusCaptor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(statusCaptor.capture());
		assertTrue(statusCaptor.getAllValues().stream()
				.noneMatch(s -> s.getState() == SearchIndexState.FAILED),
				"transient errors must not mark the index FAILED");
	}

	@Test
	public void testHandleCreateUnwrapsLockUnavailableNestedInsideAnotherException() throws Exception {
		// LockUnavilableException wrapped inside another exception is still a transient
		// writer-contention signal — surface the wrapped lock exception so the worker re-queues.
		stubBuildLock();
		UserInfo triggering = triggeringUser();
		SearchIndex searchIndex = new SearchIndex().setDefiningSQL(DEFINING_SQL).setParentId("syn100");

		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(userManager.getUserInfo(USER_ID)).thenReturn(triggering);
		when(userManager.getUserInfo(ANON_ID)).thenReturn(anonymousUser());
		when(entityManager.getEntity(triggering, ENTITY_ID, SearchIndex.class)).thenReturn(searchIndex);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(ENTITY_ID)))
				.thenReturn(Collections.singletonList(
						new ColumnModel().setId("100").setName("name").setColumnType(ColumnType.STRING)));
		when(searchConfigurationResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
		when(tableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenReturn(new QueryResultBundle().setQueryCount(0L));

		LockUnavilableException nestedLock = new LockUnavilableException(
				org.sagebionetworks.workers.util.semaphore.LockType.Write, "k", "other-worker");
		RuntimeException wrapper = new RuntimeException("wrapped", nestedLock);
		doThrow(wrapper).when(tableQueryManager).runQueryAsStream(any(), any(), any(), any(), any());

		// buildIndex's inner cause-cause unwrap surfaces the LockUnavilableException; the
		// outer handleCreate wrapper then converts it to RecoverableMessageException. The
		// branch under test is the LockUnavilableException unwrap path inside buildIndex —
		// the resulting RecoverableMessageException's cause must be the original lock ex.
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> manager.handleCreate(progressCallback, ENTITY_ID, USER_ID));
		assertTrue(thrown.getCause() instanceof LockUnavilableException,
				"the recoverable wrapper must carry the LockUnavilableException as its cause");

		// Not marked FAILED — transient.
		ArgumentCaptor<SearchIndexStatus> nestedLockCaptor = ArgumentCaptor.forClass(SearchIndexStatus.class);
		verify(statusDao, atLeastOnce()).createOrUpdate(nestedLockCaptor.capture());
		assertTrue(nestedLockCaptor.getAllValues().stream()
				.noneMatch(s -> s.getState() == SearchIndexState.FAILED),
				"nested lock-unavailable must not mark the index FAILED");
	}

}
