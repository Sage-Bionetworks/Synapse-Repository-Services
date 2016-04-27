package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_COUNT;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_RESULTS;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;

public class TableQueryManagerImplTest {
	
	@Mock
	ColumnModelDAO mockColumnModelDAO;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	ConnectionFactory mockTableConnectionFactory;
	@Mock
	TableIndexDAO mockTableIndexDAO;
	@Mock
	ProgressCallback<Void> mockProgressCallbackVoid;
	@Mock
	ProgressCallback<Object> mockProgressCallback2;
	
	TableQueryManagerImpl manager;
	
	List<ColumnModel> models;
	UserInfo user;
	String tableId;
	RowSet set;
	TableStatus status;
	String ETAG;
	int maxBytesPerRequest;
	
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		manager = new TableQueryManagerImpl();
		ReflectionTestUtils.setField(manager, "columnModelDAO", mockColumnModelDAO);
		ReflectionTestUtils.setField(manager, "tableManagerSupport", mockTableManagerSupport);
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockTableConnectionFactory);
		
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		
		tableId = "syn123";
		user = new UserInfo(false, 7L);
		
		status = new TableStatus();
		status.setTableId(tableId);
		status.setState(TableState.AVAILABLE);
		status.setChangedOn(new Date(123));
		status.setLastTableChangeEtag("etag");
		ETAG = "";
		
		models = TableModelTestUtils.createOneOfEachType(true);
		
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		
		// Just call the caller.
		stub(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).toAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if(invocation == null) return null;
				ProgressingCallable<Object, Object> callable = (ProgressingCallable<Object, Object>) invocation.getArguments()[3];
						if (callable != null) {
							return callable.call(mockProgressCallback2);
						} else {
							return null;
						}
			}
		});
		
		maxBytesPerRequest = 10000000;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		
		List<Row> rows = TableModelTestUtils.createRows(models, 10);
		set = new RowSet();
		set.setTableId(tableId);
		set.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		set.setRows(rows);
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.query(any(ProgressCallback.class),any(SqlQuery.class))).thenReturn(set);
		stub(mockTableIndexDAO.queryAsStream(any(ProgressCallback.class),any(SqlQuery.class), any(RowAndHeaderHandler.class))).toAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				SqlQuery query = (SqlQuery) invocation.getArguments()[1];
				RowAndHeaderHandler handler =  (RowAndHeaderHandler) invocation.getArguments()[2];
				boolean isCount = false;
				if (query.getModel().getSelectList().getColumns() != null) {
					String sql = query.getModel().getSelectList().getColumns().get(0).toString();
					if (sql.equals("COUNT(*)")) {
						isCount = true;
					} else if (sql.contains("FOUND_ROWS()")) {
						isCount = true;
					}
				}
				if (isCount) {
					handler.writeHeader();
					handler.nextRow(TableModelTestUtils.createRow(null, null, "10"));
				} else {
					// Pass all rows to the handler
					handler.writeHeader();
					for (Row row : set.getRows()) {
						handler.nextRow(row);
					}
				}
				return true;
			}
		});	
		// Just call the caller.
		stub(mockTableIndexDAO.executeInReadTransaction(any(TransactionCallback.class))).toAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				if (invocation == null)
					return null;
				TransactionCallback<Void> callable = (TransactionCallback<Void>) invocation.getArguments()[0];
				return callable.doInTransaction(null);
			}
		});
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryUnauthroized() throws Exception {
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, null, null, true, false, true);
	}
	
	@Test 
	public void testQueryHappyCaseIsConsistentFalse() throws Exception {
		RowSet expected = new RowSet();
		expected.setTableId(tableId);
		when(mockTableIndexDAO.query(any(ProgressCallback.class),any(SqlQuery.class))).thenReturn(expected);
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, false);
		// The etag should be null for this case
		assertEquals("The etag must be null for non-consistent query results.  These results cannot be used for a table update.", null,
				results.getFirst().getQueryResults().getEtag());
		assertEquals(expected, results.getFirst().getQueryResults());
		// The table status should not be checked for this case
		verify(mockTableManagerSupport, never()).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryHappyCaseIsConsistentTrue() throws Exception {
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryCountHappyCaseIsConsistentTrue() throws Exception {
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, true, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
		assertEquals(1L, results.getSecond().longValue());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test
	public void testQueryAndCountHappyCaseIsConsistentTrue() throws Exception {
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getFirst().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getFirst().getQueryResults().setEtag(null);
		assertEquals(set, results.getFirst().getQueryResults());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test 
	public void testQueryNoColumns() throws Exception {
		// Return no columns
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		Pair<QueryResult, Long> results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		assertNotNull(results);
		assertEquals(tableId, results.getFirst().getQueryResults().getTableId());
		assertNull(results.getFirst().getQueryResults().getEtag());
		assertNull(results.getFirst().getQueryResults().getHeaders());
		assertNull(results.getFirst().getQueryResults().getRows());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	/**
	 * Test for a consistent query when the table index is not available.
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotAvailable() throws Exception {
		status.setState(TableState.PROCESSING);
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableManagerSupport, times(1)).getTableStatusOrCreateIfNotExists(tableId);
	}
	
	/**
	 * Test for a consistent query when the table index is not available and not yet being build
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotFound() throws Exception {
		status.setState(TableState.PROCESSING);
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableManagerSupport, times(1)).getTableStatusOrCreateIfNotExists(tableId);
	}

	/**
	 * Test for a consistent query when the table index worker is holding a write-lock-precursor on the index. For this
	 * case the tryRunWithSharedLock() will throw a LockUnavilableException(), which should then be translated into a
	 * TableUnavilableException that contains the Table's status.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testQueryIsConsistentTrueLockUnavilableException() throws Exception {
		// Throw a lock LockUnavilableException
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),anyString(), anyInt(), any(ProgressingCallable.class))).thenThrow(new LockUnavilableException());
		try{
			manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavilableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
	}

	@Test
	public void testQueryBundle() throws Exception {
		RowSet selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select * from " + tableId, selectStar, 10L, TableModelUtils.createColumnModelColumnMapper(models, false)
				.getSelectColumns().toString(), 2929L);
	}

	@Test
	public void testQueryBundleColumnsExpanded() throws Exception {
		RowSet selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select " + StringUtils.join(Lists.transform(models, TableModelTestUtils.convertToNameFunction), ",") + " from "
				+ tableId, selectStar, 10L, TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns().toString(),
				2929L);
	}

	@Test
	public void testQueryBundleWithAggregate() throws Exception {
		RowSet totals = new RowSet();
		totals.setEtag("etag");
		totals.setHeaders(Lists.newArrayList(TableModelTestUtils.createSelectColumn(null, "COUNT(*)", ColumnType.INTEGER)));
		totals.setTableId(tableId);
		totals.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "10")));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(totals);

		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("COUNT(*)");
		selectColumn.setColumnType(ColumnType.INTEGER);
		runQueryBundleTest("select count(*) from " + tableId, totals, 10L, "[" + selectColumn.toString() + "]", 500000L);
	}

	private void runQueryBundleTest(String sql, RowSet selectResult, Long countResult, String selectColumns, Long maxRowsPerPage)
			throws Exception {
		Query query = new Query();
		query.setSql(sql);
		query.setIsConsistent(true);
		query.setOffset(0L);
		query.setLimit(Long.MAX_VALUE);
		QueryBundleRequest queryBundle = new QueryBundleRequest();
		queryBundle.setQuery(query);

		// Request query only
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_RESULTS);
		QueryResultBundle bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(selectResult, bundle.getQueryResult().getQueryResults());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// Count only
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_COUNT);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(countResult, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// select columns
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_SELECT_COLUMNS);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns().toString());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// max rows per page
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());

		// now combine them all
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_RESULTS | BUNDLE_MASK_QUERY_COUNT
				| BUNDLE_MASK_QUERY_SELECT_COLUMNS | BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(selectResult, bundle.getQueryResult().getQueryResults());
		assertEquals(countResult, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns().toString());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
	}
	
	@Test
	public void testGetMaxRowsPerPage(){
		Long maxRows = this.manager.getMaxRowsPerPage(TableModelUtils.createColumnModelColumnMapper(models, false));
		int maxRowSize = TableModelUtils.calculateMaxRowSize(TableModelUtils.createColumnModelColumnMapper(models, false).getColumnModels());
		Long expected = (long) (this.maxBytesPerRequest/maxRowSize);
		assertEquals(expected, maxRows);
	}
	
	@Test
	public void testGetMaxRowsPerPageEmpty(){
		Long maxRows = this.manager.getMaxRowsPerPage(TableModelUtils.createColumnModelColumnMapper(new LinkedList<ColumnModel>(), false));
		assertEquals(null, maxRows);
	}
	
	@Test
	public void testNextPageToken() throws Exception {
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		Pair<QueryResult, Long> query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test
	public void testNextPageTokenEscaping() throws Exception {
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		// introduce escape-needed column names
		for (int i = 0; i < models.size(); i++) {
			String name = models.get(i).getName();
			name = name.substring(0, 1) + "-" + name.substring(1);
			models.get(i).setName(name);
		}

		Pair<QueryResult, Long> query = manager.query(mockProgressCallbackVoid, user, "select \"i-0\" from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
		assertTrue(query.getFirst().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);

		query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getFirst().getNextPageToken());
		assertTrue(query.getFirst().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);
		verify(mockTableManagerSupport, times(2)).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testValidateTableIsAvailableWithStateAvailable() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.AVAILABLE);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// call under test
		TableStatus resultsStatus = manager.validateTableIsAvailable(tableId);
		assertNotNull(resultsStatus);
		assertEquals(status, resultsStatus);
	}
	
	@Test (expected=TableUnavilableException.class)
	public void testValidateTableIsAvailableWithStateProcessing() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.PROCESSING);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// call under test
		manager.validateTableIsAvailable(tableId);
	}
	
	@Test (expected=TableFailedException.class)
	public void testValidateTableIsAvailableWithStateFailed() throws NotFoundException, TableUnavilableException, TableFailedException{
		status.setState(TableState.PROCESSING_FAILED);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// call under test
		manager.validateTableIsAvailable(tableId);
	}
}
