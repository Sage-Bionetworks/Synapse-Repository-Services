package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
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
import org.mockito.ArgumentCaptor;
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
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
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
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
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
	List<Row> rows;
	TableStatus status;
	int maxBytesPerRequest;
	CSVWriterStream writer;
	List<String[]> writtenLines;
	
	List<SortItem> sortList;
	SqlQuery capturedQuery;
	
	
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
		
		rows = TableModelTestUtils.createRows(models, 10);
		
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(10L);
		
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(models);
		when(mockTableConnectionFactory.getConnection(tableId)).thenReturn(mockTableIndexDAO);
		stub(mockTableIndexDAO.queryAsStream(any(ProgressCallback.class),any(SqlQuery.class), any(RowHandler.class))).toAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				capturedQuery = (SqlQuery) invocation.getArguments()[1];
				RowHandler handler =  (RowHandler) invocation.getArguments()[2];
				// Pass all rows to the handler
				for (Row row : rows) {
					handler.nextRow(row);
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
		writer = new CSVWriterStream() {

			@Override
			public void writeNext(String[] nextLine) {
				writtenLines.add(nextLine);
			}
		};

		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		sortList = Lists.newArrayList(sort);
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryAsStreamUnauthroized() throws Exception {
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		RowHandler rowHandler = null;
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
	}
	
	@Test
	public void testQueryAsStreamAuthorized() throws Exception {
		RowHandler rowHandler = null;
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryAsStreamIsConsistentTrue() throws Exception{
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		QueryResultBundle result = manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
		assertNotNull(result);
		assertNotNull(result.getQueryResult());
		assertNotNull(result.getQueryResult().getQueryResults());
		assertEquals("Consistent query must return the etag of the current status",
				status.getLastTableChangeEtag(), result.getQueryResult().getQueryResults().getEtag());
		// an exclusive lock must be held for a consistent query.
		verify(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(any(ProgressCallback.class), anyString(), anyInt(), any(ProgressingCallable.class));
		// The table status should be checked only for a consistent query.
		verify(mockTableManagerSupport).getTableStatusOrCreateIfNotExists(tableId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testQueryAsStreamIsConsistentTrueNotFoundException() throws Exception{
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new NotFoundException("not found"));
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
	}
	
	@Test (expected=TableUnavailableException.class)
	public void testQueryAsStreamIsConsistentTrueTableUnavailableException() throws Exception{
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new TableUnavailableException(new TableStatus()));
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
	}
	
	@Test (expected=TableFailedException.class)
	public void testQueryAsStreamIsConsistentTrueTableFailedException() throws Exception{
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new TableFailedException(new TableStatus()));
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
	}
	
	@Test (expected=LockUnavilableException.class)
	public void testQueryAsStreamIsConsistentTrueLockUnavilableException() throws Exception{
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new LockUnavilableException());
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
	}
	
	@Test
	public void testQueryAsStreamIsConsistentFalse() throws Exception{
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = false;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		QueryResultBundle result = manager.queryAsStream(mockProgressCallbackVoid, user, query, rowHandler, runCount, isConsistent);
		assertNotNull(result);
		assertNotNull(result.getQueryResult());
		assertNotNull(result.getQueryResult().getQueryResults());
		assertNull("Non-Consistent query result must not contain an etag.", result.getQueryResult().getQueryResults().getEtag());
		// an exclusive lock must not be held for a non-consistent query.
		verify(mockTableManagerSupport, never()).tryRunWithTableNonexclusiveLock(any(ProgressCallback.class), anyString(), anyInt(), any(ProgressingCallable.class));
		// The table status should not be checked only for a non-consistent query.
		verify(mockTableManagerSupport, never()).getTableStatusOrCreateIfNotExists(tableId);
	}
	
	@Test
	public void testQueryAsStreamAfterLockCountOnly() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// null handler indicates not to run the main query.
		RowHandler rowHandler = null;
		boolean runCount = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterLock(mockProgressCallbackVoid, query, rowHandler, runCount);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertEquals(count, results.getQueryCount());
		assertNull(results.getQueryResult());
	}
	
	@Test
	public void testQueryAsStreamAfterLockNoCount() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// non-null handler indicates the query should be run.
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = false;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterLock(mockProgressCallbackVoid, query, rowHandler, runCount);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertNull(results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
	}
	
	@Test
	public void testQueryAsStreamAfterLockQueryAndCount() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// non-null handler indicates the query should be run.
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterLock(mockProgressCallbackVoid, query, rowHandler, runCount);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertEquals(count, results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
	}
	
	@Test
	public void testRunQueryAsStream() throws ParseException{
		SinglePageRowHandler rowHandler = new SinglePageRowHandler();
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		RowSet rowSet = manager.runQueryAsStream(mockProgressCallbackVoid, query, rowHandler);
		assertNotNull(rowSet);
		assertEquals(TableModelUtils.getSelectColumns(models), rowSet.getHeaders());
		assertEquals(tableId, rowSet.getTableId());
		assertEquals(rows, rowHandler.getRows());
	}
	
	@Test
	public void testCreateEmptyBundle(){
		QueryResultBundle results = TableQueryManagerImpl.createEmptyBundle(tableId);
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNull(results.getQueryResult().getQueryResults().getEtag());
		assertNotNull(results.getQueryResult().getQueryResults().getHeaders());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		assertEquals(tableId, results.getQueryResult().getQueryResults().getTableId());
		assertEquals(new Long(0), results.getQueryCount());
		assertEquals(new Long(1), results.getMaxRowsPerPage());
	}
	

	@Test 
	public void testQuerySinglePageEmptySchema() throws Exception {
		// Return no columns
		when(mockColumnModelDAO.getColumnModelsForObject(tableId)).thenReturn(new LinkedList<ColumnModel>());
		QueryResultBundle results = manager.querySinglePage(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
		assertNotNull(results);
		QueryResultBundle emptyResults = TableQueryManagerImpl.createEmptyBundle(tableId);
		assertEquals(emptyResults, results);
	}
	
	/**
	 * Test for a consistent query when the table index is not available.
	 * @throws Exception
	 */
	@Test
	public void testQueryIsConsistentTrueNotAvailable() throws Exception {
		status.setState(TableState.PROCESSING);
		try{
			manager.querySinglePage(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavailableException e){
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
			manager.querySinglePage(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, true, false, true);
			fail("should have failed");
		}catch(TableUnavailableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableManagerSupport, times(1)).getTableStatusOrCreateIfNotExists(tableId);
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

		runQueryBundleTest("select * from " + tableId, selectStar, 10L, TableModelUtils.getSelectColumns(models).toString(), 2929L);
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
				2929L);
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
	public void testCreateNextPageTokenEscapingSingle() throws Exception {
		String sql = "select \"i-0\" from " + tableId;
		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		List<SortItem> sortList= Lists.newArrayList(sort);
		
		Long nextOffset = 10L;
		Long limit = 21L;
		boolean isConsistent = true;
		QueryNextPageToken token = TableQueryManagerImpl.createNextPageToken(sql, sortList, nextOffset, limit, isConsistent);
		Query query = TableQueryManagerImpl.createQueryFromNextPageToken(token);
		assertEquals(sql, query.getSql());
		assertEquals(nextOffset, query.getOffset());
		assertEquals(limit, query.getLimit());
		assertEquals(isConsistent, query.getIsConsistent());
		assertEquals(sortList, query.getSort());
	}
	
	@Test
	public void testValidateTableIsAvailableWithStateAvailable() throws NotFoundException, TableUnavailableException, TableFailedException{
		status.setState(TableState.AVAILABLE);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// call under test
		TableStatus resultsStatus = manager.validateTableIsAvailable(tableId);
		assertNotNull(resultsStatus);
		assertEquals(status, resultsStatus);
	}
	
	@Test (expected=TableUnavailableException.class)
	public void testValidateTableIsAvailableWithStateProcessing() throws NotFoundException, TableUnavailableException, TableFailedException{
		status.setState(TableState.PROCESSING);
		when(mockTableManagerSupport.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// call under test
		manager.validateTableIsAvailable(tableId);
	}
	
	@Test (expected=TableFailedException.class)
	public void testValidateTableIsAvailableWithStateFailed() throws NotFoundException, TableUnavailableException, TableFailedException{
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
	public void testRunConsistentQueryAsStreamDownload() throws NotFoundException, TableUnavailableException, TableFailedException, LockUnavilableException{
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
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRunConsistentQueryAsStreamEmptyDownload() throws NotFoundException, TableUnavailableException, TableFailedException, LockUnavilableException {
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
	
	
	@Test
	public void testRunCountQuerySimpleAggregate() throws ParseException{
		SqlQuery query = new SqlQuery("select max(i0) from "+tableId, models);
		long count = manager.runCountQuery(query);
		assertEquals(1l, count);
		// no need to run a query for a simple aggregate
		verify(mockTableIndexDAO, never()).countQuery(anyString(), anyMapOf(String.class, Object.class));
	}
	
	@Test
	public void testRunCountQueryNoPagination() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" where i0 = 'aValue'", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query);
		assertEquals(200L, count);
		assertEquals("SELECT COUNT(*) FROM T123 WHERE _C0_ = :b0", sqlCaptrue.getValue());
		verify(mockTableIndexDAO).countQuery(anyString(), anyMapOf(String.class, Object.class));
	}
	
	@Test
	public void testRunCountQueryWithLimitLessCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query);
		assertEquals(100L, count);
	}
	
	@Test
	public void testRunCountQueryWithLimitMoreCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 300", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query);
		assertEquals(200L, count);
	}
	
	@Test
	public void testRunCountQueryWithLimitAndOffsetLessThanCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100 offset 50", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query);
		assertEquals(100L, count);
	}
	
	@Test
	public void testRunCountQueryWithLimitAndOffsetMoreThanCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100 offset 150", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query);
		assertEquals(50L, count);
	}
	
	@Test
	public void testIsRowCountEqualToMaxRowsPerPage(){
		QueryResultBundle bundle = null;
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		bundle = new QueryResultBundle();
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		bundle.setQueryResult(new QueryResult());
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		bundle.getQueryResult().setQueryResults(new RowSet());
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		Row row = new Row();
		bundle.getQueryResult().getQueryResults().setRows(Lists.newArrayList(row));
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		bundle.setMaxRowsPerPage(new Long(1));
		assertTrue(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
		bundle.setMaxRowsPerPage(new Long(2));
		assertFalse(TableQueryManagerImpl.isRowCountEqualToMaxRowsPerPage(bundle));
	}
	
	@Test
	public void testQuerySinglePageWithNextPage() throws Exception{
		// setup the results to return one row.
		Row row = rows.get(0);
		rows.clear();
		rows.add(row);
		
		Long offset = null;
		Long limit = null;
		boolean runQuery = true;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId;
		manager.setMaxBytesPerRequest(1);
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				offset, limit, runQuery, runCount, isConsistent);
		
		assertNotNull(result);
		assertEquals(new Long(1), result.getMaxRowsPerPage());
		assertNotNull(result.getQueryResult());
		assertNotNull(result.getQueryResult().getNextPageToken());
		Query nextQuery = TableQueryManagerImpl.createQueryFromNextPageToken(result.getQueryResult().getNextPageToken());
		assertNotNull(nextQuery);
		assertEquals(null, nextQuery.getLimit());
		assertEquals(new Long(1),nextQuery.getOffset());
		assertEquals(query, nextQuery.getSql());
		assertEquals(isConsistent, nextQuery.getIsConsistent());
	}
	
	@Test
	public void testQuerySinglePageWithNoNextPage() throws Exception{		
		Long offset = null;
		Long limit = null;
		boolean runQuery = true;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId;
		// set to not limit the number of rows.
		manager.setMaxBytesPerRequest(Integer.MAX_VALUE);
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				offset, limit, runQuery, runCount, isConsistent);
		
		assertNotNull(result);
		assertNotNull(result.getMaxRowsPerPage());
		assertNotNull(result.getQueryResult());
		assertNull(result.getQueryResult().getNextPageToken());
	}
	
	@Test
	public void testQuerySinglePageRunQueryTrue() throws Exception{		
		Long offset = null;
		Long limit = null;
		boolean runQuery = true;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId;
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				offset, limit, runQuery, runCount, isConsistent);
		
		assertNotNull(result);
		assertNotNull(result.getQueryResult());
		assertNotNull(result.getQueryResult().getQueryResults());
		List<Row> rows = result.getQueryResult().getQueryResults().getRows();
		assertNotNull(rows);
		assertEquals(rows, rows);
	}
	
	@Test
	public void testQuerySinglePageRunQueryFalse() throws Exception{		
		Long offset = null;
		Long limit = null;
		boolean runQuery = false;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId;
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				offset, limit, runQuery, runCount, isConsistent);
		
		assertNotNull(result);
		// there should be no query results.
		assertNull(result.getQueryResult());
	}
}
