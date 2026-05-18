package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
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
		verify(statusDao, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
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
		verify(statusDao, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
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
		verify(statusDao, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
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
	public void testHandleDeleteWithMissingStatusIsNoOp() throws Exception {
		stubBuildLock();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(statusDao);
		when(statusDao.getState(456L)).thenReturn(Optional.empty());

		// call under test
		manager.handleDelete(progressCallback, ENTITY_ID);

		verify(openSearchManager, never()).deleteIndex(any());
		verify(statusDao, never()).delete(any());
		verify(writeLock).close();
	}

	@Test
	public void testHandleDeleteWithLockAlreadyHeld() throws Exception {
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
		verify(statusDao, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
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
		verify(statusDao, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
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
		SearchIndexLifecycleManagerImpl.SearchIndexRowHandler handler =
				new SearchIndexLifecycleManagerImpl.SearchIndexRowHandler("test-index", columns, openSearchManager);

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
		SearchIndexLifecycleManagerImpl.SearchIndexRowHandler handler =
				new SearchIndexLifecycleManagerImpl.SearchIndexRowHandler("test-index", columns, openSearchManager);

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
		SearchIndexLifecycleManagerImpl.SearchIndexRowHandler handler =
				new SearchIndexLifecycleManagerImpl.SearchIndexRowHandler("test-index", Collections.singletonList(col), openSearchManager);

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
	public void testRowHandlerCloseWithEmptyBatchIsNoOp() throws IOException {
		SelectColumn col = new SelectColumn();
		col.setId("100");
		SearchIndexLifecycleManagerImpl.SearchIndexRowHandler handler =
				new SearchIndexLifecycleManagerImpl.SearchIndexRowHandler("test-index", Collections.singletonList(col), openSearchManager);

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
		SearchIndexLifecycleManagerImpl.SearchIndexRowHandler handler =
				new SearchIndexLifecycleManagerImpl.SearchIndexRowHandler("test-index", columns, openSearchManager);

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
}
