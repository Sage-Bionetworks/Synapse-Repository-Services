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
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_COLUMN_MODELS;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_COUNT;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_RESULTS;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_SELECT_COLUMNS;
import static org.sagebionetworks.repo.manager.table.TableQueryManagerImpl.BUNDLE_MASK_QUERY_FACETS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
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
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
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
	
	@Captor
	ArgumentCaptor<Map<String,Object>> paramsCaptor;
	
	String facetColumnName;
	String facetMax;
	FacetColumnRangeRequest facetColumnRequest;
	private FacetColumnResultRange expectedRangeResult;
	private RowSet rowSet1;
	private RowSet rowSet2;
	
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
		
		when(mockTableManagerSupport.validateTableReadAccess(user, tableId)).thenReturn(EntityType.table);
		
		ColumnModel benefactorColumn = EntityField.benefactorId.getColumnModel();
		benefactorColumn.setId("999");
		when(mockTableManagerSupport.getColumnModel(EntityField.benefactorId)).thenReturn(benefactorColumn);
		HashSet<Long> benfactors = Sets.newHashSet(333L,444L);
		HashSet<Long> subSet = Sets.newHashSet(444L);
		when(mockTableIndexDAO.getDistinctLongValues(tableId, benefactorColumn.getId())).thenReturn(benfactors);
		when(mockTableManagerSupport.getAccessibleBenefactors(user, benfactors)).thenReturn(subSet);
		
		facetColumnName = "i2";
		facetMax = "45";
		facetColumnRequest = new FacetColumnRangeRequest();
		facetColumnRequest.setColumnName(facetColumnName);
		facetColumnRequest.setMax(facetMax);
		
		String expectedColMin = "100";
		String expectedColMax = "123";
		String expectedColumnName = "i2";
		FacetType expectedFacetType = FacetType.range;
		rowSet1 = createRowSetForTest(Lists.newArrayList(FacetTransformerValueCounts.VALUE_ALIAS, FacetTransformerValueCounts.COUNT_ALIAS));
		rowSet2 = createRowSetForTest(Lists.newArrayList(FacetTransformerRange.MIN_ALIAS, FacetTransformerRange.MAX_ALIAS), Lists.newArrayList(expectedColMin, expectedColMax));
		expectedRangeResult = new FacetColumnResultRange();
		expectedRangeResult.setColumnName(expectedColumnName);
		expectedRangeResult.setColumnMin(expectedColMin);
		expectedRangeResult.setFacetType(expectedFacetType);
		expectedRangeResult.setColumnMax(expectedColMax);
	}

	@Test (expected = UnauthorizedException.class)
	public void testQueryAsStreamUnauthroized() throws Exception {
		doThrow(new UnauthorizedException()).when(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		RowHandler rowHandler = null;
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
	}
	
	@Test
	public void testQueryAsStreamAuthorized() throws Exception {
		RowHandler rowHandler = null;
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
	}
	
	@Test
	public void testQueryAsStreamIsConsistentTrue() throws Exception{
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		QueryResultBundle result = manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
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
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
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
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
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
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
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
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
	}
	
	@Test (expected=EmptyResultException.class)
	public void testQueryAsStreamEmptyResultException() throws Exception{
		when(mockTableManagerSupport.tryRunWithTableNonexclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new EmptyResultException());
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
	}
	
	@Test
	public void testQueryAsStreamIsConsistentFalse() throws Exception{
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		boolean isConsistent = false;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test.
		QueryResultBundle result = manager.queryAsStream(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false, isConsistent);
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
	public void testQueryAsStreamWithAuthorizationTableEntity() throws Exception{
		// Setup a table entity.
		EntityType type = EntityType.table;
		when(mockTableManagerSupport.validateTableReadAccess(user, tableId)).thenReturn(type);
		SqlQuery query = new SqlQuery("select i0 from "+tableId, models);
		
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		// call under test
		QueryResultBundle result = manager.queryAsStreamWithAuthorization(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false);
		// auth check should occur
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		// a benefactor check should not occur for TableEntities
		verify(mockTableManagerSupport, never()).getAccessibleBenefactors(any(UserInfo.class), anySetOf(Long.class));
	}
	
	@Test
	public void testQueryAsStreamWithAuthorizationFileView() throws Exception{
		// add benefactor to the schema
		ColumnModel benefactorColumn = EntityField.benefactorId.getColumnModel();
		benefactorColumn.setId("999");
		models.add(benefactorColumn);
		// capture the SQL
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// Setup a fileView
		EntityType type = EntityType.entityview;
		when(mockTableManagerSupport.validateTableReadAccess(user, tableId)).thenReturn(type);
		SqlQuery query = new SqlQuery("select i0 from "+tableId, models);
		
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		// call under test
		QueryResultBundle result = manager.queryAsStreamWithAuthorization(mockProgressCallbackVoid, user, query, null, rowHandler, runCount, false);
		assertNotNull(result);
		// auth check should occur
		verify(mockTableManagerSupport).validateTableReadAccess(user, tableId);
		// a benefactor check must occur for FileViews
		verify(mockTableManagerSupport).getAccessibleBenefactors(any(UserInfo.class), anySetOf(Long.class));
		// validate the benefactor filter is applied
		assertEquals("SELECT COUNT(*) FROM T123 WHERE _C999_ IN ( 444 )", sqlCaptrue.getValue());
	}
	
	@Test
	public void testQueryAsStreamAfterAuthorizationCountOnly() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// null handler indicates not to run the main query.
		RowHandler rowHandler = null;
		boolean runCount = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, null, rowHandler, runCount, false, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertEquals(count, results.getQueryCount());
		assertNull(results.getQueryResult());
	}
	
	/**
	 * A limit included in the query limits the count.
	 * @throws Exception
	 */
	@Test
	public void testQueryAsStreamAfterAuthorizationWithLimit() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// null handler indicates not to run the main query.
		RowHandler rowHandler = null;
		boolean runCount = true;
		SqlQuery query = new SqlQuery("select * from " + tableId+" limit 11", models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, null, rowHandler, runCount, false, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(new Long(11), results.getQueryCount());
	}
	
	@Test
	public void testQueryAsStreamAfterAuthorizationNoCount() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// non-null handler indicates the query should be run.
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = false;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, null, rowHandler, runCount, false, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertNull(results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
	}
	
	@Test
	public void testQueryAsStreamAfterAuthorizationQueryAndCount() throws Exception {
		Long count = 201L;
		// setup count results
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		// non-null handler indicates the query should be run.
		RowHandler rowHandler = new SinglePageRowHandler();
		boolean runCount = true;
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, null, rowHandler, runCount, false, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertEquals(count, results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
	}
	
	

	@Test
	public void testQueryAsStreamAfterAuthorizationNonEmptyFacetColumnsListNotReturningFacets() throws ParseException, LockUnavilableException, TableUnavailableException, TableFailedException{
		Long count = 201L;
		// setup count results
		ArgumentCaptor<String> queryStringCaptor = ArgumentCaptor.forClass(String.class);
		//capture the query to check that the queryToRun is result of appendFacetSearchCondition() and not the original query
		when(mockTableIndexDAO.countQuery(queryStringCaptor.capture(), paramsCaptor.capture())).thenReturn(count);

		List<FacetColumnRequest> facetRequestList = new ArrayList<>();
		facetRequestList.add(facetColumnRequest);
		
		RowHandler rowHandler = null;
		boolean returnFacets = false;
		boolean runCount = true; //running count to check that the SqlQuery gets facet filters appended
		
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		
		
		assertEquals(1, facetRequestList.size());
		
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, facetRequestList, rowHandler, runCount, returnFacets, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertEquals(count, results.getQueryCount());
		assertNull(results.getQueryResult());
		
		//check to make sure count query was run using a SqlQuery with an facet WHERE clause
		assertTrue(queryStringCaptor.getValue().contains("WHERE ( ( ( _C2_ <= :b0 ) ) )"));
		Map<String, Object> capturedParams = paramsCaptor.getValue();
		assertFalse(capturedParams.isEmpty());
		assertEquals(facetMax, capturedParams.get("b0").toString());
	}
	
	@Test
	public void testQueryAsStreamAfterAuthorizationNonEmptyFacetColumnsListReturnFacets() throws ParseException, LockUnavilableException, TableUnavailableException, TableFailedException{	
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet1, rowSet2);
		List<FacetColumnRequest> facetRequestList = new ArrayList<>();
		facetRequestList.add(facetColumnRequest);
		expectedRangeResult.setSelectedMin(facetColumnRequest.getMin());
		expectedRangeResult.setSelectedMax(facetColumnRequest.getMax());

		
		RowHandler rowHandler = null;
		boolean returnFacets = true;
		boolean runCount = false; //running count to check that the SqlQuery gets facet filters appended
		
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		
		
		assertEquals(1, facetRequestList.size());
		
		QueryResultBundle results = manager.queryAsStreamAfterAuthorization(mockProgressCallbackVoid, query, facetRequestList, rowHandler, runCount, returnFacets, mockTableIndexDAO);
		assertNotNull(results);
		assertEquals(models, results.getColumnModels());
		assertEquals(TableModelUtils.getSelectColumns(models), results.getSelectColumns());
		assertNull(results.getQueryResult());
		assertNull(results.getQueryCount());
		
		//facet result asserts
		assertNotNull(results.getFacets());
		assertEquals(2, results.getFacets().size());
		FacetColumnResult facetResultColumn = results.getFacets().get(1);
		assertEquals(expectedRangeResult, facetResultColumn);
	}
	
	
	@Test
	public void testRunQueryAsStream() throws ParseException{
		SinglePageRowHandler rowHandler = new SinglePageRowHandler();
		SqlQuery query = new SqlQuery("select * from " + tableId, models);
		// call under test
		RowSet rowSet = manager.runQueryAsStream(mockProgressCallbackVoid, query, rowHandler, mockTableIndexDAO);
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
		QueryResultBundle results = manager.querySinglePage(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, null, true, false, false ,true);
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
			manager.querySinglePage(mockProgressCallbackVoid, user, "select * from " + tableId + " limit 1", null, null, null, null, true, false, false, true);
			fail("should have failed");
		}catch(TableUnavailableException e){
			// expected
			assertEquals(status, e.getStatus());
		}
		verify(mockTableManagerSupport, times(1)).getTableStatusOrCreateIfNotExists(tableId);
	}
	
	
	@Test
	public void runQueryBundleTest()
			throws Exception {
		List<SelectColumn> selectColumns = TableModelUtils.getSelectColumns(models);
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		maxBytesPerRequest = maxRowSizeBytes*10;
		manager.setMaxBytesPerRequest(maxBytesPerRequest);
		Long maxRowsPerPage = new Long(maxBytesPerRequest/maxRowSizeBytes);
		// setup the count
		Long count = 101L;
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(count);
		
		Query query = new Query();
		query.setSql("select * from " + tableId);
		query.setIsConsistent(true);
		query.setOffset(0L);
		query.setLimit(Long.MAX_VALUE);
		QueryBundleRequest queryBundle = new QueryBundleRequest();
		queryBundle.setQuery(query);

		// Request query only
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_RESULTS);
		QueryResultBundle bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(rows, bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// Count only
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_COUNT);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(count, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// select columns
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_SELECT_COLUMNS);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns());
		assertEquals(null, bundle.getMaxRowsPerPage());

		// max rows per page
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
		
		// max rows per page
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_COLUMN_MODELS);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(null, bundle.getQueryResult());
		assertEquals(null, bundle.getQueryCount());
		assertEquals(null, bundle.getSelectColumns());
		assertEquals(models, bundle.getColumnModels());

		// now combine them all
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_RESULTS | BUNDLE_MASK_QUERY_COUNT
				| BUNDLE_MASK_QUERY_SELECT_COLUMNS | BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertEquals(rows, bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(count, bundle.getQueryCount());
		assertEquals(selectColumns, bundle.getSelectColumns());
		assertEquals(maxRowsPerPage, bundle.getMaxRowsPerPage());
	}
	
	@Test
	public void testQueryBundleFacets() throws LockUnavilableException, TableUnavailableException, TableFailedException{
		when(mockTableIndexDAO.query(any(ProgressCallback.class), any(SqlQuery.class))).thenReturn(rowSet1, rowSet2);
		
		Query query = new Query();
		query.setSql("select * from " + tableId);
		query.setIsConsistent(true);
		query.setOffset(0L);
		query.setLimit(Long.MAX_VALUE);
		QueryBundleRequest queryBundle = new QueryBundleRequest();
		queryBundle.setQuery(query);
		
		queryBundle.setPartMask(BUNDLE_MASK_QUERY_FACETS);
		QueryResultBundle bundle = manager.queryBundle(mockProgressCallbackVoid, user, queryBundle);
		assertNull(bundle.getQueryResult());
		assertNull(bundle.getQueryCount());
		assertNull(bundle.getSelectColumns());
		assertNull(bundle.getColumnModels());
		assertNotNull(bundle.getFacets());
		assertEquals(2, bundle.getFacets().size());
		//we don't care about the first facet result because it has no useful data and only exists to make sure for loops work
		assertEquals(expectedRangeResult, bundle.getFacets().get(1));
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
		FacetColumnRequest facet = new FacetColumnRangeRequest();
		facet.setColumnName(facetColumnName);
		List<FacetColumnRequest> selectedFacets = Lists.newArrayList(facet);
		
		Long nextOffset = 10L;
		Long limit = 21L;
		boolean isConsistent = true;
		QueryNextPageToken token = TableQueryManagerImpl.createNextPageToken(sql, sortList, nextOffset, limit, isConsistent, selectedFacets);
		Query query = TableQueryManagerImpl.createQueryFromNextPageToken(token);
		assertEquals(sql, query.getSql());
		assertEquals(nextOffset, query.getOffset());
		assertEquals(limit, query.getLimit());
		assertEquals(isConsistent, query.getIsConsistent());
		assertEquals(sortList, query.getSort());
		assertEquals(selectedFacets, query.getSelectedFacets());
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
	public void testCreateQuerySelectStar() throws EmptyResultException {
		List<SortItem> sortList= null;
		// call under test
		SqlQuery result = manager.createQuery("select * from "+tableId, sortList);
		assertNotNull(result);
		assertEquals("SELECT \"i0\", \"i1\", \"i2\", \"i3\", \"i4\", \"i5\", \"i6\", \"i7\", \"i8\", \"i9\" FROM syn123", result.getModel().toSql());
	}
	
	@Test
	public void testCreateQueryOverrideSort() throws EmptyResultException {
		SortItem sort = new SortItem();
		sort.setColumn("i0");
		sort.setDirection(SortDirection.DESC);
		List<SortItem> sortList= Lists.newArrayList(sort);
		// call under test
		SqlQuery result = manager.createQuery("select i2, i0 from "+tableId, sortList);
		assertNotNull(result);
		assertEquals("SELECT i2, i0 FROM syn123 ORDER BY \"i0\" DESC", result.getModel().toSql());
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
		} catch (EmptyResultException e) {
			assertEquals(tableId, e.getTableId());
		}
	}
	
	@Test
	public void testRunConsistentQueryAsStreamDownload() throws NotFoundException, TableUnavailableException, TableFailedException, LockUnavilableException{
		String sql = "select * from "+tableId;
		List<SortItem> sortList = null;
		boolean includeRowIdAndVersion = false;
		boolean writeHeader = true;
		List<FacetColumnRequest> selectedFacets = null;

		// call under test
		DownloadFromTableResult results = manager.runConsistentQueryAsStream(
				mockProgressCallbackVoid, user, sql, sortList, selectedFacets, writer,
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
		List<FacetColumnRequest> selectedFacets = null;
		// call under test
		manager.runConsistentQueryAsStream(
				mockProgressCallbackVoid, user, sql, sortList, selectedFacets, writer,
				includeRowIdAndVersion, writeHeader);
	}
	
	
	@Test
	public void testRunCountQuerySimpleAggregate() throws ParseException{
		SqlQuery query = new SqlQuery("select max(i0) from "+tableId, models);
		long count = manager.runCountQuery(query, mockTableIndexDAO);
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
		long count = manager.runCountQuery(query, mockTableIndexDAO);
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
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals(100L, count);
	}
	
	@Test
	public void testRunCountQueryWithLimitMoreCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 300", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals(200L, count);
	}
		
	@Test
	public void testRunCountQueryWithLimitAndOffsetLessThanCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100 offset 50", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals(100L, count);
	}
	
	@Test
	public void testRunCountQueryWithLimitAndOffsetMoreThanCount() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100 offset 150", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals(50L, count);
	}
	
	@Test
	public void testRunCountQueryWithCountLessThanOffset() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" limit 100 offset 150", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(149L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals(0L, count);
	}
	
	/**
	 * The group by references an 'AS' value from the select.  The
	 * resulting count(distinct) must use a direct reference not the 'AS' value.
	 * @throws ParseException
	 */
	@Test
	public void testRunCountQueryPLFM_3899() throws ParseException{
		SqlQuery query = new SqlQuery("select i0 as bar from "+tableId+" group by bar", models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals("SELECT COUNT(DISTINCT _C0_) FROM T123", sqlCaptrue.getValue());
	}
	
	/**
	 * When a distinct query is converted to a count query, any 'AS' clause
	 * from the original SQL must be excluded in the resulting count(distinct).
	 * @throws ParseException
	 */
	@Test
	public void testRunCountQueryPLFM_3900() throws ParseException{
		SqlQuery query = new SqlQuery("select distinct i0 as bar, i4 from "+tableId, models);
		ArgumentCaptor<String> sqlCaptrue = ArgumentCaptor.forClass(String.class);
		// setup the count returned from query
		when(mockTableIndexDAO.countQuery(sqlCaptrue.capture(), anyMapOf(String.class, Object.class))).thenReturn(200L);
		// method under test
		long count = manager.runCountQuery(query, mockTableIndexDAO);
		assertEquals("SELECT COUNT(DISTINCT _C0_, _C4_) FROM T123", sqlCaptrue.getValue());
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
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
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
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
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
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
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
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
		assertNotNull(result);
		// there should be no query results.
		assertNull(result.getQueryResult());
	}
	
	/**
	 * Override of the limit should not change the total count.
	 */
	@Test
	public void testQuerySinglePageOverrideLimit() throws Exception{
		Long totalCount = 101L;
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(totalCount);
		Long offset = 10L;
		Long limit = 11L;;
		boolean runQuery = false;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId;
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
		assertNotNull(result);
		// there should be no query results.
		assertNull(result.getQueryResult());
		assertEquals(totalCount, result.getQueryCount());
	}
	
	/**
	 * When the query includes a limit, that limit will change the count.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQuerySinglePageWithLimit() throws Exception{
		Long totalCount = 101L;
		when(mockTableIndexDAO.countQuery(anyString(), anyMapOf(String.class, Object.class))).thenReturn(totalCount);
		Long offset = null;
		Long limit = null;;
		boolean runQuery = false;
		boolean runCount = true;
		boolean isConsistent = true;
		String query = "select * from "+tableId+" limit 11";
		// call under test.
		QueryResultBundle result = manager.querySinglePage(
				mockProgressCallbackVoid, user, query, sortList,
				null, offset, limit, runQuery, runCount, false, isConsistent);
		
		assertNotNull(result);
		// there should be no query results.
		assertNull(result.getQueryResult());
		assertEquals(new Long(11), result.getQueryCount());
	}
	
	@Test
	public void testBuildBenefactorFilter() throws ParseException, EmptyResultException{
		// add benefactor to the schema
		ColumnModel benefactorColumn = EntityField.benefactorId.getColumnModel();
		benefactorColumn.setId("99");
		models.add(benefactorColumn);
		
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" where i1 is not null", models);
		LinkedHashSet<Long> benefactorIds = new LinkedHashSet<Long>();
		benefactorIds.add(456L);
		benefactorIds.add(123L);
		
		// call under test
		SqlQuery filtered = TableQueryManagerImpl.buildBenefactorFilter(query, benefactorIds, benefactorColumn.getId());
		assertNotNull(filtered);
		// should filter by benefactorId
		assertEquals("SELECT i0 FROM syn123 WHERE ( i1 IS NOT NULL ) AND _C99_ IN ( 456, 123 )", filtered.getModel().toSql());
		assertEquals("SELECT _C0_, ROW_ID, ROW_VERSION FROM T123 WHERE ( _C1_ IS NOT NULL ) AND _C99_ IN ( 456, 123 )", filtered.getOutputSQL());
	}
	
	/**
	 * 
	 * PLFM-4036 identified that the benefactor search condition would limit the row visibility to
	 * the caller by appending 'AND <BENEFACTOR_FILTER> to a user's existing query. Therefore, if
	 * the user's original query contained at least two search conditions separated by an 'OR', either
	 * of the original conditions could negate the benefactor filter.
	 * 
	 * The fix was to unconditionally add the filter benefactor to the query such as:
	 * WHERE ( <USER_CONDITION_1> OR <USER_CONDITION_2> ) AND <BENEFACTOR_FILTER>
	 * @throws ParseException
	 * @throws EmptyResultException
	 */
	@Test
	public void testBuildBenefactorFilterPLFM_4036() throws ParseException, EmptyResultException{
		// add benefactor to the schema
		ColumnModel benefactorColumn = EntityField.benefactorId.getColumnModel();
		benefactorColumn.setId("99");
		
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" where i1 > 0 or i1 is not null", models);
		LinkedHashSet<Long> benefactorIds = new LinkedHashSet<Long>();
		benefactorIds.add(456L);
		benefactorIds.add(123L);
		
		// call under test
		SqlQuery filtered = TableQueryManagerImpl.buildBenefactorFilter(query, benefactorIds, benefactorColumn.getId());
		assertNotNull(filtered);
		// should filter by benefactorId
		assertEquals("SELECT i0 FROM syn123 WHERE ( i1 > 0 OR i1 IS NOT NULL ) AND _C99_ IN ( 456, 123 )", filtered.getModel().toSql());
	}
	
	@Test
	public void testBuildBenefactorFilterNoWhere() throws ParseException, EmptyResultException{
		// no where clause in the original query.
		SqlQuery query = new SqlQuery("select i0 from "+tableId, models);
		LinkedHashSet<Long> benefactorIds = new LinkedHashSet<Long>();
		benefactorIds.add(123L);
		String benefactorColumnId = "44";
		// call under test
		SqlQuery filtered = TableQueryManagerImpl.buildBenefactorFilter(query, benefactorIds, benefactorColumnId);
		assertNotNull(filtered);
		// should filter by benefactorId
		assertEquals("SELECT i0 FROM syn123 WHERE _C44_ IN ( 123 )", filtered.getModel().toSql());
	}
	
	@Test (expected=EmptyResultException.class)
	public void testBuildBenefactorFilterEmpty() throws ParseException, EmptyResultException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId+" where i1 is not null", models);
		LinkedHashSet<Long> benefactorIds = new LinkedHashSet<Long>();
		String benefactorColumnId = "44";
		// call under test
		TableQueryManagerImpl.buildBenefactorFilter(query, benefactorIds, benefactorColumnId);
	}
	
	@Test (expected=EmptyResultException.class)
	public void testAddRowLevelFilterEmpty() throws ParseException, EmptyResultException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId, models);
		ColumnModel benefactorColumn = EntityField.benefactorId.getColumnModel();
		benefactorColumn.setId("999");
		when(mockTableManagerSupport.getColumnModel(EntityField.benefactorId)).thenReturn(benefactorColumn);
		//return empty benefactors
		when(mockTableIndexDAO.getDistinctLongValues(tableId, benefactorColumn.getId())).thenReturn(new HashSet<Long>());
		// call under test
		manager.addRowLevelFilter(user, query, mockTableIndexDAO);
	}
	
	@Test
	public void testAddRowLevelFilter() throws ParseException, EmptyResultException{
		SqlQuery query = new SqlQuery("select i0 from "+tableId, models);
		// call under test
		SqlQuery result = manager.addRowLevelFilter(user, query, mockTableIndexDAO);
		assertNotNull(result);
		assertEquals("SELECT i0 FROM syn123 WHERE _C999_ IN ( 444 )", result.getModel().toSql());
	}
	
	
	////////////////////////////
	// runFacetQueries() Tests
	////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testRunFacetQueriesNullFacetModel(){
		manager.runFacetQueries(null, mockTableIndexDAO);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testRunFacetQueriesFacetColumns(){
		manager.runFacetQueries(Mockito.mock(FacetModel.class), null);
	}	
	
	@Test
	public void testRunFacetQueries(){
		//setup
		FacetModel mockFacetModel = Mockito.mock(FacetModel.class);
		FacetTransformer mockTransformer1 = Mockito.mock(FacetTransformerValueCounts.class);
		FacetTransformer mockTransformer2 = Mockito.mock(FacetTransformerRange.class);
		SqlQuery mockSql1 = Mockito.mock(SqlQuery.class);
		SqlQuery mockSql2 = Mockito.mock(SqlQuery.class);
		RowSet rs1 = new RowSet();
		RowSet rs2 = new RowSet();
		FacetColumnResultValues result1 = new FacetColumnResultValues();
		FacetColumnResultRange result2 = new FacetColumnResultRange();
		
		
		when(mockTransformer1.getFacetSqlQuery()).thenReturn(mockSql1);
		when(mockTransformer2.getFacetSqlQuery()).thenReturn(mockSql2);
		when(mockTableIndexDAO.query(null, mockSql1)).thenReturn(rs1);
		when(mockTableIndexDAO.query(null, mockSql2)).thenReturn(rs2);
		when(mockTransformer1.translateToResult(rs1)).thenReturn(result1);
		when(mockTransformer2.translateToResult(rs2)).thenReturn(result2);
		List<FacetTransformer> transformersList = Arrays.asList(mockTransformer1, mockTransformer2);
		when(mockFacetModel.getFacetInformationQueries()).thenReturn(transformersList);
		
		//call method
		List<FacetColumnResult> results = manager.runFacetQueries(mockFacetModel, mockTableIndexDAO);
		
		//verify and assert
		verify(mockFacetModel).getFacetInformationQueries();
		verify(mockTransformer1).getFacetSqlQuery();
		verify(mockTransformer2).getFacetSqlQuery();
		verify(mockTableIndexDAO).query(null, mockSql1);
		verify(mockTableIndexDAO).query(null, mockSql2);
		verify(mockTransformer1).translateToResult(rs1);
		verify(mockTransformer2).translateToResult(rs2);
		
		
		assertEquals(2, results.size());
		assertEquals(result1, results.get(0));
		assertEquals(result2, results.get(1));
		
		verifyNoMoreInteractions(mockTableIndexDAO, mockFacetModel,mockTransformer1, mockTransformer2);

	}
	
	private RowSet createRowSetForTest(List<String> headerNames, List<String>... rowValues){
		RowSet rowSet = new RowSet();
		List<SelectColumn> headerObjects = new ArrayList<>();
		
		//select column for first row
		for(String headerName: headerNames){
			SelectColumn headerObj = new SelectColumn();
			headerObj.setName(headerName);
			headerObjects.add(headerObj);
		}
		rowSet.setHeaders(headerObjects);
		rowSet.setRows(new ArrayList<Row>());
		
		List<Row> rows = new ArrayList<Row>();
		for(List<String> rowValue : rowValues){
			Row row = new Row();
			row.setValues(rowValue);
			rows.add(row);
		}
		rowSet.setRows(rows);
		return rowSet;
	}

}

