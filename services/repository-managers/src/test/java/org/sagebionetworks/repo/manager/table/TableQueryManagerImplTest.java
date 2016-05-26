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
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.csv.CSVWriterStream;
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
	CSVWriterStream writer;
	List<String[]> writtenLines;
	
	
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
		set.setHeaders(TableModelUtils.getSelectColumns(models));
		set.setRows(rows);
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		when(mockTableIndexDAO.query(any(ProgressCallback.class),any(SqlQuery.class))).thenReturn(set);
		stub(mockTableIndexDAO.queryAsStream(any(ProgressCallback.class),any(SqlQuery.class), any(RowHandler.class))).toAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				SqlQuery query = (SqlQuery) invocation.getArguments()[1];
				RowHandler handler =  (RowHandler) invocation.getArguments()[2];
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
					handler.nextRow(TableModelTestUtils.createRow(null, null, "10"));
				} else {
					// Pass all rows to the handler
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
		
		// Writer that captures lines
		writtenLines = new LinkedList<String[]>();
		writer = new CSVWriterStream(){

			@Override
			public void writeNext(String[] nextLine) {
				writtenLines.add(nextLine);
			}};
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
		QueryResultWithCount results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, false);
		// The etag should be null for this case
		assertEquals("The etag must be null for non-consistent query results.  These results cannot be used for a table update.", null,
				results.getQueryResult().getQueryResults().getEtag());
		assertEquals(expected, results.getQueryResult().getQueryResults());
		// The table status should not be checked for this case
		verify(mockTableManagerSupport, never()).setTableToProcessingAndTriggerUpdate(tableId);
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryHappyCaseIsConsistentTrue() throws Exception {
		QueryResultWithCount results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getQueryResult().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getQueryResult().getQueryResults().setEtag(null);
		assertEquals(set, results.getQueryResult().getQueryResults());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryCountHappyCaseIsConsistentTrue() throws Exception {
		QueryResultWithCount results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, true, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getQueryResult().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getQueryResult().getQueryResults().setEtag(null);
		assertEquals(set, results.getQueryResult().getQueryResults());
		assertEquals(1L, results.getCount().longValue());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test
	public void testQueryAndCountHappyCaseIsConsistentTrue() throws Exception {
		QueryResultWithCount results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		// The etag should be set
		assertEquals(status.getLastTableChangeEtag(), results.getQueryResult().getQueryResults().getEtag());
		// Clear the etag for the test
		results.getQueryResult().getQueryResults().setEtag(null);
		assertEquals(set, results.getQueryResult().getQueryResults());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test 
	public void testQueryNoColumns() throws Exception {
		// Return no columns
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		QueryResultWithCount results = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		assertNotNull(results);
		assertEquals(tableId, results.getQueryResult().getQueryResults().getTableId());
		assertNull(results.getQueryResult().getQueryResults().getEtag());
		assertNull(results.getQueryResult().getQueryResults().getHeaders());
		assertNull(results.getQueryResult().getQueryResults().getRows());
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
		selectStar.setHeaders(TableModelUtils.getSelectColumns(models));
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select * from " + tableId, selectStar, 10L, TableModelUtils.getSelectColumns(models).toString(), 1095L);
	}
	
	@Test
	public void testQueryBundleEmptySchema() throws Exception {
		// setup an empty schema.
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		Query query = new Query();
		query.setSql("select * from "+tableId);
		query.setIsConsistent(true);
		query.setOffset(0L);
		query.setLimit(Long.MAX_VALUE);
		QueryBundleRequest queryBundle = new QueryBundleRequest();
		queryBundle.setQuery(query);

		// Request query only
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_RESULTS);
		// call under test.
		QueryResultBundle bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertNotNull(bundle);
		assertNotNull(bundle.getColumnModels());
		assertNotNull(bundle.getQueryCount());
		assertNotNull(bundle.getMaxRowsPerPage());
		assertNotNull(bundle.getSelectColumns());
	}

	@Test
	public void testQueryBundleColumnsExpanded() throws Exception {
		RowSet selectStar = new RowSet();
		selectStar.setEtag("etag");
		selectStar.setHeaders(TableModelUtils.getSelectColumns(models));
		selectStar.setTableId(tableId);
		selectStar.setRows(TableModelTestUtils.createRows(models, 10));
		QueryResult selectStarResult = new QueryResult();
		selectStarResult.setNextPageToken(null);
		selectStarResult.setQueryResults(selectStar);

		runQueryBundleTest("select " + StringUtils.join(Lists.transform(models, TableModelTestUtils.convertToNameFunction), ",") + " from "
				+ tableId, selectStar, 10L, TableModelUtils.getSelectColumns(models).toString(),
				1095L);
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
		Long maxRows = this.manager.getMaxRowsPerPage(models);
		int maxRowSize = TableModelUtils.calculateMaxRowSize(models);
		Long expected = (long) (this.maxBytesPerRequest/maxRowSize);
		assertEquals(expected, maxRows);
	}
	
	@Test
	public void testGetMaxRowsPerPageEmpty(){
		Long maxRows = this.manager.getMaxRowsPerPage(new LinkedList<ColumnModel>());
		assertEquals(null, maxRows);
	}
	
	@Test
	public void testNextPageToken() throws Exception {
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		QueryResultWithCount query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getQueryResult().getNextPageToken());
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}

	@Test
	public void testNextPageTokenEscapingSingle() throws Exception {
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		// introduce escape-needed column names
		for (int i = 0; i < models.size(); i++) {
			String name = models.get(i).getName();
			name = name.substring(0, 1) + "-" + name.substring(1);
			models.get(i).setName(name);
		}

		QueryResultWithCount query = manager.query(mockProgressCallbackVoid, user, "select \"i-0\" from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getQueryResult().getNextPageToken());
		assertTrue(query.getQueryResult().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);
		
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testNextPageTokenEscapingStart() throws Exception {
		RowSet rowSet = new RowSet();
		rowSet.setRows(Collections.nCopies(100000, new Row()));
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet);

		// introduce escape-needed column names
		for (int i = 0; i < models.size(); i++) {
			String name = models.get(i).getName();
			name = name.substring(0, 1) + "-" + name.substring(1);
			models.get(i).setName(name);
		}

		QueryResultWithCount query = manager.query(mockProgressCallbackVoid, user, "select * from " + tableId, null, 0L, 100000L, true, false, false);
		assertNotNull(query.getQueryResult().getNextPageToken());
		assertTrue(query.getQueryResult().getNextPageToken().getToken().indexOf("&quot;i-0&quot") != -1);
		
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
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
	
	@Test
	public void testCreateQuerySelectStar() throws EmptySchemaException {
		List<SortItem> sortList= null;
		// call under test
		SqlQuery result = manager.createQuery("select * from "+tableId, sortList);
		assertNotNull(result);
		assertEquals("SELECT i0, i1, i2, i3, i4, i5, i6, i7, i8 FROM syn123", result.getModel().toSql());
	}
	
	@Test
	public void testCreateQueryOverrideSort() throws EmptySchemaException {
		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		List<SortItem> sortList= Lists.newArrayList(sort);
		// call under test
		SqlQuery result = manager.createQuery("select i2, i0 from "+tableId, sortList);
		assertNotNull(result);
		assertEquals("SELECT i2, i0 FROM syn123 ORDER BY i0 DESC", result.getModel().toSql());
	}
	
	@Test
	public void testCreateQueryEmptySchema() {
		// setup an empty schema.
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		List<SortItem> sortList= null;
		// call under test
		try {
			manager.createQuery("select * from "+tableId, sortList);
			fail("Should have failed since the schema is empty");
		} catch (EmptySchemaException e) {
			assertEquals(tableId, e.getTableId());
		}
	}
	
	@Test
	public void testRunConsistentQueryAsStreamDownload() throws NotFoundException, TableUnavilableException, TableFailedException{
		String sql = "select * from "+tableId;
		List<SortItem> sortList = null;
		boolean includeRowIdAndVersion = false;
		boolean writeHeader = true;
		// call under test
		DownloadFromTableResult results = manager.runConsistentQueryAsStream(
				mockProgressCallbackVoid, user, sql, sortList, writer,
				includeRowIdAndVersion, writeHeader);
		assertNotNull(results);
		
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		assertEquals(11, writtenLines.size());
		verify(mockProgressCallbackVoid, times(1)).progressMade(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRunConsistentQueryAsStreamEmptyDownload() throws NotFoundException, TableUnavilableException, TableFailedException{
		// setup an empty schema.
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		String sql = "select * from "+tableId;
		List<SortItem> sortList = null;
		boolean includeRowIdAndVersion = false;
		boolean writeHeader = true;
		// call under test
		manager.runConsistentQueryAsStream(
				mockProgressCallbackVoid, user, sql, sortList, writer,
				includeRowIdAndVersion, writeHeader);
	}
	
}
