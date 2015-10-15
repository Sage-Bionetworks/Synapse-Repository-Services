package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.util.csv.CSVWriterStreamProxy;
import org.sagebionetworks.util.csv.CsvNullReader;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableWorkerIntegrationTest {

	/**
	 * 
	 */
	public static final int MAX_WAIT_MS = 1000 * 60 * 3;
	
	@Autowired
	StackConfiguration config;
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	CertifiedUserManager certifiedUserManager;
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	AccessRequirementManager accessRequirementManager;
	@Autowired
	AccessApprovalManager accessApprovalManager;
	@Autowired
	EntityPermissionsManager entityPermissionsManager;
	@Autowired
	TableRowCache tableRowCache;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	FileHandleManager fileHandleManager;
	
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired
	private IdGenerator idGenerator;

	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	private String tableId;
	
	ProgressCallback<Long> mockPprogressCallback;
	
	private int oldMaxBytesPerRequest;

	private List<UserInfo> users = Lists.newArrayList();

	private String projectId;

	@Before
	public void before() throws Exception {
		mockPprogressCallback = Mockito.mock(ProgressCallback.class);
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		this.tableId = null;
		// Start with an empty database
		this.tableConnectionFactory.dropAllTablesForAllConnections();
		tableRowCache.truncateAllData();
		oldMaxBytesPerRequest = (Integer) ReflectionTestUtils.getField(getTargetObject(tableRowManager), "maxBytesPerRequest");
	}
	
	@After
	public void after() throws Exception {
		if (config.getTableEnabled()) {
			if (tableId != null) {
				tableRowManager.deleteAllRows(tableId);
				columnManager.unbindAllColumnsAndOwnerFromObject(tableId);
				entityManager.deleteEntity(adminUserInfo, tableId);
			}
			if (projectId != null) {
				entityManager.deleteEntity(adminUserInfo, projectId);
			}
			// cleanup
			tableRowCache.truncateAllData();
			columnManager.truncateAllColumnData(adminUserInfo);
			// Drop all data in the index database
			this.tableConnectionFactory.dropAllTablesForAllConnections();
			ReflectionTestUtils.setField(getTargetObject(tableRowManager), "maxBytesPerRequest", oldMaxBytesPerRequest);
			for (UserInfo user : users) {
				try {
					userManager.deletePrincipal(adminUserInfo, user.getId());
				} catch (Exception e) {
				}
			}
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: columnModels){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 2));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 2));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		System.out.println("testRoundTrip");
		System.out.println(queryResult);
		assertNotNull(queryResult);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(tableId, queryResult.getQueryResults().getTableId());
		assertNotNull(queryResult.getQueryResults().getHeaders());
		assertEquals(schema.size(), queryResult.getQueryResults().getHeaders().size());
		assertNotNull(queryResult.getQueryResults().getRows());
		assertEquals(6, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getQueryResults().getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results", referenceSet.getEtag(),
				queryResult.getQueryResults().getEtag());
		assertEquals("The etag should also match the rereferenceSet.etag", referenceSet.getEtag(), queryResult.getQueryResults().getEtag());

		@SuppressWarnings("unchecked")
		RowSet expectedRowSet = tableRowManager.getRowSet(tableId, referenceSet.getRows().get(0).getVersionNumber(),
				TableModelUtils.createColumnModelColumnMapper(schema, false));
		assertEquals(expectedRowSet, queryResult.getQueryResults());
	}

	@Test
	public void testLimitOffset() throws Exception {
		// Create one column of each type
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 6));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);
		rowSet = tableRowManager.getCellValues(adminUserInfo, tableId, referenceSet,
				TableModelUtils.createColumnModelColumnMapper(schema, false));
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 7L);
		assertEquals(6, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 0, 6, queryResult.getQueryResults());

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 6L);
		assertEquals(6, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 0, 6, queryResult.getQueryResults());

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 5L);
		assertEquals(5, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 0, 5, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql, null, 5L, 1L, true, false, true).getFirst();
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 5, 1, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql, null, 5L, 2L, true, false, true).getFirst();
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 5, 1, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql + " limit 2 offset 3", null, 0L, 8L, true,
				false, true).getFirst();
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 3, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql + " limit 8 offset 2", null, 2L, 2L, true,
				false, true).getFirst();
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 4, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql + " limit 8 offset 3", null, 2L, 2L, true,
				false, true).getFirst();
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 5, 1, queryResult.getQueryResults());

		ReflectionTestUtils.setField(getTargetObject(tableRowManager), "maxBytesPerRequest", TableModelUtils.calculateMaxRowSize(schema) * 2);
		queryResult = tableRowManager.query(adminUserInfo, sql, null, 0L, 5L, true, false, true).getFirst();
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getNextPageToken());
		compareValues(rowSet, 0, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.queryNextPage(adminUserInfo, queryResult.getNextPageToken());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getNextPageToken());
		compareValues(rowSet, 2, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.queryNextPage(adminUserInfo, queryResult.getNextPageToken());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 4, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.query(adminUserInfo, sql + " limit 3", null, 0L, 100L, true, false, true).getFirst();
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getNextPageToken());
		compareValues(rowSet, 0, 2, queryResult.getQueryResults());

		queryResult = tableRowManager.queryNextPage(adminUserInfo, queryResult.getNextPageToken());
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertNull(queryResult.getNextPageToken());
		compareValues(rowSet, 2, 1, queryResult.getQueryResults());
	}

	@Test
	public void testSorting() throws Exception {
		// Create one column of each type
		schema = Lists.newArrayList(
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "name", ColumnType.STRING)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col1", ColumnType.INTEGER)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col2", ColumnType.INTEGER)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col3", ColumnType.INTEGER)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "a", "1", "10", "3"),
				TableModelTestUtils.createRow(null, null, "b", "2", "11", "1"),
				TableModelTestUtils.createRow(null, null, "c", "3", "11", "2")));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select name from " + tableId + " order by col1";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		compareValues(new String[] { "a", "b", "c" }, queryResult.getQueryResults());

		SortItem sort1 = new SortItem();
		sort1.setColumn("col1");
		sort1.setDirection(SortDirection.DESC);
		SortItem sort2 = new SortItem();
		sort2.setColumn("col2");
		sort2.setDirection(SortDirection.DESC);

		queryResult = waitForConsistentQuery(adminUserInfo, sql, Lists.newArrayList(sort2), null);
		compareValues(new String[] { "b", "c", "a" }, queryResult.getQueryResults());

		queryResult = waitForConsistentQuery(adminUserInfo, sql, Lists.newArrayList(sort2, sort1), null);
		compareValues(new String[] { "c", "b", "a" }, queryResult.getQueryResults());

		queryResult = waitForConsistentQuery(adminUserInfo, sql, Lists.newArrayList(sort1), null);
		compareValues(new String[] { "c", "b", "a" }, queryResult.getQueryResults());
	}

	@Test
	public void testDoubleSetFunctions() throws Exception {
		// Create one column of each type
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(null, "number", ColumnType.DOUBLE)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "1.5"), TableModelTestUtils.createRow(null, null, "2.0"),
				TableModelTestUtils.createRow(null, null, "2.0"), TableModelTestUtils.createRow(null, null, "4.5"),
				TableModelTestUtils.createRow(null, null, "2.0")));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select avg(number) from " + tableId;
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
		compareValues(new String[] { "2.4" }, queryResult.getQueryResults());

		sql = "select sum(number) as ss from " + tableId + " group by number order by ss asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
		assertEquals("ss", queryResult.getQueryResults().getHeaders().get(0).getName());
		compareValues(new String[] { "1.5", "4.5", "6" }, queryResult.getQueryResults());

		sql = "select sum(number) from " + tableId + " group by number order by sum(number) asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
		assertEquals("SUM(number)", queryResult.getQueryResults().getHeaders().get(0).getName());
		compareValues(new String[] { "1.5", "4.5", "6" }, queryResult.getQueryResults());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullNextPageToken() throws Exception {
		tableRowManager.queryNextPage(adminUserInfo, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyNextPageToken() throws Exception {
		tableRowManager.queryNextPage(adminUserInfo, new QueryNextPageToken());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidNextPageToken() throws Exception {
		QueryNextPageToken token = new QueryNextPageToken();
		token.setToken("<invalid/>");
		tableRowManager.queryNextPage(adminUserInfo, token);
	}

	@Test
	public void testLimitWithCountQueries() throws Exception {
		// Create one column of each type
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 10));
		assertEquals(ColumnType.STRING, schema.get(0).getColumnType());
		// make sure we can order by first column (which should be STRING) to make row 0 come first
		rowSet.getRows().get(0).getValues().set(0, "!!" + rowSet.getRows().get(0).getValues().get(0));
		// and make grouping return 9 rows
		rowSet.getRows().get(4).getValues().set(0, rowSet.getRows().get(0).getValues().get(0));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " limit 5";
		QueryResultBundle queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals(5, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(5L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 3 offset 1";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals(3, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(3L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 5 offset 3";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals(5, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(5L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 5 offset 8";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(2L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 5";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, 0L, 3L);
		assertEquals(3, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(5L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 3 offset 1";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, 2L, 1L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(3L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 5 offset 3";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, 6L, 3L);
		assertEquals(0, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(5L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 5 offset 8";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(2L, queryResultBundle.getQueryCount().longValue());

		sql = "select * from " + tableId + " limit 8 offset 12";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L);
		assertEquals(0, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(0L, queryResultBundle.getQueryCount().longValue());

		sql = "select count(*) from " + tableId + " limit 100";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("10", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals(1L, queryResultBundle.getQueryCount().longValue());

		sql = "select max(" + schema.get(0).getName() + ") from " + tableId + " limit 100";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(1L, queryResultBundle.getQueryCount().longValue());

		String groupSql = " group by " + schema.get(0).getName();
		String orderSql = " order by " + schema.get(0).getName() + " asc";

		sql = "select * from " + tableId + groupSql + orderSql + " limit 100";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals(rowSet.getRows().get(0).getValues().get(0), queryResultBundle.getQueryResult().getQueryResults().getRows().get(0)
				.getValues().get(0));
		assertEquals(9L, queryResultBundle.getQueryCount().longValue());

		sql = "select count(" + schema.get(0).getName() + ") from " + tableId + groupSql + orderSql + " limit 100";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L);
		assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("1", queryResultBundle.getQueryResult().getQueryResults().getRows().get(1).getValues().get(0));
		assertEquals(9L, queryResultBundle.getQueryCount().longValue());

		sql = "select count(*) as c from " + tableId + groupSql + " order by c desc limit 100";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L);
		assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("1", queryResultBundle.getQueryResult().getQueryResults().getRows().get(1).getValues().get(0));
		assertEquals(9L, queryResultBundle.getQueryCount().longValue());

		sql = "select count(*) as c from " + tableId + groupSql + " order by c desc limit 1";
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L);
		assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals(1L, queryResultBundle.getQueryCount().longValue());
	}

	@Test
	public void testColumnOrderWithQueries() throws Exception {
		// Create one column of each type
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 10));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		QueryResultBundle queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		for (int i = 0; i < queryResultBundle.getSelectColumns().size(); i++) {
			assertEquals(schema.get(i).getName(), queryResultBundle.getSelectColumns().get(i).getName());
			assertEquals(schema.get(i).getId(), queryResultBundle.getQueryResult().getQueryResults().getHeaders().get(i).getId());
		}

		sql = "select i1, i2 from " + tableId;
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals("i1", queryResultBundle.getSelectColumns().get(0).getName());
		assertEquals("i2", queryResultBundle.getSelectColumns().get(1).getName());

		sql = "select i3, i1 from " + tableId;
		queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		assertEquals("i3", queryResultBundle.getSelectColumns().get(0).getName());
		assertEquals("i1", queryResultBundle.getSelectColumns().get(1).getName());

		PartialRowSet rowsToDelete = new PartialRowSet();
		rowsToDelete.setTableId(tableId);
		List<PartialRow> deleteRows = Lists.newArrayList();
		for (RowReference row : referenceSet.getRows()) {
			PartialRow partialRow = new PartialRow();
			partialRow.setRowId(row.getRowId());
			deleteRows.add(partialRow);
		}
		// PLFM-2965 removing all rows means query result headers is not filled out
		// rowsToDelete.setRows(deleteRows);
		// tableRowManager.appendPartialRows(adminUserInfo, tableId, schema, rowsToDelete);
		// sql = "select * from " + tableId;
		// queryResultBundle = waitForConsistentQueryBundle(adminUserInfo, sql, null, null);
		// assertEquals(0, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		// for (int i = 0; i < queryResultBundle.getSelectColumns().size(); i++) {
		// assertEquals(schema.get(i), queryResultBundle.getSelectColumns().get(i));
		// assertEquals(schema.get(i).getId(),
		// queryResultBundle.getQueryResult().getQueryResults().getHeaders().get(i));
		// }
	}

	/**
	 * Test if things work if the table index is not being build, which can happen for example after a migration
	 */
	@Test
	public void testRoundTripAfterMigrate() throws Exception {
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		RowSet rowSet = createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), rowSet, mockPprogressCallback);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertEquals(2, queryResult.getQueryResults().getRows().size());

		// reset table index
		tableStatusDAO.clearAllTableState();
		tableConnectionFactory.dropAllTablesForAllConnections();

		// now we still should get the index taken care of
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertEquals(2, queryResult.getQueryResults().getRows().size());
	}

	/**
	 * Test if things work after a migration where there is no table status, but the index and current index have to be
	 * built
	 */
	@Test
	public void testAfterMigrate() throws Exception {
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		RowSet rowSet = createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), rowSet, mockPprogressCallback);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertEquals(2, queryResult.getQueryResults().getRows().size());

		// reset table index
		tableStatusDAO.clearAllTableState();
		tableConnectionFactory.dropAllTablesForAllConnections();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.TABLE);
		message.setObjectId(KeyFactory.stringToKey(tableId).toString());
		message.setObjectEtag(UUID.randomUUID().toString());
		message = changeDAO.replaceChange(message);
		// and pretend we just created it
		repositoryMessagePublisher.publishToTopic(message);

		final TableIndexDAO tableIndexDAO = tableConnectionFactory.getConnection(tableId);
		assertTrue("Index table was not created", TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				return tableIndexDAO.getRowCountForTable(tableId) != null;
			}
		}));
		assertTrue("Current index was not created", TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				return tableRowCache.getCurrentVersionNumbers(KeyFactory.stringToKey(tableId), 0, 1000).size() == 2;
			}
		}));

		// now we still should get the index taken care of
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertEquals(2, queryResult.getQueryResults().getRows().size());
	}

	@Test
	public void testPartialUpdateRoundTrip() throws Exception {
		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType(true);
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : columnModels) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 10);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 3));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 3));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(16, queryResult.getQueryResults().getRows().size());

		RowSet expectedRowSet = tableRowManager.getRowSet(tableId, referenceSet.getRows().get(0).getVersionNumber(),
				TableModelUtils.createColumnModelColumnMapper(schema, false));

		// apply updates to expected and actual
		List<PartialRow> partialRows = Lists.newArrayList(); 
		for(int i = 0; i < 16; i++){
			partialRows.add(TableModelTestUtils.updatePartialRow(schema, expectedRowSet.getRows().get(i), i));
			expectedRowSet.getRows().get(i).setVersionNumber(1L);
		}
		rows = TableModelTestUtils.createExpectedFullRows(schema, 5);
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setRowId(16L + i);
			rows.get(i).setVersionNumber(1L);
		}
		expectedRowSet.getRows().addAll(rows);
		partialRows.addAll(TableModelTestUtils.createPartialRows(schema, 5));

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setRows(partialRows);
		partialRowSet.setTableId(tableId);
		tableRowManager
				.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), partialRowSet, mockPprogressCallback);

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		// we couldn't know the etag in advance
		expectedRowSet.setEtag(queryResult.getQueryResults().getEtag());
		assertEquals(expectedRowSet.toString(), queryResult.getQueryResults().toString());
		assertEquals(expectedRowSet, queryResult.getQueryResults());
	}

	@Test
	public void testRemoveColumn() throws Exception {
		// Create one column
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(null, "col1", ColumnType.STRING)));
		List<Long> headers = TableModelUtils.getIds(schema);

		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

		// add data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "a")));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(1, queryResult.getQueryResults().getRows().get(0).getValues().size());

		// add new column
		schema.add(columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col2", ColumnType.STRING)));
		headers = TableModelUtils.getIds(schema);
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		String newColumnId = ""+headers.get(headers.size()-1);
		// set data on new column
		
		PartialRow firstRow = TableModelTestUtils.createPartialRow(queryResult.getQueryResults().getRows().get(0).getRowId(), newColumnId, "b");
		PartialRowSet firstRowChange = new PartialRowSet();
		firstRowChange.setRows(Lists.newArrayList(firstRow));
		firstRowChange.setTableId(tableId);
		tableRowManager.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				firstRowChange, mockPprogressCallback);

		// wait for table to be available
		sql = "select * from " + tableId;
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(2, queryResult.getQueryResults().getRows().get(0).getValues().size());

		// remove column a
		schema.remove(0);
		headers = TableModelUtils.getIds(schema);
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

		// wait for table to be available
		sql = "select * from " + tableId;
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(1, queryResult.getQueryResults().getRows().get(0).getValues().size());

		// set data again
		firstRow = TableModelTestUtils.createPartialRow(queryResult.getQueryResults().getRows().get(0).getRowId(), newColumnId, "c");
		firstRowChange = new PartialRowSet();
		firstRowChange.setRows(Lists.newArrayList(firstRow));
		firstRowChange.setTableId(tableId);
		tableRowManager.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				firstRowChange, mockPprogressCallback);

	}

	@Test
	public void testDates() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "coldate", ColumnType.DATE)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		DateFormat dateTimeInstance = new SimpleDateFormat("yyy-M-d h:mm");
		dateTimeInstance.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date[] dates = new Date[] { dateTimeInstance.parse("2014-2-3 2:12"), dateTimeInstance.parse("2014-2-3 3:41"),
				dateTimeInstance.parse("2015-2-3 3:41"), dateTimeInstance.parse("2016-2-3 3:41") };
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, dates.length);
		for (int i = 0; i < dates.length; i++) {
			rows.get(i).getValues().set(0, "" + dates[i].getTime());
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " where coldate between '2014-2-3 3:00' and '2016-1-1' order by coldate asc";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertEquals("2014-2-3 3:41",
				dateTimeInstance.format(new Date(Long.parseLong(queryResult.getQueryResults().getRows().get(0).getValues().get(0)))));
		assertEquals("2015-2-3 3:41",
				dateTimeInstance.format(new Date(Long.parseLong(queryResult.getQueryResults().getRows().get(1).getValues().get(0)))));

		// Again, but now with longs
		sql = "select * from " + tableId + " where coldate between " + dateTimeInstance.parse("2014-2-3 3:00").getTime() + " and "
				+ dateTimeInstance.parse("2016-1-1 0:00").getTime() + " order by coldate asc";
		QueryResult queryResult2 = waitForConsistentQuery(adminUserInfo, sql, null, 8L);
		assertEquals(queryResult, queryResult2);
	}

	@Test
	public void testDoubles() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "coldouble", ColumnType.DOUBLE)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		Double[] doubles = { Double.NaN, null, Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, 0.0, 1.0, 3e42, Double.MAX_VALUE,
				Double.POSITIVE_INFINITY };
		String[] expected = { "NaN", null, "-Infinity", "-1.7976931348623157e308", "-1", "0", "1", "3e42", "1.7976931348623157e308",
				"Infinity" };
		assertEquals(doubles.length, expected.length);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, doubles.length);
		for (int i = 0; i < doubles.length; i++) {
			rows.get(i).getValues().set(0, doubles[i] == null ? null : doubles[i].toString());
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by coldouble ASC";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(doubles.length, queryResult.getQueryResults().getRows().size());
		for (int i = 0; i < doubles.length; i++) {
			assertEquals(expected[i], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
		}

		sql = "select * from " + tableId + " order by coldouble DESC";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(doubles.length, queryResult.getQueryResults().getRows().size());
		for (int i = 0; i < doubles.length; i++) {
			assertEquals(expected[doubles.length - i - 1], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
		}

		sql = "select * from " + tableId + " where isNaN(coldouble)";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals("NaN", queryResult.getQueryResults().getRows().get(0).getValues().get(0));

		sql = "select * from " + tableId + " where isInfinity(coldouble) order by coldouble";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertEquals("-Infinity", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("Infinity", queryResult.getQueryResults().getRows().get(1).getValues().get(0));

		sql = "select avg(coldouble) from " + tableId
				+ " where not isNaN(coldouble) and not isInfinity(coldouble) and coldouble is not null order by coldouble";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, null);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals("0", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
	}

	@Test
	public void testBooleans() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "colbool", ColumnType.BOOLEAN)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

		String[] booleans = new String[] { null, "", "true", "false", "True", "False", "TRUE", "FALSE", Boolean.TRUE.toString(),
				Boolean.FALSE.toString(), Boolean.FALSE.toString() };
		String[] expectedOut = new String[] { null, null, "true", "false", "true", "false", "true", "false", "true", "false", "false" };
		int expectedTrueCount = 4;
		int expectedFalseCount = 5;
		int expectedNullCount = 2;
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, booleans.length);
		for (int i = 0; i < booleans.length; i++) {
			rows.get(i).getValues().set(0, booleans[i]);
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		String[] failingBooleans = new String[] { "1", "0", "2", "falseish", "nottrue" };
		for (String failingBoolean : failingBooleans) {
			List<Row> failRow = TableModelTestUtils.createRows(schema, 1);
			failRow.get(0).getValues().set(0, failingBoolean);

			RowSet failRowSet = new RowSet();
			failRowSet.setRows(failRow);
			failRowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
			failRowSet.setTableId(tableId);
			try {
				tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), failRowSet, mockPprogressCallback);
				fail("Should have rejected as boolean: " + failingBoolean);
			} catch (IllegalArgumentException e) {
			}
		}

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id asc";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(expectedOut.length, queryResult.getQueryResults().getRows().size());
		for (int i = 0; i < expectedOut.length; i++) {
			assertEquals(expectedOut[i], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
		}

		sql = "select * from " + tableId + " where colbool is true order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool is false order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool is not true order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedFalseCount + expectedNullCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool is not false order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedTrueCount + expectedNullCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool = true order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool = false order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool <> true order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());

		sql = "select * from " + tableId + " where colbool <> false order by row_id asc";
		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 20L);
		assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());
	}

	@Test
	public void testReplaceAndDeleteRoundTrip() throws Exception {
		// Create one column of each type
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("col1");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);

		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 4);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);
		assertEquals(4, referenceSet.getRows().size());

		rowSet.setEtag(referenceSet.getEtag());
		for (int i = 0; i < 4; i++) {
			rows.get(i).setRowId(referenceSet.getRows().get(i).getRowId());
			rows.get(i).setVersionNumber(referenceSet.getRows().get(i).getVersionNumber());
		}

		List<Row> updateRows = Lists.newArrayList(rows);
		updateRows.remove(3);

		TableModelTestUtils.updateRow(schema, updateRows.get(0), 333);
		TableModelTestUtils.updateRow(schema, updateRows.get(1), 444);
		TableModelTestUtils.updateRow(schema, updateRows.get(2), 555);
		rowSet.setRows(updateRows);
		RowReferenceSet referenceSet2 = tableRowManager.appendRows(adminUserInfo, tableId,
				TableModelUtils.createColumnModelColumnMapper(schema, false), rowSet, mockPprogressCallback);
		assertEquals(3, referenceSet2.getRows().size());

		RowSelection rowsToDelete = new RowSelection();
		rowsToDelete.setEtag(referenceSet2.getEtag());
		rowsToDelete.setRowIds(Lists.newArrayList(referenceSet2.getRows().get(1).getRowId(), referenceSet.getRows().get(3).getRowId()));

		referenceSet = tableRowManager.deleteRows(adminUserInfo, tableId, rowsToDelete);
		assertEquals(2, referenceSet.getRows().size());

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals("TableId: " + tableId, 2, queryResult.getQueryResults().getRows().size());
		assertEquals("updatestring333", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("updatestring555", queryResult.getQueryResults().getRows().get(1).getValues().get(0));
	}

	@Test
	public void testPartialUpdate() throws Exception {
		// four columns, two with default value
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("col1");
		cm.setColumnType(ColumnType.STRING);
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setName("col2");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setDefaultValue("default");
		cm.setName("col3");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		cm.setName("col4");
		schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

		// Now add some data
		List<Row> rows = Lists.newArrayList();
		for (int i = 0; i < 6; i++) {
			rows.add(TableModelTestUtils.createRow(null, null, "something", null, "something", null));
		}
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(6, queryResult.getQueryResults().getRows().size());
		for (int i = 0; i < 6; i++) {
			assertEquals(Lists.newArrayList("something", null, "something", "default"), queryResult.getQueryResults().getRows().get(i)
					.getValues());
		}

		// append
		PartialRow partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(null);
		partialRowAppend.setValues(ImmutableMap.<String, String> builder().put(schema.get(0).getId(), "something")
				.put(schema.get(2).getId(), "something").build());

		// update null columns
		PartialRow partialRowUpdateNull = new PartialRow();
		partialRowUpdateNull.setRowId(queryResult.getQueryResults().getRows().get(0).getRowId());
		partialRowUpdateNull.setValues(ImmutableMap.<String, String> builder().put(schema.get(1).getId(), "other")
				.put(schema.get(3).getId(), "other").build());

		// update non null columns
		PartialRow partialRowUpdateNonNull = new PartialRow();
		partialRowUpdateNonNull.setRowId(queryResult.getQueryResults().getRows().get(1).getRowId());
		partialRowUpdateNonNull.setValues(ImmutableMap.<String, String> builder().put(schema.get(0).getId(), "other")
				.put(schema.get(2).getId(), "other").build());

		// update nothing
		PartialRow partialRowUpdateNothing = new PartialRow();
		partialRowUpdateNothing.setRowId(queryResult.getQueryResults().getRows().get(2).getRowId());
		partialRowUpdateNothing.setValues(ImmutableMap.<String, String> builder().build());

		// update with nulls
		PartialRow partialRowUpdateNulls = new PartialRow();
		partialRowUpdateNulls.setRowId(queryResult.getQueryResults().getRows().get(3).getRowId());
		Map<String, String> values = Maps.newHashMap();
		values.put(schema.get(0).getId(), null);
		values.put(schema.get(1).getId(), null);
		values.put(schema.get(2).getId(), null);
		values.put(schema.get(3).getId(), null);
		partialRowUpdateNulls.setValues(values);

		// append for no change
		PartialRow partialRowAppendForDelete1 = new PartialRow();
		partialRowAppendForDelete1.setRowId(queryResult.getQueryResults().getRows().get(4).getRowId());
		partialRowAppendForDelete1.setValues(Collections.<String, String> emptyMap());

		// append for deletion
		PartialRow partialRowAppendForDelete2 = new PartialRow();
		partialRowAppendForDelete2.setRowId(queryResult.getQueryResults().getRows().get(5).getRowId());
		partialRowAppendForDelete2.setValues(null);

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend, partialRowAppendForDelete1, partialRowAppendForDelete2,
				partialRowUpdateNull, partialRowUpdateNonNull, partialRowUpdateNothing, partialRowUpdateNulls));
		tableRowManager
				.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), partialRowSet, mockPprogressCallback);

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(6, queryResult.getQueryResults().getRows().size());
		// update null columns
		assertEquals(Lists.newArrayList("something", "other", "something", "other"), queryResult.getQueryResults().getRows().get(0)
				.getValues());
		// update non null columns
		assertEquals(Lists.newArrayList("other", null, "other", "default"), queryResult.getQueryResults().getRows().get(1).getValues());
		// update nothing
		assertEquals(Lists.newArrayList("something", null, "something", "default"), queryResult.getQueryResults().getRows().get(2)
				.getValues());
		// update with nulls
		assertEquals(Lists.newArrayList(null, null, "default", "default"), queryResult.getQueryResults().getRows().get(3).getValues());
		// no change
		assertEquals(Lists.newArrayList("something", null, "something", "default"), queryResult.getQueryResults().getRows().get(4)
				.getValues());
		// append
		assertEquals(Lists.newArrayList("something", null, "something", "default"), queryResult.getQueryResults().getRows().get(5)
				.getValues());
	}

	@Test
	public void testUpdateFilehandles() throws Exception {
		// four columns, two with default value
		schema = new LinkedList<ColumnModel>();
		for (int i = 0; i < 5; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.FILEHANDLEID);
			cm.setName("col" + i);
			schema.add(columnManager.createColumnModel(adminUserInfo, cm));
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

		// Now add a file handle
		ExternalFileHandle fileHandle1 = new ExternalFileHandle();
		fileHandle1.setFileName("file1");
		fileHandle1.setExternalURL("http://not.com/file1");
		fileHandle1 = fileHandleManager.createExternalFileHandle(adminUserInfo, fileHandle1);
		ExternalFileHandle fileHandle2 = new ExternalFileHandle();
		fileHandle2.setFileName("file2");
		fileHandle2.setExternalURL("http://not.com/file2");
		fileHandle2 = fileHandleManager.createExternalFileHandle(adminUserInfo, fileHandle2);

		List<Row> rows = Lists.newArrayList(TableModelTestUtils.createRow(null, null, fileHandle1.getId(), fileHandle1.getId(),
				fileHandle1.getId(), null, null));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		referenceSet = tableRowManager.appendRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet, mockPprogressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(Lists.newArrayList(fileHandle1.getId(), fileHandle1.getId(), fileHandle1.getId(), null, null), queryResult
				.getQueryResults().getRows().get(0).getValues());

		// append
		PartialRow partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(referenceSet.getRows().get(0).getRowId());
		partialRowAppend.setValues(buildMap(schema.get(0).getId(), fileHandle2.getId(), schema.get(1).getId(), fileHandle1.getId(), schema
				.get(2).getId(), null, schema.get(3).getId(), fileHandle2.getId()));

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend));
		tableRowManager
				.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), partialRowSet, mockPprogressCallback);

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(Lists.newArrayList(fileHandle2.getId(), fileHandle1.getId(), null, fileHandle2.getId(), null), queryResult
				.getQueryResults().getRows().get(0).getValues());

		// append
		partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(referenceSet.getRows().get(0).getRowId());
		partialRowAppend.setValues(buildMap(schema.get(0).getId(), fileHandle1.getId(), schema.get(1).getId(), fileHandle1.getId(), schema
				.get(2).getId(), fileHandle1.getId(), schema.get(3).getId(), fileHandle1.getId()));

		partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend));
		tableRowManager
				.appendPartialRows(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), partialRowSet, mockPprogressCallback);

		queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 100L);
		assertEquals(1, queryResult.getQueryResults().getRows().size());
		assertEquals(Lists.newArrayList(fileHandle1.getId(), fileHandle1.getId(), fileHandle1.getId(), fileHandle1.getId(), null),
				queryResult.getQueryResults().getRows().get(0).getValues());
	}

	Map<String, String> buildMap(String... keyValues) {
		Map<String, String> map = Maps.newHashMap();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	@Ignore // This is a very slow test that pushes massive amounts of data so it is disabled.
	@Test
	public void testAppendRowsAtScale() throws Exception {
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 500000);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		String etag = tableRowManager.appendRowsAsStream(adminUserInfo, tableId,
				TableModelUtils.createColumnModelColumnMapper(schema, false),
				rowSet.getRows().iterator(), null, null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		String sql = "select * from " + tableId + "";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 2L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(tableId, queryResult.getQueryResults().getTableId());
		assertNotNull(queryResult.getQueryResults().getHeaders());
		assertEquals(schema.size(), queryResult.getQueryResults().getHeaders().size());
		assertNotNull(queryResult.getQueryResults().getRows());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getQueryResults().getEtag());
		assertEquals("The etag for the last applied change set should be set for the status and the results", etag, queryResult
				.getQueryResults().getEtag());
		assertEquals("The etag should also match the rereferenceSet.etag", referenceSet.getEtag(), queryResult.getQueryResults().getEtag());
	}

	@Test
	public void testColumnNameRange() throws Exception {
		// Create one column of each type
		String specialChars = "Specialchars~!@#$%^^&*()_+|}{:?></.,;'[]\'";
		List<ColumnModel> temp = TableModelTestUtils.createColumsWithNames("Has Space", "a", "A", specialChars);
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 10);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), rowSet
				.getRows()
				.iterator(), null, null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Query for the results
		String sql = "select A, a, \"Has Space\",\"" + specialChars + "\" from " + tableId + "";
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 2L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(tableId, queryResult.getQueryResults().getTableId());
		assertNotNull(queryResult.getQueryResults().getHeaders());
		assertEquals(4, queryResult.getQueryResults().getHeaders().size());
		assertEquals(headers.get(0).toString(), queryResult.getQueryResults().getHeaders().get(2).getId());
		assertEquals(headers.get(1).toString(), queryResult.getQueryResults().getHeaders().get(1).getId());
		assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
		assertEquals(headers.get(3).toString(), queryResult.getQueryResults().getHeaders().get(3).getId());
		assertNotNull(queryResult.getQueryResults().getRows());
		assertEquals(2, queryResult.getQueryResults().getRows().size());
		assertNotNull(queryResult.getQueryResults().getEtag());

		try {
			waitForConsistentQuery(adminUserInfo, "select A, Has Space from " + tableId, null, 100L);
			fail("not acceptible sql");
		} catch (IllegalArgumentException e) {
		}

		// select a string literal
		queryResult = waitForConsistentQuery(adminUserInfo, "select A, 'Has Space' from " + tableId, null, 100L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(2, queryResult.getQueryResults().getHeaders().size());
		assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
		assertEquals("Has Space", queryResult.getQueryResults().getHeaders().get(1).getName());
		assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("Has Space", queryResult.getQueryResults().getRows().get(0).getValues().get(1));

		queryResult = waitForConsistentQuery(adminUserInfo, "select A, \"Has Space\" from " + tableId, null, 100L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(2, queryResult.getQueryResults().getHeaders().size());
		assertEquals(headers.get(0).toString(), queryResult.getQueryResults().getHeaders().get(1).getId());
		assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
		assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("string0", queryResult.getQueryResults().getRows().get(0).getValues().get(1));

		queryResult = waitForConsistentQuery(adminUserInfo, "select A, \"Has Space\" as HasSpace from " + tableId, null, 100L);
		assertNotNull(queryResult.getQueryResults());
		assertEquals(2, queryResult.getQueryResults().getHeaders().size());
		assertEquals(headers.get(0).toString(), queryResult.getQueryResults().getHeaders().get(1).getId());
		assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
		assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		assertEquals("string0", queryResult.getQueryResults().getRows().get(0).getValues().get(1));
	}
	
	/**
	 * There were several issue related to creating tables with no columns an now rows.  This test validates that such tables are supported.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws Exception 
	 */
	@Test
	public void testNoColumnsNoRows() throws Exception {
		// Create a table with no columns.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(null);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, null, tableId, true);
		// We should be able to query
		String sql = "select * from " + tableId;
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 1L);
		assertNotNull(queryResult.getQueryResults());
		assertNull(queryResult.getQueryResults().getEtag());
		assertEquals(tableId, queryResult.getQueryResults().getTableId());
		assertTrue(queryResult.getQueryResults().getHeaders() == null || queryResult.getQueryResults().getHeaders().isEmpty());
		assertTrue(queryResult.getQueryResults().getRows() == null || queryResult.getQueryResults().getRows().isEmpty());
	}
	
	/**
	 * There were several issue related to creating tables with no columns an now rows.  This test validates that such tables are supported.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws Exception 
	 */
	@Test
	public void testNoRows() throws Exception {
		// Create one column of each type
		List<ColumnModel> temp = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// We should be able to query
		String sql = "select * from " + tableId;
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, sql, null, 1L);
		assertNotNull(queryResult.getQueryResults());
		assertNull(queryResult.getQueryResults().getEtag());
		assertEquals(tableId, queryResult.getQueryResults().getTableId());
		assertTrue("TableId: " + tableId, queryResult.getQueryResults().getRows() == null
				|| queryResult.getQueryResults().getRows().isEmpty());
	}
	
	/**
	 * This test will first create a table from an input CSV, then stream all of the data from the table
	 * to an output CSV.  The output CSV is then updated and then used to update the table.
	 * The output date is then stream again, but without the headers so that it can be used to create
	 * a copy of original table.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyAndUpdateTableFromCSV() throws Exception {
		// Create one column of each type
		List<ColumnModel> temp = new LinkedList<ColumnModel>();
		temp.add(TableModelTestUtils.createColumn(0L, "a", ColumnType.STRING));
		temp.add(TableModelTestUtils.createColumn(0L, "b", ColumnType.INTEGER));
		temp.add(TableModelTestUtils.createColumn(0L, "c", ColumnType.DOUBLE));
		schema = new LinkedList<ColumnModel>();
		for(ColumnModel cm: temp){
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Create some CSV data
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "1.1" });
		input.add(new String[] { null, "3", "1.2" });
		input.add(new String[] { "AAA", "4", null });
		input.add(new String[] { "ZZZ", null, "1.3" });
		// This is the starting input stream
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader, true, null);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), iterator,
				null, null, null);
		// Now wait for the table index to be ready
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, "select * from " + tableId, null, 100L);
		assertNotNull(queryResult.getQueryResults());
		// Now stream the query results to a CSV
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		CSVWriterStreamProxy proxy = new CSVWriterStreamProxy(csvWriter);
		// Downlaod the data to a csv
		boolean includeRowIdAndVersion = true;
		DownloadFromTableResult response = waitForConsistentStreamQuery("select * from " + tableId, proxy, includeRowIdAndVersion, true);
		assertNotNull(response);
		assertNotNull(response.getEtag());
		// Read the results
		CsvNullReader copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		List<String[]> copy = copyReader.readAll();
		assertNotNull(copy);
		// the results should include a header.
		assertEquals(input.size(),  copy.size());
		// the first two columns should include the rowId can verionNumber
		assertEquals(Arrays.asList(TableConstants.ROW_ID, TableConstants.ROW_VERSION, "a", "b", "c").toString(), Arrays.toString(copy.get(0)));
		assertEquals(Arrays.asList("0", "0", "AAA", "2", "1.1").toString(), Arrays.toString(copy.get(1)));
		assertEquals(Arrays.asList("1", "0",  null, "3", "1.2" ).toString(), Arrays.toString(copy.get(2)));

		// test with aggregate columns
		stringWriter = new StringWriter();
		csvWriter = new CSVWriter(stringWriter);
		proxy = new CSVWriterStreamProxy(csvWriter);
		includeRowIdAndVersion = false;
		response = waitForConsistentStreamQuery("select count(a), a from " + tableId + " group by a order by a", proxy,
				includeRowIdAndVersion, true);
		assertNotNull(response);
		assertNotNull(response.getEtag());
		// Read the results
		copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		List<String[]> counts = copyReader.readAll();
		assertNotNull(counts);
		// the first two columns should include the rowId can verionNumber
		assertEquals(Arrays.asList("COUNT(a)", "a").toString(), Arrays.toString(counts.get(0)));
		assertEquals(Arrays.asList("0", null).toString(), Arrays.toString(counts.get(1)));
		assertEquals(Arrays.asList("2", "AAA").toString(), Arrays.toString(counts.get(2)));
		assertEquals(Arrays.asList("1", "ZZZ").toString(), Arrays.toString(counts.get(3)));

		// make some changes
		copy.get(1)[2] = "DDD";
		copy.get(2)[2] = "EEE";
		copy.get(3)[2] = "FFF";
		reader = TableModelTestUtils.createReader(copy);
		// Use the data to update the table
		iterator = new CSVToRowIterator(schema, reader, true, null);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), iterator,
				response.getEtag(), null, null);
		// Fetch the results again but this time without row id and version so it can be used to create a new table.
		stringWriter = new StringWriter();
		csvWriter = new CSVWriter(stringWriter);
		proxy = new CSVWriterStreamProxy(csvWriter);
		includeRowIdAndVersion = false;
		response = waitForConsistentStreamQuery("select c, a, b from " + tableId, proxy, includeRowIdAndVersion, true);
		// read the results
		copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		copy = copyReader.readAll();
		assertNotNull(copy);
		// As long as the updated data does not includes rowIds and row version we can use it to create a new table.
		assertEquals(Arrays.asList( "c", "a", "b").toString(), Arrays.toString(copy.get(0)));
		assertEquals(Arrays.asList("1.1","DDD", "2").toString(), Arrays.toString(copy.get(1)));
		assertEquals(Arrays.asList("1.2","EEE", "3").toString(), Arrays.toString(copy.get(2)));
	}
	
	@Test
	public void testCSVDownloadAggregateColumn() throws Exception {
		// Create one column of each type
		schema = Lists.newArrayList(
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(0L, "a", ColumnType.STRING)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(0L, "b", ColumnType.INTEGER)));
		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		// Create some CSV data
		String[][] input = { { "a", "b" }, { "A", "1" }, { "A", "2" }, { "C", "4" } };
		// This is the starting input stream
		CsvNullReader reader = TableModelTestUtils.createReader(Lists.newArrayList(input));
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader, true, null);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), iterator,
				null,
				null, null);
		// Now wait for the table index to be ready
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, "select * from " + tableId, null, null);
		assertNotNull(queryResult.getQueryResults());

		String aggregateSql = "select a, sum(b) from " + tableId + " group by a order by a";
		// the results should include a header.
		// the first two columns should include the rowId can verionNumber
		String[][] expectedResults = { { "a", "SUM(b)" }, { "A", "3" }, { "C", "4" } };

		queryResult = waitForConsistentQuery(adminUserInfo, aggregateSql, null, null);
		assertEquals(expectedResults.length - 1, queryResult.getQueryResults().getRows().size());
		assertEquals(null, queryResult.getQueryResults().getHeaders().get(0).getId());
		assertEquals(null, queryResult.getQueryResults().getHeaders().get(1).getId());
		assertEquals(expectedResults[0][0], queryResult.getQueryResults().getHeaders().get(0).getName());
		assertEquals(expectedResults[0][1], queryResult.getQueryResults().getHeaders().get(1).getName());
		for (int i = 1; i < expectedResults.length; i++) {
			assertArrayEquals(expectedResults[i], queryResult.getQueryResults().getRows().get(i - 1).getValues().toArray(new String[0]));
		}

		// Now stream the query results to a CSV
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		CSVWriterStreamProxy proxy = new CSVWriterStreamProxy(csvWriter);
		// Downlaod the data to a csv
		boolean includeRowIdAndVersion = true;
		DownloadFromTableResult response = waitForConsistentStreamQuery(aggregateSql, proxy, includeRowIdAndVersion, true);
		assertNotNull(response);
		assertNotNull(response.getEtag());
		// Read the results
		CsvNullReader copyReader = new CsvNullReader(new StringReader(stringWriter.toString()));
		List<String[]> copy = copyReader.readAll();
		copyReader.close();
		assertNotNull(copy);
		assertEquals(expectedResults.length, copy.size());
		for (int i = 0; i < expectedResults.length; i++) {
			assertArrayEquals(expectedResults[i], copy.get(i));
		}
	}

	@Test
	public void testPermissions() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		long userId = userManager.createUser(user);
		certifiedUserManager.setUserCertificationStatus(adminUserInfo, userId, true);
		authenticationManager.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, true);
		UserInfo owner = userManager.getUserInfo(userId);
		users.add(owner);

		user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		userId = userManager.createUser(user);
		certifiedUserManager.setUserCertificationStatus(adminUserInfo, userId, true);
		authenticationManager.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, true);
		UserInfo notOwner = userManager.getUserInfo(userId);
		users.add(notOwner);

		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : columnModels) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		Project project = new Project();
		project.setName("Proj-" + UUID.randomUUID().toString());
		projectId = entityManager.createEntity(owner, project, null);

		List<Long> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		table.setParentId(projectId);
		tableId = entityManager.createEntity(owner, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(owner, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);
		tableRowManager.appendRows(owner, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING)), mockPprogressCallback);

		try {
			tableRowManager.appendRows(notOwner, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
					createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING)), mockPprogressCallback);
			fail("no update permissions");
		} catch (UnauthorizedException e) {
		}

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		waitForConsistentQuery(owner, sql, null, 8L);
		try {
			waitForConsistentQuery(notOwner, sql, null, 8L);
			fail("no read permissions");
		} catch (UnauthorizedException e) {
		}

		// add users to acl
		AccessControlList acl = entityPermissionsManager.getACL(projectId, adminUserInfo);
		acl.setId(tableId);
		entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
		acl = entityPermissionsManager.getACL(tableId, adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(notOwner.getId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE));
		acl.getResourceAccess().add(ra);
		acl = entityPermissionsManager.updateACL(acl, adminUserInfo);

		tableRowManager.appendRows(notOwner, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING)), mockPprogressCallback);
		waitForConsistentQuery(notOwner, sql, null, 8L);

		// add access restriction
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(tableId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Collections.singletonList(rod));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		TermsOfUseAccessRequirement downloadAR = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);

		try {
			waitForConsistentQuery(notOwner, sql, null, 8L);
			fail();
		} catch (UnauthorizedException e) {
		}

		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(notOwner.getId().toString());
		aa.setRequirementId(downloadAR.getId());
		accessApprovalManager.createAccessApproval(notOwner, aa);

		waitForConsistentQuery(notOwner, sql, null, 8L);
		tableRowManager.appendRows(notOwner, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false),
				createRowSet(Lists.transform(headers, TableModelUtils.LONG_TO_STRING)), mockPprogressCallback);
	}

	private RowSet createRowSet(List<String> headers) {
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
		rowSet.setTableId(tableId);
		return rowSet;
	}

	/**
	 * Attempt to run a query. If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * 
	 * @param user
	 * @param sql
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private QueryResult waitForConsistentQuery(UserInfo user, String sql, List<SortItem> sortItems, Long limit) throws Exception {
		long start = System.currentTimeMillis();
		while(true){
			try {
				Pair<QueryResult, Long> queryResult = tableRowManager.query(user, sql, sortItems, 0L, limit, true, false, true);
				return queryResult.getFirst();
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: "+e.getStatus().getErrorMessage(),TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
				Thread.sleep(1000);
			}
		}
	}
	
	private QueryResultBundle waitForConsistentQueryBundle(UserInfo user, String sql, Long offset, Long limit) throws Exception {
		long start = System.currentTimeMillis();
		while (true) {
			try {
				QueryBundleRequest queryBundleRequest = new QueryBundleRequest();
				queryBundleRequest.setPartMask(-1L);
				Query query = new Query();
				query.setIsConsistent(true);
				query.setSql(sql);
				query.setOffset(offset);
				query.setLimit(limit);
				queryBundleRequest.setQuery(query);
				QueryResultBundle queryResult = tableRowManager.queryBundle(user, queryBundleRequest);
				return queryResult;
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.",
						(System.currentTimeMillis() - start) < MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: " + e.getStatus().getErrorMessage(), TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: " + e.getStatus());
				Thread.sleep(1000);
			}
		}
	}

	private void waitForConsistentQueryError(final UserInfo user, final String sql) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, 250, new Callable<Pair<Boolean, Void>>() {
			@Override
			public Pair<Boolean, Void> call() throws Exception {
				try {
					tableRowManager.query(user, sql, null, 0L, 100L, true, false, true);
					fail("should not have succeeded");
				} catch (TableUnavilableException e) {
					return Pair.create(false, null);
				} catch (TableFailedException e) {
					return Pair.create(true, null);
				}
				return Pair.create(false, null);
			}
		});
	}

	/**
	 * Attempt to run a query as a stream.  If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * @param sql
	 * @param writer
	 * @param includeRowIdAndVersion
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private DownloadFromTableResult waitForConsistentStreamQuery(String sql, CSVWriterStream writer, boolean includeRowIdAndVersion,
			boolean writeHeader) throws Exception {
		long start = System.currentTimeMillis();
		while(true){
			try {
				return tableRowManager.runConsistentQueryAsStream(adminUserInfo, sql, null, writer, includeRowIdAndVersion, writeHeader);
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: "+e.getStatus().getErrorMessage(),TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
				Thread.sleep(1000);
			}
		}
	}

	private void compareValues(RowSet expected, int offset, int count, RowSet actual) {
		assertEquals(count, actual.getRows().size());
		int expectedIndex = offset;
		int actualIndex = 0;
		for (int i = 0; i < count; i++, expectedIndex++, actualIndex++) {
			assertEquals("Row " + i, expected.getRows().get(expectedIndex).getValues().toString(), actual.getRows().get(actualIndex)
					.getValues().toString());
		}
	}

	private void compareValues(String[] expected, RowSet actual) {
		assertEquals(expected.length, actual.getRows().size());
		for (int i = 0; i < expected.length; i++) {
			List<String> values = actual.getRows().get(i).getValues();
			assertEquals(1, values.size());
			assertEquals(expected[i], values.get(0));
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> T getTargetObject(T proxy) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return proxy;
		}
	}
}
