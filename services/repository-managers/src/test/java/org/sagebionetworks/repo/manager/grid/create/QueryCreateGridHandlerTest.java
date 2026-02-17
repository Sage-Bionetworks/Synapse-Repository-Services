package org.sagebionetworks.repo.manager.grid.create;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.grid.SnapshotRowHandler;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.query.MainQuery;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

@ExtendWith(MockitoExtension.class)
public class QueryCreateGridHandlerTest {
	
	@Mock
	private GridDao mockGridDao;

	@Mock
	private UserInfo mockUser;
	
	@Mock
	private UserInfo mockSessionOwnerUser;

	@Mock
	private TableQueryManager mockQueryManager;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@Mock
	private QueryTranslations mockQueryTranslattion;
	@Mock
	private MainQuery mockMainQuery;
	@Mock
	private QueryTranslator mockTranslator;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private JsonSchemaManager mockSchemaManager;
	@Mock
	private GridAuthorizationManager mockGridAuthorizationManager;
	
	@Mock
	private SnapshotStore mockSnapshotStore;
	@Mock
	private FileProvider mockFileProvider;
	@Captor
	private ArgumentCaptor<RowHandlerProvider> rowHandlerProviderCaptor;

	@Spy
	@InjectMocks
	QueryCreateGridHandler handler;

	@TempDir
	File tempDir;
	private File tempFile;

	private static final String mockStackName = "test";
	private Long userId;
	private String gridSessionId;
	private Long gridSessionIdLong;
	private boolean isAgent;

	private Long replicaId;
	private GridReplica replica;

	private Query query;
	private String tableId;
	private List<Row> rows;
	private List<SelectColumn> tableColumnSchema;
	private QueryResult queryResults;
	private QueryResultBundle queryResultBundle;
	private String schema$id;
	private QueryOptions queryOptions;
	private Long maxRowsPerPage;

	private JsonSchemaObjectBinding schemaBinding;

	@BeforeEach
	public void before() throws IOException {
		userId = 123L;
		gridSessionIdLong = 456L;
		gridSessionId = GridUtils.gridSessionIdAsString(gridSessionIdLong);
		isAgent = false;
		replicaId = 88L;
		replica = new GridReplica().setReplicaId(replicaId);
		query = new Query().setSql("select * from syn123").setIncludeEntityEtag(false);
		tableId = "syn999";
		rows = List.of(new Row().setRowId(10101L));
		maxRowsPerPage = 78L;
		queryResults = new QueryResult().setQueryResults(new RowSet().setTableId(tableId).setRows(rows));
		tableColumnSchema = List.of(new SelectColumn().setName("foo").setColumnType(ColumnType.INTEGER));
		queryResultBundle = new QueryResultBundle().setQueryResult(queryResults).setMaxRowsPerPage(maxRowsPerPage).setSelectColumns(tableColumnSchema);
		schema$id = "someorg-somename";
		queryOptions = new QueryOptions().withReturnMaxRowsPerPage(true).withRunQuery(true)
				.withReturnSelectColumns(true);

		schemaBinding = new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(schema$id));
		// Create a real temp file for testing
		tempFile = new File(tempDir, "snapshot-test.cbor");
	}
	
	@Test
	public void testCanCreate() {
		assertFalse(handler.canCreate(new CreateGridRequest()));
		assertFalse(handler.canCreate(new CreateGridRequest().setRecordSetId("syn123")));
		assertTrue(handler.canCreate(new CreateGridRequest().setInitialQuery(new Query())));
	}

	@Test
	public void testBuildSessionFromQuery() throws Exception {
		
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, gridSessionId)).thenReturn(mockSessionOwnerUser);
		when(mockUser.getId()).thenReturn(userId);
		when(mockQueryManager.querySinglePage(mockCallback, mockUser, new Query().setSql(query.getSql()).setLimit(1L),
				queryOptions)).thenReturn(queryResultBundle);
		when(mockQueryManager.runQueryAsStream(eq(mockCallback), eq(mockSessionOwnerUser), eq(query),
				rowHandlerProviderCaptor.capture(), eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE))).thenReturn(new QueryResultBundle());
		doReturn(Optional.of(schema$id)).when(handler).getSchemaId(mockUser, tableId, rows);
		when(mockSchemaManager.getValidationSchema(schema$id)).thenReturn(new JsonSchema().setRequired(List.of("foo")));
		when(mockFileProvider.createTempFile("snapshot", ".cbor")).thenReturn(tempFile);

		GridSession expected = new GridSession().setSessionId(gridSessionId);
		when(mockGridDao.createGridSession(
				new CreateGridSession().setUserId(userId).setSourceId(tableId).setSchemaId(schema$id)))
				.thenReturn(expected);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);
		when(mockQueryManager.getMaxBytesPerRequest()).thenReturn(2_000_000L);

		// call under test
		handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setInitialQuery(query), mockSnapshotStore);
		assertTrue(query.getIncludeEntityEtag()); // verify that the query is mutated to include etag
		RowHandlerProvider rp = rowHandlerProviderCaptor.getValue();
		when(mockQueryTranslattion.getMainQuery()).thenReturn(mockMainQuery);
		when(mockMainQuery.getTranslator()).thenReturn(mockTranslator);
		List<ColumnModel> schema = List.of(new ColumnModel().setColumnType(ColumnType.INTEGER).setName("foo"));
		when(mockTranslator.getSchemaOfSelect()).thenReturn(schema);
		SnapshotRowHandler snapshotHandler = (SnapshotRowHandler) rp.getHandler(mockQueryTranslattion);
		snapshotHandler.close();

		verify(mockSnapshotStore).saveSnapshot(eq(gridSessionId), any(), eq(userId), eq(tempFile));
	}

	@Test
	public void testBuildSessionFromQueryWithNoSchema() throws Exception {
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, gridSessionId)).thenReturn(mockSessionOwnerUser);
		when(mockUser.getId()).thenReturn(userId);
		when(mockQueryManager.querySinglePage(mockCallback, mockUser, new Query().setSql(query.getSql()).setLimit(1L),
				queryOptions)).thenReturn(queryResultBundle);
		when(mockQueryManager.runQueryAsStream(eq(mockCallback), eq(mockSessionOwnerUser), eq(query),
				rowHandlerProviderCaptor.capture(), eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE))).thenReturn(new QueryResultBundle());
		doReturn(Optional.empty()).when(handler).getSchemaId(mockUser, tableId, rows);
		when(mockFileProvider.createTempFile("snapshot", ".cbor")).thenReturn(tempFile);

		GridSession expected = new GridSession().setSessionId(gridSessionId);
		when(mockGridDao
				.createGridSession(new CreateGridSession().setUserId(userId).setSourceId(tableId).setSchemaId(null)))
				.thenReturn(expected);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);
		when(mockQueryManager.getMaxBytesPerRequest()).thenReturn(2_000_000L);

		// call under test
		handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setInitialQuery(query), mockSnapshotStore);
		RowHandlerProvider rp = rowHandlerProviderCaptor.getValue();
		when(mockQueryTranslattion.getMainQuery()).thenReturn(mockMainQuery);
		when(mockMainQuery.getTranslator()).thenReturn(mockTranslator);
		List<ColumnModel> schema = List.of(new ColumnModel().setColumnType(ColumnType.INTEGER).setName("foo"));
		when(mockTranslator.getSchemaOfSelect()).thenReturn(schema);
		SnapshotRowHandler snapshotHandler = (SnapshotRowHandler) rp.getHandler(mockQueryTranslattion);
		snapshotHandler.close();
		verify(mockSnapshotStore).saveSnapshot(eq(gridSessionId), any(), eq(userId), eq(tempFile));
	}

	@Test
	public void testBuildSessionFromQueryWithLockUnavilableException() throws Exception {
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, gridSessionId)).thenReturn(mockSessionOwnerUser);
		when(mockUser.getId()).thenReturn(userId);
		GridSession expected = new GridSession().setSessionId(gridSessionId);
		when(mockGridDao.createGridSession(
				new CreateGridSession().setUserId(userId).setSourceId(tableId).setSchemaId(schema$id)))
				.thenReturn(expected);
		doReturn(Optional.of(schema$id)).when(handler).getSchemaId(mockUser, tableId, rows);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);
		LockUnavilableException e = new LockUnavilableException(LockType.Read, "key", "context");
		when(mockQueryManager.querySinglePage(mockCallback, mockUser, new Query().setSql(query.getSql()).setLimit(1L),
				queryOptions)).thenReturn(queryResultBundle);
		when(mockQueryManager.runQueryAsStream(eq(mockCallback), eq(mockSessionOwnerUser), eq(query), any(), eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE))).thenThrow(e);

		String message = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setInitialQuery(query), mockSnapshotStore);
		}).getMessage();
		assertEquals(
				"org.sagebionetworks.workers.util.semaphore.LockUnavilableException: Read lock unavailable for key: 'key'. Current lock holder's context: 'context'",
				message);
		verify(mockCallback).updateProgress("Waiting for table/view to become available...", 1L, 100L);
	}

	@Test
	public void testBuildSessionFromQueryWithTableUnavailableException() throws Exception {
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, gridSessionId)).thenReturn(mockSessionOwnerUser);
		when(mockUser.getId()).thenReturn(userId);
		GridSession expected = new GridSession().setSessionId(gridSessionId);
		when(mockGridDao.createGridSession(
				new CreateGridSession().setUserId(userId).setSourceId(tableId).setSchemaId(schema$id)))
				.thenReturn(expected);
		doReturn(Optional.of(schema$id)).when(handler).getSchemaId(mockUser, tableId, rows);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);
		TableUnavailableException e = new TableUnavailableException(new TableStatus().setTableId("syn123"));
		when(mockQueryManager.querySinglePage(mockCallback, mockUser, new Query().setSql(query.getSql()).setLimit(1L),
				queryOptions)).thenReturn(queryResultBundle);
		when(mockQueryManager.runQueryAsStream(eq(mockCallback), eq(mockSessionOwnerUser), eq(query), any(), eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE))).thenThrow(e);

		String message = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setInitialQuery(query), mockSnapshotStore);
		}).getMessage();
		assertEquals("org.sagebionetworks.repo.model.table.TableUnavailableException", message);
		verify(mockCallback).updateProgress("Waiting for table/view to become available...", 1L, 100L);
	}

	@Test
	public void testBuildSessionFromQueryWithOhterException() throws Exception {
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, gridSessionId)).thenReturn(mockSessionOwnerUser);
		when(mockUser.getId()).thenReturn(userId);
		GridSession expected = new GridSession().setSessionId(gridSessionId);
		when(mockGridDao.createGridSession(
				new CreateGridSession().setUserId(userId).setSourceId(tableId).setSchemaId(schema$id)))
				.thenReturn(expected);
		doReturn(Optional.of(schema$id)).when(handler).getSchemaId(mockUser, tableId, rows);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);
		IOException e = new IOException("not connected");
		when(mockQueryManager.querySinglePage(mockCallback, mockUser, new Query().setSql(query.getSql()).setLimit(1L),
				queryOptions)).thenReturn(queryResultBundle);
		when(mockQueryManager.runQueryAsStream(eq(mockCallback), eq(mockSessionOwnerUser), eq(query), any(), eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE))).thenThrow(e);

		String message = assertThrows(RuntimeException.class, () -> {
			// call under test
			handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setInitialQuery(query), mockSnapshotStore);
		}).getMessage();
		assertEquals("java.io.IOException: not connected", message);
		verify(mockCallback, never()).updateProgress(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testGetMaxRowSizeBytesWithOneRow() {
		long maxRowsPerPage = 1L;
		// call under test
		assertEquals(Long.MAX_VALUE, handler.getMaxRowSizeBytes(maxRowsPerPage));
		verifyZeroInteractions(mockQueryManager);
	}

	@Test
	public void testGetMaxRowSizeBytesWithLessThandOneRow() {
		long maxRowsPerPage = 0L;
		// call under test
		assertEquals(Long.MAX_VALUE, handler.getMaxRowSizeBytes(maxRowsPerPage));
		verifyZeroInteractions(mockQueryManager);
	}

	@Test
	public void testGetMaxRowSizeBytes() {
		when(mockQueryManager.getMaxBytesPerRequest()).thenReturn(2_000_000L);
		long maxRowsPerPage = 101L;
		// call under test
		assertEquals(2_000_000L / maxRowsPerPage, handler.getMaxRowSizeBytes(maxRowsPerPage));
	}

	@Test
	public void testGcheSchemaId() {
		when(mockEntityManager.getEntityType(tableId)).thenReturn(EntityType.entityview);
		when(mockEntityManager.getBoundSchema(mockUser, "syn10101")).thenReturn(schemaBinding);

		// call under test
		assertEquals(Optional.of(schema$id), handler.getSchemaId(mockUser, tableId, rows));
		verifyNoMoreInteractions(mockEntityManager);
	}

	@Test
	public void testGcheSchemaIdWithNonview() {
		when(mockEntityManager.getEntityType(tableId)).thenReturn(EntityType.folder);

		// call under test
		assertEquals(Optional.empty(), handler.getSchemaId(mockUser, tableId, rows));
		verifyNoMoreInteractions(mockEntityManager);
	}

	@Test
	public void testGcheSchemaIdWithNotFound() {
		when(mockEntityManager.getEntityType(tableId)).thenReturn(EntityType.entityview);
		when(mockEntityManager.getBoundSchema(mockUser, "syn10101")).thenThrow(new NotFoundException("not here"));

		// call under test
		assertEquals(Optional.empty(), handler.getSchemaId(mockUser, tableId, rows));
		verifyNoMoreInteractions(mockEntityManager);
	}

	@Test
	public void testGcheSchemaIdWithEmptyRows() {
		when(mockEntityManager.getEntityType(tableId)).thenReturn(EntityType.entityview);
		rows = List.of();

		// call under test
		assertEquals(Optional.empty(), handler.getSchemaId(mockUser, tableId, rows));
		verifyNoMoreInteractions(mockEntityManager);
	}

	@Test
	public void testGcheSchemaIdWithNullRows() {
		when(mockEntityManager.getEntityType(tableId)).thenReturn(EntityType.entityview);
		rows = null;

		// call under test
		assertEquals(Optional.empty(), handler.getSchemaId(mockUser, tableId, rows));
		verifyNoMoreInteractions(mockEntityManager);
	}
}
