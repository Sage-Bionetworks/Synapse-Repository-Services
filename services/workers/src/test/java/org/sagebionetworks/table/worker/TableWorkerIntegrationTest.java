package org.sagebionetworks.table.worker;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mysql.cj.xdevapi.Column;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.NULL_VALUE_KEYWORD;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableWorkerIntegrationTest {

	/**
	 * 
	 */
	public static final int MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	StackConfiguration config;
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableQueryManager tableQueryManger;
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
	EntityAclManager entityAclManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	
	@Autowired
	TableStatusDAO tableStatusDAO;
	@Autowired
	TableTransactionDao tableTransactionDao;
	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	
	@Autowired
	private TrashManager trashManager;
	
	@Autowired
	AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	TableRowTruthDAO tableRowTruthDAO;

	private UserInfo adminUserInfo;
	private UserInfo anonymousUser;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<String> headers;
	private String tableId;
	
	ProgressCallback mockProgressCallback;
	ProgressCallback mockProgressCallbackVoid;

	private List<UserInfo> users;

	private String projectId;
	private String simpleSql;
	
	Query query;
	QueryOptions queryOptions;

	@BeforeEach
	public void before() throws Exception {
		users = Lists.newArrayList();
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		when(mockProgressCallback.getLockTimeoutSeconds()).thenReturn(2L);
		mockProgressCallbackVoid= Mockito.mock(ProgressCallback.class);
		when(mockProgressCallbackVoid.getLockTimeoutSeconds()).thenReturn(2L);
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		anonymousUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		this.tableId = null;
		simpleSql = "select * from " + tableId;
		query = new Query();
		queryOptions = new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(true);
		
		Project project = new Project();
		project.setName("Proj-" + UUID.randomUUID().toString());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
	}
	
	@AfterEach
	public void after() throws Exception {
		if (tableId != null) {
			try {
				entityManager.deleteEntity(adminUserInfo, tableId);
			} catch (Exception e) {}
		}
		if (projectId != null) {
			entityManager.deleteEntity(adminUserInfo, projectId);
		}
		// cleanup
		columnManager.truncateAllColumnData(adminUserInfo);
		for (UserInfo user : users) {
			try {
				userManager.deletePrincipal(adminUserInfo, user.getId());
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Create the schema with on column of each type.
	 */
	void createSchemaOneOfEachType() {
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
	}
	
	/**
	 * Create a table entity using the schema.
	 * 
	 */
	void createTableWithSchema() {
		headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		table.setParentId(projectId);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		tableEntityManager.setTableSchema(adminUserInfo, headers, tableId);
	}

	@Test
	public void testRoundTrip() throws Exception {
		// Create one column of each type
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 2));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 2));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(8L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult);
			assertNotNull(queryResult.getQueryResults());
			assertEquals(tableId, queryResult.getQueryResults().getTableId());
			assertNotNull(queryResult.getQueryResults().getHeaders());
			assertEquals(schema.size(), queryResult.getQueryResults().getHeaders().size());
			assertNotNull(queryResult.getQueryResults().getRows());
			assertEquals(6, queryResult.getQueryResults().getRows().size());
			assertNotNull(queryResult.getQueryResults().getEtag());
			assertEquals(referenceSet.getEtag(),
					queryResult.getQueryResults().getEtag());
			assertEquals(referenceSet.getEtag(), queryResult.getQueryResults().getEtag());
		});
	}
	
	/**
	 * Must be able to run a query: 'SELECT ROW_ID FROM SYN123'
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3674() throws Exception {
		// Create one column of each type
		createSchemaOneOfEachType();
		createTableWithSchema();
		// add a row
		List<Row> rows = TableModelTestUtils.createRows(schema, 1);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		// Wait for the table to become available
		String sql = "select row_id from " + tableId;
		query.setSql(sql);
		query.setLimit(8L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult);
			assertNotNull(queryResult.getQueryResults());
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			Row row = queryResult.getQueryResults().getRows().get(0);
			assertEquals(new Long(1), row.getRowId());
			assertEquals(new Long(1), row.getVersionNumber());
			assertNotNull(row.getValues());
			assertEquals("1", row.getValues().get(0));
		});
	}

	@Test
	public void testGetCellValuesPLFM_4191() throws Exception {
		schema = new LinkedList<ColumnModel>();
		ColumnModel cm = TableModelTestUtils.createColumn(null, "data.csv", ColumnType.STRING);
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 1));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		String sql = "select * from " + tableId;
		query.setSql(sql);
		query.setLimit(7L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult);
		});
		// This call would throw an exception.
		rowSet = tableEntityManager.getCellValues(adminUserInfo, tableId, referenceSet.getRows(),
				schema);
	}


	@Test
	public void testLimitOffset() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		final RowSet addRowSet = new RowSet();
		addRowSet.setRows(TableModelTestUtils.createRows(schema, 6));
		addRowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		addRowSet.setTableId(tableId);
		
		referenceSet = appendRows(adminUserInfo, tableId, addRowSet, mockProgressCallback);
		String sql = "select * from " + tableId;
		query.setSql(sql);
		query.setLimit(7L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertNotNull(queryResult);
		});
		
		final RowSet rowSet = tableEntityManager.getCellValues(adminUserInfo, tableId, referenceSet.getRows(),
				schema);
		// Wait for the table to become available
		sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(7L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(6, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 0, 6, queryResult.getQueryResults());
		});
		
		query.setLimit(6L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(6, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 0, 6, queryResult.getQueryResults());
		});
		
		query.setLimit(5L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(5, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 0, 5, queryResult.getQueryResults());
		});

		query.setOffset(5L);
		query.setLimit(1L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 5, 1, queryResult.getQueryResults());
		});

		query.setOffset(5L);
		query.setLimit(2L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 5, 1, queryResult.getQueryResults());
		});

		query.setOffset(0L);
		query.setLimit(8L);
		query.setSql(sql + " limit 2 offset 3");

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 3, 2, queryResult.getQueryResults());
		});

		query.setOffset(2L);
		query.setLimit(2L);
		query.setSql(sql + " limit 8 offset 2");
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 4, 2, queryResult.getQueryResults());
		});

		query.setOffset(2L);
		query.setLimit(2L);
		query.setSql(sql + " limit 8 offset 3");
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertNull(queryResult.getNextPageToken());
			compareValues(rowSet, 5, 1, queryResult.getQueryResults());
		});
	}

	@Test
	public void testSorting() throws Exception {
		// Create one column of each type
		schema = Lists.newArrayList(
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "name", ColumnType.STRING)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col1", ColumnType.INTEGER)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col2", ColumnType.INTEGER)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col3", ColumnType.INTEGER)));
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "a", "1", "10", "3"),
				TableModelTestUtils.createRow(null, null, "b", "2", "11", "1"),
				TableModelTestUtils.createRow(null, null, "c", "3", "11", "2")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select name from " + tableId + " order by col1";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			compareValues(new String[] { "a", "b", "c" }, queryResult.getQueryResults());
			
		});

		SortItem sort1 = new SortItem();
		sort1.setColumn("col1");
		sort1.setDirection(SortDirection.DESC);
		SortItem sort2 = new SortItem();
		sort2.setColumn("col2");
		sort2.setDirection(SortDirection.DESC);
		
		query.setSort(Lists.newArrayList(sort2));
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			compareValues(new String[] { "b", "c", "a" }, queryResult.getQueryResults());
		});

		query.setSort(Lists.newArrayList(sort2, sort1));
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			compareValues(new String[] { "c", "b", "a" }, queryResult.getQueryResults());
		});

		query.setSort(Lists.newArrayList(sort1));
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			compareValues(new String[] { "c", "b", "a" }, queryResult.getQueryResults());
		});
	}

	@Test
	public void testDoubleSetFunctions() throws Exception {
		// Create one column of each type
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(null, "number", ColumnType.DOUBLE)));
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "1.5"), TableModelTestUtils.createRow(null, null, "2.0"),
				TableModelTestUtils.createRow(null, null, "2.0"), TableModelTestUtils.createRow(null, null, "4.5"),
				TableModelTestUtils.createRow(null, null, "2.0")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select avg(number) from " + tableId;
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
			compareValues(new String[] { "2.4" }, queryResult.getQueryResults());
		});

		sql = "select sum(number) as ss from " + tableId + " group by number order by ss asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
			assertEquals("ss", queryResult.getQueryResults().getHeaders().get(0).getName());
			compareValues(new String[] { "1.5", "4.5", "6.0" }, queryResult.getQueryResults());
		});

		sql = "select sum(number) from " + tableId + " group by number order by sum(number) asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(ColumnType.DOUBLE, queryResult.getQueryResults().getHeaders().get(0).getColumnType());
			assertEquals("SUM(number)", queryResult.getQueryResults().getHeaders().get(0).getName());
			compareValues(new String[] { "1.5", "4.5", "6.0" }, queryResult.getQueryResults());
		});
	}



	@Test
	public void testNullNextPageToken() throws Exception {
		assertThrows(IllegalArgumentException.class, ()->{
			tableQueryManger.queryNextPage(mockProgressCallbackVoid, adminUserInfo, null);
		});
	}

	@Test
	public void testEmptyNextPageToken() throws Exception {
		assertThrows(IllegalArgumentException.class, ()->{
			tableQueryManger.queryNextPage(mockProgressCallbackVoid, adminUserInfo, new QueryNextPageToken());
		});
	}

	@Test
	public void testInvalidNextPageToken() throws Exception {
		QueryNextPageToken token = new QueryNextPageToken();
		token.setToken("<invalid/>");
		assertThrows(IllegalArgumentException.class, ()->{
			tableQueryManger.queryNextPage(mockProgressCallbackVoid, adminUserInfo, token);
		});
	}

	@Test
	public void testLimitWithCountQueries() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 10));
		assertEquals(ColumnType.STRING, schema.get(0).getColumnType());
		// make sure we can order by first column (which should be STRING) to make row 0 come first
		rowSet.getRows().get(0).getValues().set(0, "!!" + rowSet.getRows().get(0).getValues().get(0));
		// and make grouping return 9 rows
		rowSet.getRows().get(4).getValues().set(0, rowSet.getRows().get(0).getValues().get(0));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " limit 5";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {
			assertEquals(5, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(5L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 3 offset 1";
		
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {
			assertEquals(3, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(3L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 5 offset 3";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {
			assertEquals(5, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(5L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 5 offset 8";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {
			assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(2L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 5";
		waitForConsistentQueryBundle(adminUserInfo, sql, 0L, 3L, (queryResultBundle) -> {			
			assertEquals(3, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(5L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 3 offset 1";
		waitForConsistentQueryBundle(adminUserInfo, sql, 2L, 1L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(3L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 5 offset 3";
		waitForConsistentQueryBundle(adminUserInfo, sql, 6L, 3L, (queryResultBundle) -> {			
			assertEquals(0, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(5L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 5 offset 8";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(2L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select * from " + tableId + " limit 8 offset 12";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L, (queryResultBundle) -> {			
			assertEquals(0, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(0L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select count(*) from " + tableId + " limit 100";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals("10", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals(1L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select max(" + schema.get(0).getName() + ") from " + tableId + " limit 100";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(1L, queryResultBundle.getQueryCount().longValue());
		});

		String groupSql = " group by " + schema.get(0).getName();
		String orderSql = " order by " + schema.get(0).getName() + " asc";

		sql = "select "+schema.get(0).getName()+" from " + tableId + groupSql + orderSql + " limit 100";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 1L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals(rowSet.getRows().get(0).getValues().get(0), queryResultBundle.getQueryResult().getQueryResults().getRows().get(0)
					.getValues().get(0));
			assertEquals(9L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select count(" + schema.get(0).getName() + ") from " + tableId + groupSql + orderSql + " limit 100";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L, (queryResultBundle) -> {			
			assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("1", queryResultBundle.getQueryResult().getQueryResults().getRows().get(1).getValues().get(0));
			assertEquals(9L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select count(*) as c from " + tableId + groupSql + " order by c desc limit 100";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L, (queryResultBundle) -> {			
			assertEquals(2, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("1", queryResultBundle.getQueryResult().getQueryResults().getRows().get(1).getValues().get(0));
			assertEquals(9L, queryResultBundle.getQueryCount().longValue());
		});

		sql = "select count(*) as c from " + tableId + groupSql + " order by c desc limit 1";
		waitForConsistentQueryBundle(adminUserInfo, sql, null, 2L, (queryResultBundle) -> {			
			assertEquals(1, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
			assertEquals("2", queryResultBundle.getQueryResult().getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals(1L, queryResultBundle.getQueryCount().longValue());
		});
	}

	@Test
	public void testColumnOrderWithQueries() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 10));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {			
			for (int i = 0; i < queryResultBundle.getSelectColumns().size(); i++) {
				assertEquals(schema.get(i).getName(), queryResultBundle.getSelectColumns().get(i).getName());
				assertEquals(schema.get(i).getId(), queryResultBundle.getQueryResult().getQueryResults().getHeaders().get(i).getId());
			}
		});

		sql = "select i1, i2 from " + tableId;
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {			
			assertEquals("i1", queryResultBundle.getSelectColumns().get(0).getName());
			assertEquals("i2", queryResultBundle.getSelectColumns().get(1).getName());
		});

		sql = "select i3, i1 from " + tableId;
		waitForConsistentQueryBundle(adminUserInfo, sql, null, null, (queryResultBundle) -> {			
			assertEquals("i3", queryResultBundle.getSelectColumns().get(0).getName());
			assertEquals("i1", queryResultBundle.getSelectColumns().get(1).getName());
		});

	}

	/**
	 * Test if things work if the table index is not being build, which can happen for example after a migration
	 */
	@Test
	public void testRoundTripAfterMigrate() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		RowSet rowSet = createRowSet(headers);
		appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(8L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});

		// reset table index
		tableStatusDAO.clearAllTableState();

		// now we still should get the index taken care of
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
	}
	
	@Test
	public void testDeleteTable() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		RowSet rowSet = createRowSet(headers);
		appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		// Wait for the table to become available
		String sql = "select * from " + tableId;
		query.setSql(sql);
		query.setLimit(8L);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
		
		// Move the table to the trash
		this.trashManager.moveToTrash(adminUserInfo, tableId, false);
		// change the query to avoid the cache.
		query.setSql(sql+" limit 100");
		assertThrows(EntityInTrashCanException.class, ()->{
			waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> { 
				fail("Should have not received a result");
			});
		});
		// should do nothing
		tableEntityManager.deleteTableIfDoesNotExist(tableId);
		// restore the table to the original project
		this.trashManager.restoreFromTrash(adminUserInfo, tableId, projectId);
		// should now be able to query again
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
		
		// Now actually delete the table.
		entityManager.deleteEntity(adminUserInfo, tableId);
		assertThrows(NotFoundException.class, ()->{
			waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {
				fail("Should have not received a result");
			});
		});
		// should be able to call this multiple times.
		tableEntityManager.deleteTableIfDoesNotExist(tableId);
		tableEntityManager.deleteTableIfDoesNotExist(tableId);
	}

	/**
	 * Test if things work after a migration where there is no table status, but the index and current index have to be
	 * built
	 */
	@Test
	public void testAfterMigrate() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		RowSet rowSet = createRowSet(headers);
		appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(8L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {					
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});

		// reset table index
		tableStatusDAO.clearAllTableState();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.TABLE);
		message.setObjectId(KeyFactory.stringToKey(tableId).toString());
		message = changeDAO.replaceChange(message);
		// and pretend we just created it
		repositoryMessagePublisher.publishToTopic(message);

		final IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		final TableIndexDAO tableIndexDAO = tableConnectionFactory.getConnection(idAndVersion);
		assertTrue(TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				return tableIndexDAO.getRowCountForTable(idAndVersion) != null;
			}
		}));

		// now we still should get the index taken care of
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
	}

	@Test
	public void testPartialUpdateRoundTrip() throws Exception {
		// Create one column of each type
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 10);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 3));
		// Add empty rows
		rows.addAll(TableModelTestUtils.createEmptyRows(schema, 3));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(100L);
		
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(16, response.getQueryResults().getRows().size());
		});

		RowSet expectedRowSet = queryResult.getQueryResults();

		// apply updates to expected and actual
		List<PartialRow> partialRows = Lists.newArrayList(); 
		for(int i = 0; i < 16; i++){
			partialRows.add(TableModelTestUtils.updatePartialRow(schema, expectedRowSet.getRows().get(i), i));
			expectedRowSet.getRows().get(i).setVersionNumber(2L);
		}
		rows = TableModelTestUtils.createExpectedFullRows(schema, 5);
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setRowId(17L + i);
			rows.get(i).setVersionNumber(2L);
		}
		expectedRowSet.getRows().addAll(rows);
		partialRows.addAll(TableModelTestUtils.createPartialRows(schema, 5));

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setRows(partialRows);
		partialRowSet.setTableId(tableId);
		appendPartialRows(adminUserInfo, tableId, partialRowSet, mockProgressCallback);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {	
			// we couldn't know the etag in advance
			expectedRowSet.setEtag(response.getQueryResults().getEtag());
			assertEquals(expectedRowSet, response.getQueryResults());
		});
	}

	@Test
	public void testRemoveColumn() throws Exception {
		// Create one column
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(null, "col1", ColumnType.STRING)));
		createTableWithSchema();

		// add data
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "a")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		query.setSql(sql);
		query.setLimit(100L);
		
		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(1, response.getQueryResults().getRows().get(0).getValues().size());
		});

		// add new column
		schema.add(columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(null, "col2", ColumnType.STRING)));
		headers = TableModelUtils.getIds(schema);
		asyncHelper.setTableSchema(adminUserInfo, headers, tableId, MAX_WAIT_MS);
		String newColumnId = ""+headers.get(headers.size()-1);
		// set data on new column
		
		PartialRow firstRow = TableModelTestUtils.createPartialRow(queryResult.getQueryResults().getRows().get(0).getRowId(), newColumnId, "b");
		PartialRowSet firstRowChange = new PartialRowSet();
		firstRowChange.setRows(Lists.newArrayList(firstRow));
		firstRowChange.setTableId(tableId);
		appendPartialRows(adminUserInfo, tableId,
				firstRowChange, mockProgressCallback);

		// wait for table to be available
		sql = "select * from " + tableId;
		query.setSql(sql);
		
		queryResult = waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(2, response.getQueryResults().getRows().get(0).getValues().size());
		});

		// remove column a
		schema.remove(0);
		headers = TableModelUtils.getIds(schema);
		asyncHelper.setTableSchema(adminUserInfo, headers, tableId, MAX_WAIT_MS);

		// wait for table to be available
		sql = "select * from " + tableId;
		query.setSql(sql);
		queryResult = waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(1, response.getQueryResults().getRows().get(0).getValues().size());
		});

		// set data again
		firstRow = TableModelTestUtils.createPartialRow(queryResult.getQueryResults().getRows().get(0).getRowId(), newColumnId, "c");
		firstRowChange = new PartialRowSet();
		firstRowChange.setRows(Lists.newArrayList(firstRow));
		firstRowChange.setTableId(tableId);
		appendPartialRows(adminUserInfo, tableId,
				firstRowChange, mockProgressCallback);

	}

	@Test
	public void testDates() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "coldate", ColumnType.DATE)));
		createTableWithSchema();
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
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " where coldate between '2014-2-3 3:00' and '2016-1-1' order by coldate asc";
		query.setSql(sql);
		query.setLimit(8L);
		
		QueryResult expectedResults = waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {
			assertNotNull(queryResult.getQueryResults());
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertEquals("2014-2-3 3:41",
					dateTimeInstance.format(new Date(Long.parseLong(queryResult.getQueryResults().getRows().get(0).getValues().get(0)))));
			assertEquals("2015-2-3 3:41",
					dateTimeInstance.format(new Date(Long.parseLong(queryResult.getQueryResults().getRows().get(1).getValues().get(0)))));

		});
		
		// Again, but now with longs
		sql = "select * from " + tableId + " where coldate between " + dateTimeInstance.parse("2014-2-3 3:00").getTime() + " and "
				+ dateTimeInstance.parse("2016-1-1 0:00").getTime() + " order by coldate asc";
		query.setSql(sql);
		query.setLimit(8L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedResults, queryResult);
		});
	}

	@Test
	public void testDoubles() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "coldouble", ColumnType.DOUBLE)));
		createTableWithSchema();
		Double[] doubles = { Double.NaN, Double.NEGATIVE_INFINITY, -Double.MAX_VALUE+1.0E307, -1.0, 0.0, 1.0, 3e42, Double.MAX_VALUE-1.0E307,
				Double.POSITIVE_INFINITY };
		String[] expected = { "NaN", "-Infinity", "-1.6976931348623157e308", "-1", "0", "1", "3e42", "1.6976931348623157e308",
				"Infinity" };
		assertEquals(doubles.length, expected.length);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, doubles.length);
		for (int i = 0; i < doubles.length; i++) {
			rows.get(i).getValues().set(0, doubles[i] == null ? null : doubles[i].toString());
		}

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by coldouble ASC";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
			assertEquals(doubles.length, queryResult.getQueryResults().getRows().size());
			for (int i = 0; i < doubles.length; i++) {
				assertEquals(expected[i], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
			}
		});

		sql = "select * from " + tableId + " order by coldouble DESC";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertNotNull(queryResult.getQueryResults());
			assertEquals(doubles.length, queryResult.getQueryResults().getRows().size());
			for (int i = 0; i < doubles.length; i++) {
				assertEquals(expected[doubles.length - i - 1], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
			}
		});

		sql = "select * from " + tableId + " where isNaN(coldouble)";
		query.setSql(sql);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertNotNull(queryResult.getQueryResults());
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertEquals("NaN", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		});

		sql = "select * from " + tableId + " where isInfinity(coldouble) order by coldouble";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertNotNull(queryResult.getQueryResults());
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertEquals("-Infinity", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("Infinity", queryResult.getQueryResults().getRows().get(1).getValues().get(0));
		});

		sql = "select avg(coldouble) from " + tableId
				+ " where not isNaN(coldouble) and not isInfinity(coldouble) and coldouble is not null order by coldouble";
		query.setSql(sql);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertNotNull(queryResult.getQueryResults());
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertEquals("0.0", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
		});
	}

	@Test
	public void testBooleans() throws Exception {
		schema = Lists.newArrayList(columnManager.createColumnModel(adminUserInfo,
				TableModelTestUtils.createColumn(0L, "colbool", ColumnType.BOOLEAN)));
		createTableWithSchema();

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
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		String[] failingBooleans = new String[] { "1", "0", "2", "falseish", "nottrue" };
		for (String failingBoolean : failingBooleans) {
			List<Row> failRow = TableModelTestUtils.createRows(schema, 1);
			failRow.get(0).getValues().set(0, failingBoolean);

			RowSet failRowSet = new RowSet();
			failRowSet.setRows(failRow);
			failRowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
			failRowSet.setTableId(tableId);
			assertThrows(IllegalArgumentException.class, ()->{
				appendRows(adminUserInfo, tableId, failRowSet, mockProgressCallback);
			});
		}

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id asc";
		query.setSql(sql);
		query.setLimit(20L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertNotNull(queryResult.getQueryResults());
			assertEquals(expectedOut.length, queryResult.getQueryResults().getRows().size());
			for (int i = 0; i < expectedOut.length; i++) {
				assertEquals(expectedOut[i], queryResult.getQueryResults().getRows().get(i).getValues().get(0));
			}
		});

		sql = "select * from " + tableId + " where colbool is true order by row_id asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool is false order by row_id asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool is not true order by row_id asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {						
			assertEquals(expectedFalseCount + expectedNullCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool is not false order by row_id asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedTrueCount + expectedNullCount, queryResult.getQueryResults().getRows().size());
		});
		
		sql = "select * from " + tableId + " where colbool = true order by row_id asc";
		query.setSql(sql);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool = false order by row_id asc";
		query.setSql(sql);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool <> true order by row_id asc";
		query.setSql(sql);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedFalseCount, queryResult.getQueryResults().getRows().size());
		});

		sql = "select * from " + tableId + " where colbool <> false order by row_id asc";
		query.setSql(sql);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(expectedTrueCount, queryResult.getQueryResults().getRows().size());
		});
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

		createTableWithSchema();
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 4);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
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
		RowReferenceSet referenceSet2 = appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		assertEquals(3, referenceSet2.getRows().size());

		RowSelection rowsToDelete = new RowSelection();
		rowsToDelete.setEtag(referenceSet2.getEtag());
		rowsToDelete.setRowIds(Lists.newArrayList(referenceSet2.getRows().get(1).getRowId(), referenceSet.getRows().get(3).getRowId()));

		referenceSet = tableEntityManager.deleteRows(adminUserInfo, tableId, rowsToDelete);
		assertEquals(2, referenceSet.getRows().size());

		// Wait for the table to become available
		String sql = "select * from " + tableId;
		query.setSql(sql);
		query.setLimit(100L);

		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertEquals("updatestring333", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("updatestring555", queryResult.getQueryResults().getRows().get(1).getValues().get(0));
		});
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
		createTableWithSchema();

		// Now add some data
		List<Row> rows = Lists.newArrayList();
		for (int i = 0; i < 6; i++) {
			rows.add(TableModelTestUtils.createRow(null, null, "something", null, "something", null));
		}
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(100L);

		QueryResult queryResult = waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(6, response.getQueryResults().getRows().size());
			for (int i = 0; i < 6; i++) {
				assertEquals(Lists.newArrayList("something", null, "something", "default"), response.getQueryResults().getRows().get(i)
						.getValues());
			}
		});

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
		appendPartialRows(adminUserInfo, tableId, partialRowSet, mockProgressCallback);

		query.setSql(sql);
		query.setLimit(100L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {
			assertEquals(6, response.getQueryResults().getRows().size());
			// update null columns
			assertEquals(Lists.newArrayList("something", "other", "something", "other"), response.getQueryResults().getRows().get(0)
					.getValues());
			// update non null columns
			assertEquals(Lists.newArrayList("other", null, "other", "default"), response.getQueryResults().getRows().get(1).getValues());
			// update nothing
			assertEquals(Lists.newArrayList("something", null, "something", "default"), response.getQueryResults().getRows().get(2)
					.getValues());
			// update with nulls
			assertEquals(Lists.newArrayList(null, null, "default", "default"), response.getQueryResults().getRows().get(3).getValues());
			// no change
			assertEquals(Lists.newArrayList("something", null, "something", "default"), response.getQueryResults().getRows().get(4)
					.getValues());
			// append
			assertEquals(Lists.newArrayList("something", null, "something", "default"), response.getQueryResults().getRows().get(5)
					.getValues());
		});
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
		createTableWithSchema();

		// Now add a file handle
		ExternalFileHandle fileHandle1 = new ExternalFileHandle();
		fileHandle1.setFileName("file1");
		fileHandle1.setExternalURL("http://not.com/file1");
		fileHandle1 = fileHandleManager.createExternalFileHandle(adminUserInfo, fileHandle1);
		ExternalFileHandle fileHandle2 = new ExternalFileHandle();
		fileHandle2.setFileName("file2");
		fileHandle2.setExternalURL("http://not.com/file2");
		fileHandle2 = fileHandleManager.createExternalFileHandle(adminUserInfo, fileHandle2);

		final String fileHandle1Id = fileHandle1.getId();
		final String fileHandle2Id = fileHandle2.getId();
		
		List<Row> rows = Lists.newArrayList(TableModelTestUtils.createRow(null, null, fileHandle1.getId(), fileHandle1.getId(),
				fileHandle1.getId(), null, null));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";
		query.setSql(sql);
		query.setLimit(100L);
		
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(Lists.newArrayList(fileHandle1Id, fileHandle1Id, fileHandle1Id, null, null), response
					.getQueryResults().getRows().get(0).getValues());
		});

		// append
		PartialRow partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(referenceSet.getRows().get(0).getRowId());
		partialRowAppend.setValues(buildMap(schema.get(0).getId(), fileHandle2.getId(), schema.get(1).getId(), fileHandle1.getId(), schema
				.get(2).getId(), null, schema.get(3).getId(), fileHandle2.getId()));

		PartialRowSet partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend));
		appendPartialRows(adminUserInfo, tableId, partialRowSet, mockProgressCallback);

		query.setSql(sql);
		query.setLimit(100L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(Lists.newArrayList(fileHandle2Id, fileHandle1Id, null, fileHandle2Id, null), response
					.getQueryResults().getRows().get(0).getValues());
		});

		// append
		partialRowAppend = new PartialRow();
		partialRowAppend.setRowId(referenceSet.getRows().get(0).getRowId());
		partialRowAppend.setValues(buildMap(schema.get(0).getId(), fileHandle1.getId(), schema.get(1).getId(), fileHandle1.getId(), schema
				.get(2).getId(), fileHandle1.getId(), schema.get(3).getId(), fileHandle1.getId()));

		partialRowSet = new PartialRowSet();
		partialRowSet.setTableId(tableId);
		partialRowSet.setRows(Lists.newArrayList(partialRowAppend));
		appendPartialRows(adminUserInfo, tableId, partialRowSet, mockProgressCallback);

		query.setSql(sql);
		query.setLimit(100L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (response) -> {			
			assertEquals(1, response.getQueryResults().getRows().size());
			assertEquals(Lists.newArrayList(fileHandle1Id, fileHandle1Id, fileHandle1Id, fileHandle1Id, null),
					response.getQueryResults().getRows().get(0).getValues());
		});
	}

	Map<String, String> buildMap(String... keyValues) {
		Map<String, String> map = Maps.newHashMap();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return map;
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
		createTableWithSchema();
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 10);
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, schema);
		long start = System.currentTimeMillis();
		appendRowsAsStream(adminUserInfo, tableId, schema, sparse.writeToDto().getRows().iterator(), null, null, null);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		// Query for the results
		String sql = "select A, a, \"Has Space\",\"" + specialChars + "\" from " + tableId + "";
		query.setSql(sql);
		query.setLimit(2L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
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
		});

		query.setSql("select A, Has Space from " + tableId);
		query.setLimit(100L);
		
		assertThrows(IllegalArgumentException.class, ()->{
			waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {
				fail("Should have not receive a response");
			});
		});

		// select a string literal
		query.setSql("select A, 'Has Space' from " + tableId);
		query.setLimit(100L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
			assertEquals(2, queryResult.getQueryResults().getHeaders().size());
			assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
			assertEquals("Has Space", queryResult.getQueryResults().getHeaders().get(1).getName());
			assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("string0", queryResult.getQueryResults().getRows().get(0).getValues().get(1));
		});

		query.setSql("select A, \"Has Space\" from " + tableId);
		query.setLimit(100L);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
			assertEquals(2, queryResult.getQueryResults().getHeaders().size());
			assertEquals(headers.get(0).toString(), queryResult.getQueryResults().getHeaders().get(1).getId());
			assertEquals(headers.get(2).toString(), queryResult.getQueryResults().getHeaders().get(0).getId());
			assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("string0", queryResult.getQueryResults().getRows().get(0).getValues().get(1));
		});

		query.setSql("select A, \"Has Space\" as HasSpace from " + tableId);
		query.setLimit(100L);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
			assertEquals(2, queryResult.getQueryResults().getHeaders().size());
			assertEquals(null, queryResult.getQueryResults().getHeaders().get(1).getId());
			assertEquals(null, queryResult.getQueryResults().getHeaders().get(0).getId());
			assertEquals("string200000", queryResult.getQueryResults().getRows().get(0).getValues().get(0));
			assertEquals("string0", queryResult.getQueryResults().getRows().get(0).getValues().get(1));
		});
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
		tableEntityManager.setTableSchema(adminUserInfo, null, tableId);
		// We should be able to query
		String sql = "select * from " + tableId;
		waitForConsistentQuery(adminUserInfo, sql, null, 1L, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
			assertNull(queryResult.getQueryResults().getEtag());
			assertEquals(tableId, queryResult.getQueryResults().getTableId());
			assertTrue(queryResult.getQueryResults().getHeaders() == null || queryResult.getQueryResults().getHeaders().isEmpty());
			assertTrue(queryResult.getQueryResults().getRows() == null || queryResult.getQueryResults().getRows().isEmpty());
		});
	}


	/**
	 * There were several issue related to creating tables with no columns and no rows.  This test validates that such tables are supported.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws Exception 
	 */
	@Test
	public void testNoRows() throws Exception {
		// Create one column of each type
		createSchemaOneOfEachType();
		createTableWithSchema();
		// We should be able to query
		String sql = "select * from " + tableId;
		waitForConsistentQuery(adminUserInfo, sql, null, 1L, (queryResult) -> {	
			assertNotNull(queryResult.getQueryResults());
			assertNotNull(queryResult.getQueryResults().getEtag());
			assertEquals(tableId, queryResult.getQueryResults().getTableId());
			assertTrue(queryResult.getQueryResults().getRows() == null
					|| queryResult.getQueryResults().getRows().isEmpty());
		});
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
		createTableWithSchema();
		// Create some CSV data
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "1.1" });
		input.add(new String[] { null, "3", "1.2" });
		input.add(new String[] { "AAA", "4", null });
		input.add(new String[] { "ZZZ", null, "1.3" });
		// This is the starting input stream
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader, true, null);
		appendRowsAsStream(adminUserInfo, tableId, schema, iterator,
				null, null, null);
		// Now wait for the table index to be ready
		
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId, null, 100L, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
		});
		
		// Downlaod the data to a csv
		boolean includeRowIdAndVersion = true;
		
		DownloadFromTableResult downloadResult = waitForConsistentStreamQuery("select * from " + tableId, null, includeRowIdAndVersion, true, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getEtag());
			assertNotNull(response.getResultsFileHandleId());
		});
		
		String resultFileContents = asyncHelper.downloadFileHandleFromS3(downloadResult.getResultsFileHandleId());
		// Read the results
		CSVReader copyReader = new CSVReader(new StringReader(resultFileContents));
		List<String[]> copy = copyReader.readAll();
		assertNotNull(copy);
		// the results should include a header.
		assertEquals(input.size(),  copy.size());
		// the first two columns should include the rowId can verionNumber
		assertEquals(Arrays.asList(TableConstants.ROW_ID, TableConstants.ROW_VERSION, "a", "b", "c").toString(), Arrays.toString(copy.get(0)));
		assertEquals(Arrays.asList("1", "1", "AAA", "2", "1.1").toString(), Arrays.toString(copy.get(1)));
		assertEquals(Arrays.asList("2", "1",  null, "3", "1.2" ).toString(), Arrays.toString(copy.get(2)));

		// test with aggregate columns
		includeRowIdAndVersion = false;
		
		downloadResult = waitForConsistentStreamQuery("select count(a), a from " + tableId + " group by a order by a",
				null, includeRowIdAndVersion, true, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getEtag());
			assertNotNull(response.getResultsFileHandleId());
		});
		
		resultFileContents = asyncHelper.downloadFileHandleFromS3(downloadResult.getResultsFileHandleId());
		// Read the results
		copyReader = new CSVReader(new StringReader(resultFileContents));
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
		appendRowsAsStream(adminUserInfo, tableId, schema, iterator, downloadResult.getEtag(), null, null);
		// Fetch the results again but this time without row id and version so it can be used to create a new table.
		includeRowIdAndVersion = false;
		
		downloadResult = waitForConsistentStreamQuery("select c, a, b from " + tableId, null, includeRowIdAndVersion, true, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getEtag());
			assertNotNull(response.getResultsFileHandleId());	
		});
		
		resultFileContents = asyncHelper.downloadFileHandleFromS3(downloadResult.getResultsFileHandleId());
		// read the results
		copyReader = new CSVReader(new StringReader(resultFileContents));
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
		createTableWithSchema();
		// Create some CSV data
		String[][] input = { { "a", "b" }, { "A", "1" }, { "A", "2" }, { "C", "4" } };
		// This is the starting input stream
		CSVReader reader = TableModelTestUtils.createReader(Lists.newArrayList(input));
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader, true, null);
		appendRowsAsStream(adminUserInfo, tableId, schema, iterator,
				null,
				null, null);
		// Now wait for the table index to be ready
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId, null, null, (queryResult) -> {			
			assertNotNull(queryResult.getQueryResults());
		});

		String aggregateSql = "select a, sum(b) from " + tableId + " group by a order by a";
		// the results should include a header.
		// the first two columns should include the rowId can verionNumber
		String[][] expectedResults = { { "a", "SUM(b)" }, { "A", "3" }, { "C", "4" } };

		waitForConsistentQuery(adminUserInfo, aggregateSql, null, null, (queryResult) -> {			
			assertEquals(expectedResults.length - 1, queryResult.getQueryResults().getRows().size());
			assertEquals(null, queryResult.getQueryResults().getHeaders().get(0).getId());
			assertEquals(null, queryResult.getQueryResults().getHeaders().get(1).getId());
			assertEquals(expectedResults[0][0], queryResult.getQueryResults().getHeaders().get(0).getName());
			assertEquals(expectedResults[0][1], queryResult.getQueryResults().getHeaders().get(1).getName());
			for (int i = 1; i < expectedResults.length; i++) {
				assertArrayEquals(expectedResults[i], queryResult.getQueryResults().getRows().get(i - 1).getValues().toArray(new String[0]));
			}
		});

		// Downlaod the data to a csv
		boolean includeRowIdAndVersion = true;
		DownloadFromTableResult downloadResult = waitForConsistentStreamQuery(aggregateSql, null, includeRowIdAndVersion, true, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getEtag());
			assertNotNull(response.getResultsFileHandleId());			
		});
		String resultFileContents = asyncHelper.downloadFileHandleFromS3(downloadResult.getResultsFileHandleId());
		// Read the results
		CSVReader copyReader = new CSVReader(new StringReader(resultFileContents));
		List<String[]> copy = copyReader.readAll();
		copyReader.close();
		assertNotNull(copy);
		assertEquals(expectedResults.length, copy.size());
		for (int i = 0; i < expectedResults.length; i++) {
			assertArrayEquals(expectedResults[i], copy.get(i));
		}
	}
	
	@Test
	public void testCSVDownloadWithFacets() throws Exception{
		//setup
		facetTestSetup();
		boolean includeRowIdAndVersion = false;
		String sql = "select i0 from " + tableId;
		FacetColumnValuesRequest facetRequest = new FacetColumnValuesRequest();
		facetRequest.setColumnName("i0");
		facetRequest.setFacetValues(Sets.newHashSet("string0", "string2"));
		List<FacetColumnRequest> selectedFacets = new ArrayList<>();
		selectedFacets.add(facetRequest);
		
		// download the data to a csv
		DownloadFromTableResult downloadResult = waitForConsistentStreamQuery(sql, selectedFacets, includeRowIdAndVersion, true, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getEtag());
			assertNotNull(response.getResultsFileHandleId());			
		});
		
		String resultFileContents = asyncHelper.downloadFileHandleFromS3(downloadResult.getResultsFileHandleId());
		// Read the results
		CSVReader copyReader = new CSVReader(new StringReader(resultFileContents));
		List<String[]> copy = copyReader.readAll();
		copyReader.close();
		assertNotNull(copy);
		
		//compare the results
		String[][] expectedResults = { { "i0" }, { "string0" }, { "string2" } };
		assertEquals(expectedResults.length, copy.size());
		for (int i = 0; i < copy.size(); i++) {
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
		authenticationManager.setTermsOfUseAcceptance(userId, true);
		UserInfo owner = userManager.getUserInfo(userId);
		users.add(owner);

		user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		userId = userManager.createUser(user);
		certifiedUserManager.setUserCertificationStatus(adminUserInfo, userId, true);
		authenticationManager.setTermsOfUseAcceptance(userId, true);
		UserInfo notOwner = userManager.getUserInfo(userId);
		users.add(notOwner);

		// Create one column of each type
		List<ColumnModel> columnModels = TableModelTestUtils.createOneOfEachType();
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : columnModels) {
			// skip files 
			if(ColumnType.FILEHANDLEID.equals(cm.getColumnType())){
				continue;
			}
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		Project project = new Project();
		project.setName("Proj-" + UUID.randomUUID().toString());
		projectId = entityManager.createEntity(owner, project, null);

		List<String> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		table.setParentId(projectId);
		tableId = entityManager.createEntity(owner, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		tableEntityManager.setTableSchema(adminUserInfo, headers, tableId);
		appendRows(owner, tableId,
				createRowSet(headers), mockProgressCallback);
		assertThrows(UnauthorizedException.class, ()->{
			appendRows(notOwner, tableId,
					createRowSet(headers), mockProgressCallback);
		});

		// Wait for the table to become available
		String sql = "select * from " + tableId + " order by row_id";

		waitForConsistentQuery(owner, sql, null, 8L, (response) -> {
			assertNotNull(response);
		});
		
		assertThrows(UnauthorizedException.class, ()->{
			waitForConsistentQuery(notOwner, sql, null, 8L, (response) -> {
				fail("Should have not receive a response");
			});
		});

		// add users to acl
		AccessControlList acl = entityAclManager.getACL(projectId, adminUserInfo);
		acl.setId(tableId);
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		acl = entityAclManager.getACL(tableId, adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(notOwner.getId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE));
		acl.getResourceAccess().add(ra);
		acl = entityAclManager.updateACL(acl, adminUserInfo);

		appendRows(notOwner, tableId,
				createRowSet(headers), mockProgressCallback);
		
		waitForConsistentQuery(notOwner, sql, null, 8L, (response) -> {
			assertNotNull(response);
		});

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
		assertThrows(UnauthorizedException.class, ()->{
			waitForConsistentQuery(notOwner, sql+" limit 100", null, 8L, (response) -> {
				fail("Should have not receive a response");
			});
		});

		AccessApproval aa = new AccessApproval();
		aa.setAccessorId(notOwner.getId().toString());
		aa.setRequirementId(downloadAR.getId());
		accessApprovalManager.createAccessApproval(notOwner, aa);

		waitForConsistentQuery(notOwner, sql, null, 8L, (response) -> {
			assertNotNull(response);
		});
		
		appendRows(notOwner, tableId,
				createRowSet(headers), mockProgressCallback);
	}
	
	@Test
	public void testPLFM_4051() throws DatastoreException, NotFoundException, IOException{
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setName("aString");
		oldColumn.setColumnType(ColumnType.STRING);
		oldColumn.setMaximumSize(50L);;
		oldColumn = columnManager.createColumnModel(adminUserInfo, oldColumn);
		
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		
		List<String> headers = Lists.newArrayList(oldColumn.getId());
		
		// Create the table with the column
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		tableEntityManager.setTableSchema(adminUserInfo, headers, tableId);
		
		// Add a row
		PartialRow row = new PartialRow();
		row.setValues(Collections.singletonMap(oldColumn.getId(), "string"));
		PartialRowSet set = new PartialRowSet();
		set.setRows(Lists.newArrayList(row));
		set.setTableId(tableId);
		// append the row
		appendPartialRows(adminUserInfo, tableId, set, mockProgressCallback);
		
		// Now change the column type.
		ColumnModel newColumn = new ColumnModel();
		newColumn.setName("aString");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(100L);
		newColumn = columnManager.createColumnModel(adminUserInfo, newColumn);
		
		schema = Lists.newArrayList(newColumn);
		headers = Lists.newArrayList(newColumn.getId());
		
		TableSchemaChangeRequest updateRequest = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(oldColumn.getId());
		change.setNewColumnId(newColumn.getId());
		updateRequest.setChanges(Lists.newArrayList(change));
		updateRequest.setEntityId(tableId);
		// change the column type
		updateTable(mockProgressCallbackVoid, adminUserInfo, updateRequest);
		
		// now try to make a partial row change.
		row = new PartialRow();
		row.setRowId(0L);
		row.setValues(Collections.singletonMap(newColumn.getId(), "string-two"));
		set = new PartialRowSet();
		set.setRows(Lists.newArrayList(row));
		set.setTableId(tableId);
		// This call was failing to merge the partial row with the previous value.
		appendPartialRows(adminUserInfo, tableId, set, mockProgressCallback);
	}
	
	/**
	 * This bug occurs when RowSets are applied to a table that do not include all columns.
	 * When the table worker would apply such a change set it would delete the missing columns
	 * causing all the data in the column to be lost.
	 * @throws Exception 
	 * 
	 */
	@Test
	public void testPLFM_4089() throws Exception{
		// setup a simple two column schema
		schema = Lists.newArrayList(
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(0L, "aBoolean", ColumnType.BOOLEAN)),
				columnManager.createColumnModel(adminUserInfo, TableModelTestUtils.createColumn(0L, "anInteger", ColumnType.INTEGER)));
		
		createTableWithSchema();
		// Apply a rowset with all columns
		List<String> rowOneValues = Lists.newArrayList("true","123");
		addRowToTable(schema, rowOneValues);
		
		// Apply a row set with only the first column
		List<ColumnModel> firstColumnOnly = schema.subList(0, 1);
		List<String> rowTwoValues = Lists.newArrayList("false");
		addRowToTable(firstColumnOnly, rowTwoValues);

		// Wait for the table and check the results.
		String sql = "select * from " + tableId;
		waitForConsistentQuery(adminUserInfo, sql, null, null, (queryResult) -> {
			List<Row> queryRows = queryResult.getQueryResults().getRows();
			assertEquals(2, queryRows.size());
			assertEquals(rowOneValues, queryRows.get(0).getValues());
			assertEquals(Lists.newArrayList("false", null), queryRows.get(1).getValues());			
		});
	}
	
	@Test
	public void testFacetNoneSelected() throws Exception{
		facetTestSetup();
		long expectedMin = 203000;
		long expectedMax = 203005;
		query.setSql(simpleSql);
		query.setOffset(5L);
		query.setLimit(1L);
		queryOptions.withReturnFacets(true);
		
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {
			List<FacetColumnResult> facets = queryResultBundle.getFacets();
			assertNotNull(facets);
			assertEquals(3, facets.size());
			
			
			//first facet should be string
			FacetColumnResult strFacet = facets.get(0);
			assertEquals(FacetType.enumeration, strFacet.getFacetType());
			assertTrue(strFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> enumValues = ((FacetColumnResultValues) strFacet).getFacetValues();
			for(int i = 0; i < enumValues.size() ; i++){
				FacetColumnResultValueCount enumVal = enumValues.get(i);
				assertEquals(1, enumVal.getCount().longValue());
				assertEquals("string" + i, enumVal.getValue());
			}
			
			//second facet is an integer
			FacetColumnResult intFacet = facets.get(1);
			assertEquals(FacetType.range, intFacet.getFacetType());
			assertTrue(intFacet instanceof FacetColumnResultRange);
			FacetColumnResultRange facetRange = (FacetColumnResultRange) intFacet;
			assertNotNull(facetRange);
			assertEquals(expectedMin, Long.parseLong(facetRange.getColumnMin()));
			assertEquals(expectedMax, Long.parseLong(facetRange.getColumnMax()));

			//third facet is an STRING_LIST
			FacetColumnResult strListFacet = facets.get(2);
			assertEquals(FacetType.enumeration, strListFacet.getFacetType());
			assertTrue(strListFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> listEnumerationValues = ((FacetColumnResultValues) strListFacet).getFacetValues();
			for(int i = 0; i < listEnumerationValues.size() ; i++){
				FacetColumnResultValueCount enumVal = listEnumerationValues.get(i);
				assertEquals(1, enumVal.getCount().longValue());
				//each row list column has 2 values so first 2 rows will have row 1 and row 2
				if(i < listEnumerationValues.size() / 2) {
					assertEquals("otherstring120000" + i % (listEnumerationValues.size() / 2), enumVal.getValue());
				}else{
					assertEquals("string120000" + i % (listEnumerationValues.size() / 2), enumVal.getValue());
				}
			}
		});
	}

	@Test
	public void testFacet_SingleValueColumnSelected() throws Exception{
		facetTestSetup();

		long expectedMin = 203000;
		long expectedMax = 203003;
		
		List<FacetColumnRequest> selectedFacets = new ArrayList<>();
		FacetColumnRequest selectedColumn = new FacetColumnValuesRequest();
		//select values on the non-list column
		selectedColumn.setColumnName("i0");
		Set<String> facetValues = new HashSet<>();
		facetValues.add("string0");
		facetValues.add("string3");
		((FacetColumnValuesRequest)selectedColumn).setFacetValues(facetValues);
		selectedFacets.add(selectedColumn);
		
		query.setSql(simpleSql);
		query.setOffset(5L);
		query.setLimit(1L);
		query.setSelectedFacets(selectedFacets);
		queryOptions.withReturnFacets(true);
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {
			List<FacetColumnResult> facets = queryResultBundle.getFacets();
			assertNotNull(facets);
			assertEquals(3, facets.size());
			
			
			//first facet should be string
			FacetColumnResult strFacet = facets.get(0);
			assertEquals(FacetType.enumeration, strFacet.getFacetType());
			assertTrue(strFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> enumValues = ((FacetColumnResultValues)strFacet).getFacetValues();
			//selecting facet within same category should not affect the existence and counts of the other values
			for(int i = 0; i < enumValues.size() ; i++){
				FacetColumnResultValueCount enumVal = enumValues.get(i);
				assertEquals(1, enumVal.getCount().longValue());
				assertEquals("string" + i, enumVal.getValue());
			}
			
			//second facet is an integer range
			FacetColumnResult intFacet = facets.get(1);
			assertEquals(FacetType.range, intFacet.getFacetType());
			assertTrue(intFacet instanceof FacetColumnResultRange);
			FacetColumnResultRange facetRange = (FacetColumnResultRange) intFacet;
			assertNotNull(facetRange);
			//it should affect the values in other columns when a facet of another column is selected
			assertEquals(expectedMin, Long.parseLong(facetRange.getColumnMin()));
			assertEquals(expectedMax, Long.parseLong(facetRange.getColumnMax()));


			//third facet should be string_list
			FacetColumnResult strListFacet = facets.get(2);
			assertEquals(FacetType.enumeration, strListFacet.getFacetType());
			assertTrue(strListFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> enumListValues = ((FacetColumnResultValues)strListFacet).getFacetValues();
			// the List's values should have been affected by selecting from the single value columns
			// facet results for the list should match the selected values for column "i0" since the generated values
			// simply wrap the exact same non-list values values into a list.
			// (e.g. column "i0" (non-list) has value "string0" and column "i10" (list) has value "[\"string0\"]")
			assertEquals(4, enumListValues.size());
			assertEquals("otherstring1200000", enumListValues.get(0).getValue());
			assertEquals((Long) 1L, enumListValues.get(0).getCount());

			assertEquals("otherstring1200003", enumListValues.get(1).getValue());
			assertEquals((Long) 1L, enumListValues.get(1).getCount());

			assertEquals("string1200000", enumListValues.get(2).getValue());
			assertEquals((Long) 1L, enumListValues.get(2).getCount());

			assertEquals("string1200003", enumListValues.get(3).getValue());
			assertEquals((Long) 1L, enumListValues.get(3).getCount());
		});
	}


	@Test
	public void testUnnest_returnCount() throws Exception{
		//reproduces PLFM-6206

		facetTestSetup();
		long expectedMin = 203000;
		long expectedMax = 203005;
		query.setSql("SELECT UNNEST(i12) from " + tableId);
		query.setLimit(25L);
		query.setOffset(0L);
		queryOptions.withReturnFacets(true);

		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {			
			List<Row> rows = queryResultBundle.getQueryResult().getQueryResults().getRows();
			
			//6 rows are created as part of the setup, each with 2 elements in the list
			assertEquals(12, rows.size());
		});
	}

	//reproduces PLFM-6235
	@Test
	public void testUnnest_nullColumns() throws Exception{
		// setup an string list column.
		ColumnModel strListColumn = new ColumnModel();
		strListColumn.setColumnType(ColumnType.STRING_LIST);
		strListColumn.setName("strList");
		strListColumn = columnManager.createColumnModel(adminUserInfo, strListColumn);
		schema = Lists.newArrayList(strListColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));


		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "[\"asdf1\",\"asdf2\",\"asdf3\"]"),
				TableModelTestUtils.createRow(null, null, "[\"qwer1\",\"qwer2\"]"),
				TableModelTestUtils.createRow(null, null, (String) null)));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select ROW_ID, UNNEST("+strListColumn.getName()+") from " + tableId, null, null, (QueryResult result) -> {
			//UNNEST() should expand into 6 rows accounting for the null row
			assertEquals(6, result.getQueryResults().getRows().size());
			Row expectedNullRow = new Row();
			expectedNullRow.setRowId(3L);
			expectedNullRow.setVersionNumber(1L);
			// ROWID, UNNEST(strListColumn)
			expectedNullRow.setValues(Arrays.asList("3", null));
			assertTrue(result.getQueryResults().getRows().contains(expectedNullRow));
		});
	}


	@Test
	public void testFacet_ListColumnValueSelected() throws Exception{
		facetTestSetup();

		long expectedMin = 203000;
		long expectedMax = 203003;

		List<FacetColumnRequest> selectedFacets = new ArrayList<>();
		FacetColumnRequest selectedColumn = new FacetColumnValuesRequest();
		//select values on the list column
		selectedColumn.setColumnName("i12");
		Set<String> facetValues = new HashSet<>();
		facetValues.add("string1200000");
		facetValues.add("otherstring1200003");
		((FacetColumnValuesRequest)selectedColumn).setFacetValues(facetValues);
		selectedFacets.add(selectedColumn);

		query.setSql(simpleSql);
		query.setOffset(5L);
		query.setLimit(1L);
		query.setSelectedFacets(selectedFacets);
		queryOptions.withReturnFacets(true);
		
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {			
			List<FacetColumnResult> facets = queryResultBundle.getFacets();
			assertNotNull(facets);
			assertEquals(3, facets.size());
			//first facet should be string
			FacetColumnResult strFacet = facets.get(0);
			assertEquals(FacetType.enumeration, strFacet.getFacetType());
			assertTrue(strFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> enumValues = ((FacetColumnResultValues)strFacet).getFacetValues();
			// the non-list column's values should have been affected by selecting from the single value columns
			// the generated values simply wrap the exact same non-list values values into a list.
			// (e.g. column "i0" (non-list) has value "string0" and column "i10" (list) has value "[\"string0\"]")
			assertEquals(2, enumValues.size());
			assertEquals("string0", enumValues.get(0).getValue());
			assertEquals((Long) 1L, enumValues.get(0).getCount());

			assertEquals("string3", enumValues.get(1).getValue());
			assertEquals((Long) 1L, enumValues.get(1).getCount());


			//second facet is an integer range
			FacetColumnResult intFacet = facets.get(1);
			assertEquals(FacetType.range, intFacet.getFacetType());
			assertTrue(intFacet instanceof FacetColumnResultRange);
			FacetColumnResultRange facetRange = (FacetColumnResultRange) intFacet;
			assertNotNull(facetRange);
			//it should affect the values in other columns when a facet of another column is selected
			assertEquals(expectedMin, Long.parseLong(facetRange.getColumnMin()));
			assertEquals(expectedMax, Long.parseLong(facetRange.getColumnMax()));

			//third facet is a string_list
			FacetColumnResult strListFacet = facets.get(2);
			assertEquals(FacetType.enumeration, strListFacet.getFacetType());
			assertTrue(strListFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> listEnumerationValues = ((FacetColumnResultValues) strListFacet).getFacetValues();
			//selecting facet within same category should not affect the existence and counts of the other values
			for(int i = 0; i < listEnumerationValues.size() ; i++){
				FacetColumnResultValueCount enumVal = listEnumerationValues.get(i);
				assertEquals(1, enumVal.getCount().longValue());
				//each row list column has 2 values so first 2 rows will have row 1 and row 2
				if(i < listEnumerationValues.size() / 2) {
					assertEquals("otherstring120000" + i % (listEnumerationValues.size() / 2), enumVal.getValue());
				}else{
					assertEquals("string120000" + i % (listEnumerationValues.size() / 2), enumVal.getValue());
				}
			}
		});

	}


	@Test
	public void testFacet_ListColumnEmptyJSONStringEmptyArrayValues() throws Exception{
		ColumnModel defaultEmptyList = new ColumnModel();
		defaultEmptyList.setName("defaultEmptyList");
		defaultEmptyList.setDefaultValue("[]");
		defaultEmptyList.setFacetType(FacetType.enumeration);
		defaultEmptyList.setColumnType(ColumnType.STRING_LIST);
		defaultEmptyList = columnManager.createColumnModel(adminUserInfo, defaultEmptyList);

		schema = Collections.singletonList(defaultEmptyList);
		createTableWithSchema();


		RowSet rowSet = new RowSet();
		Row row = new Row();
		row.setValues(Collections.singletonList("[]"));
		Row row2 = new Row();
		row2.setValues(Collections.singletonList("[\"actually has value\"]"));
		Row row3 = new Row();
		row3.setValues(Collections.singletonList(null));
		Row row4 = new Row();
		row4.setValues(Collections.singletonList(""));

		rowSet.setRows(Arrays.asList(row, row2, row3, row4));

		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		simpleSql = "select * from " + tableId;

		query.setSql(simpleSql);


		//verify facets
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {
			List<FacetColumnResult> facets = queryResultBundle.getFacets();
			assertNotNull(facets);
			assertEquals(1, facets.size());
			//first facet should be string
			FacetColumnResult strFacet = facets.get(0);
			assertEquals(FacetType.enumeration, strFacet.getFacetType());
			assertTrue(strFacet instanceof FacetColumnResultValues);
			List<FacetColumnResultValueCount> enumValues = ((FacetColumnResultValues)strFacet).getFacetValues();

			assertEquals(2, enumValues.size());

			assertEquals(NULL_VALUE_KEYWORD, enumValues.get(0).getValue());
			assertEquals( 3L, enumValues.get(0).getCount());

			assertEquals("actually has value", enumValues.get(1).getValue());
			assertEquals( 1L, enumValues.get(1).getCount());
		});

		FacetColumnValuesRequest facetSelection = new FacetColumnValuesRequest();
		facetSelection.setColumnName(defaultEmptyList.getName());
		facetSelection.setFacetValues(Collections.singleton(NULL_VALUE_KEYWORD));

		List<FacetColumnRequest> selectedFacets = Collections.singletonList(facetSelection);
		query.setSql(simpleSql);
		query.setSelectedFacets(selectedFacets);
		queryOptions.withReturnFacets(true);
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (queryResultBundle) -> {
			//3 rows should be null
			assertEquals(3, queryResultBundle.getQueryResult().getQueryResults().getRows().size());
		});

	}

	@Test
	public void testRenameListColumn() throws Exception{
		// setup an EntityId column.
		ColumnModel startColumn = new ColumnModel();
		startColumn.setColumnType(ColumnType.STRING_LIST);
		startColumn.setName("startColumn");
		startColumn = columnManager.createColumnModel(adminUserInfo, startColumn);
		schema = Lists.newArrayList(startColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));


		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "[\"1\",\"2\",\"3\"]"),
				TableModelTestUtils.createRow(null, null, "[\"3\",\"4\",\"5\"]")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where startColumn has ('3')", null, null, (queryResult) -> {			
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertEquals(Arrays.asList("[\"1\", \"2\", \"3\"]"), queryResult.getQueryResults().getRows().get(0).getValues());
			assertEquals(Arrays.asList("[\"3\", \"4\", \"5\"]"), queryResult.getQueryResults().getRows().get(1).getValues());
		});

		// now rename the column
		ColumnModel updateColumn = new ColumnModel();
		updateColumn.setColumnType(ColumnType.STRING_LIST);
		updateColumn.setName("updatedColumn");
		updateColumn = columnManager.createColumnModel(adminUserInfo, updateColumn);
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(startColumn.getId());
		change.setNewColumnId(updateColumn.getId());
		schemaChangeRequest.setChanges(Lists.newArrayList(change));
		schemaChangeRequest.setEntityId(tableId);
		updateTable(mockProgressCallback, adminUserInfo, schemaChangeRequest);

		// wait for the table.
		status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));

		//query the column expecting the index table to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where updatedColumn has ('3')", null, null, (result) ->{			
			assertEquals(2, result.getQueryResults().getRows().size());
			//result should not have changed
			assertEquals(Arrays.asList("[\"1\", \"2\", \"3\"]"), result.getQueryResults().getRows().get(0).getValues());
			assertEquals(Arrays.asList("[\"3\", \"4\", \"5\"]"), result.getQueryResults().getRows().get(1).getValues());
		});
	}

	@Test
	public void testNotHasQuery() throws Exception{
		// setup an EntityId column.
		ColumnModel startColumn = new ColumnModel();
		startColumn.setColumnType(ColumnType.STRING_LIST);
		startColumn.setName("startColumn");
		startColumn = columnManager.createColumnModel(adminUserInfo, startColumn);
		schema = Lists.newArrayList(startColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));


		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "[\"asdf\",\"qwerty\",\"ggggg\"]"),
				TableModelTestUtils.createRow(null, null, "[\"asdf\",\"ayyyyyy\",\"ggggg\"]"),
				TableModelTestUtils.createRow(null, null, "[\"aAAAAAAA\",\"yeeeeet\",\"ggggg\"]")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where startColumn not has ('asdf') order by row_id", null, null, (queryResult) -> {
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertEquals(Arrays.asList("[\"aAAAAAAA\", \"yeeeeet\", \"ggggg\"]"), queryResult.getQueryResults().getRows().get(0).getValues());
		});

		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where startColumn not has ('ayyyyyy') order by row_id", null, null, (queryResult) -> {
			assertEquals(2, queryResult.getQueryResults().getRows().size());
			assertEquals(Arrays.asList("[\"asdf\", \"qwerty\", \"ggggg\"]"), queryResult.getQueryResults().getRows().get(0).getValues());
			assertEquals(Arrays.asList("[\"aAAAAAAA\", \"yeeeeet\", \"ggggg\"]"), queryResult.getQueryResults().getRows().get(1).getValues());

		});

		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where startColumn not has ('ggggg') order by row_id", null, null, (queryResult) -> {
			assertEquals(0, queryResult.getQueryResults().getRows().size());
		});
	}

	@Test
	public void testCurrentUserQuery() throws Exception{
		// setup an EntityId column.
		ColumnModel userIDColumn = new ColumnModel();
		userIDColumn.setColumnType(ColumnType.USERID);
		userIDColumn.setName("userIDcolumn");
		userIDColumn = columnManager.createColumnModel(adminUserInfo, userIDColumn);
		schema = Lists.newArrayList(userIDColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		assertTrue(TableState.AVAILABLE.equals(status.getState()));

		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, adminUserInfo.getId().toString()),
				TableModelTestUtils.createRow(null, null, "2"),
				TableModelTestUtils.createRow(null, null, "3")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column and check if the results match the userId
		waitForConsistentQuery(adminUserInfo, "select userIDcolumn from " + tableId + " where userIDcolumn = CURRENT_USER()", null, null, (queryResult) -> {
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertEquals(1, queryResult.getQueryResults().getRows().get(0).getValues().size());
			assertEquals(Arrays.asList(adminUserInfo.getId().toString()), queryResult.getQueryResults().getRows().get(0).getValues());
		});
		waitForConsistentQuery(adminUserInfo, "select CURRENT_USER() from " + tableId + " where userIDcolumn = CURRENT_USER()", null, null, (queryResult) -> {
			assertEquals(1, queryResult.getQueryResults().getRows().size());
			assertEquals(1, queryResult.getQueryResults().getRows().get(0).getValues().size());
			assertEquals(Arrays.asList(adminUserInfo.getId().toString()), queryResult.getQueryResults().getRows().get(0).getValues());
		});
	}

	@Test
	public void testChangeListColumnMaximumListLength_valueBelowMaxLists() throws Exception{
		// setup an EntityId column.
		ColumnModel startColumn = new ColumnModel();
		startColumn.setColumnType(ColumnType.STRING_LIST);
		startColumn.setName("startColumn");
		startColumn = columnManager.createColumnModel(adminUserInfo, startColumn);
		schema = Lists.newArrayList(startColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));


		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "[\"1\",\"2\",\"3\"]"),
				TableModelTestUtils.createRow(null, null, "[\"3\",\"4\",\"5\"]")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where startColumn has ('3')", null, null, (result) -> {
			assertEquals(2, result.getQueryResults().getRows().size());
			assertEquals(Arrays.asList("[\"1\", \"2\", \"3\"]"), result.getQueryResults().getRows().get(0).getValues());
			assertEquals(Arrays.asList("[\"3\", \"4\", \"5\"]"), result.getQueryResults().getRows().get(1).getValues());			
		});

		// now rename the column
		ColumnModel updateColumn = new ColumnModel();
		updateColumn.setColumnType(ColumnType.STRING_LIST);
		updateColumn.setName("startColumn");
		// set maximum list length below actual value
		updateColumn.setMaximumListLength(2L);
		updateColumn = columnManager.createColumnModel(adminUserInfo, updateColumn);
		assertNotEquals(updateColumn.getId(), startColumn.getId());
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(startColumn.getId());
		change.setNewColumnId(updateColumn.getId());
		schemaChangeRequest.setChanges(Lists.newArrayList(change));
		schemaChangeRequest.setEntityId(tableId);
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setChanges(Collections.singletonList(schemaChangeRequest));
		transactionRequest.setEntityId(tableId);
		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
			asyncHelper.assertJobResponse(adminUserInfo, transactionRequest, (response)-> {
				fail("Should have not get back a response");
			}, MAX_WAIT_MS)
		).getMessage();

		String expectedMessage = "maximumListLength for ColumnModel \"startColumn\" must be at least: 3";
		assertEquals(expectedMessage, errorMessage);
	}

	@Test
	public void testUserIdListAndEntityIdList() throws Exception{
		// setup an EntityIdList and UserIdList column.
		ColumnModel entityIdList = new ColumnModel();
		entityIdList.setColumnType(ColumnType.ENTITYID_LIST);
		entityIdList.setName("entityIdList");
		entityIdList = columnManager.createColumnModel(adminUserInfo, entityIdList);
		ColumnModel userIdList = new ColumnModel();
		userIdList.setColumnType(ColumnType.USERID_LIST);
		userIdList.setName("userIdList");
		userIdList = columnManager.createColumnModel(adminUserInfo, userIdList);
		schema = Lists.newArrayList(entityIdList, userIdList);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));


		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				//some values for entity id have syn prefix while others don't
				TableModelTestUtils.createRow(null, null, "[\"syn1\",\"2\",\"syn3\"]", "[\"9\",\"8\",\"7\"]"),
				TableModelTestUtils.createRow(null, null, "[\"3\",\"syn4\",\"5\"]", "[\"6\",\"5\",\"4\"]"),
				TableModelTestUtils.createRow(null, null, "[\"6\",\"syn7\",\"8\"]", "[\"3\",\"2\",\"1\"]")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where entityIdList has ('8', 'syn1')", null, null, (result) -> {			
			assertEquals(2, result.getQueryResults().getRows().size());
			List<List<String>> resultsRows = result.getQueryResults().getRows().stream().map(Row::getValues).collect(Collectors.toList());

			assertTrue(resultsRows.contains(Arrays.asList("[\"syn1\",\"syn2\",\"syn3\"]", "[9, 8, 7]")));
			assertTrue(resultsRows.contains(Arrays.asList("[\"syn6\",\"syn7\",\"syn8\"]", "[3, 2, 1]")));
		});

		//query the column expecting the index table for it to be populated
		waitForConsistentQuery(adminUserInfo, "select * from " + tableId + " where userIdList has (9, 5)", null, null, (result) -> {			
			assertEquals(2, result.getQueryResults().getRows().size());
			List<List<String>> resultsRows = result.getQueryResults().getRows().stream().map(Row::getValues).collect(Collectors.toList());

			assertTrue(resultsRows.contains(Arrays.asList("[\"syn1\",\"syn2\",\"syn3\"]", "[9, 8, 7]")));
			assertTrue(resultsRows.contains(Arrays.asList("[\"syn3\",\"syn4\",\"syn5\"]", "[6, 5, 4]")));
		});

	}

	@Test
	public void testPLFM_4161() throws Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("5ormore");
		cm.setColumnType(ColumnType.INTEGER);
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema = Lists.newArrayList(cm);
		createTableWithSchema();
		// Apply a rowset with all columns
		List<String> rowOneValues = Lists.newArrayList("123");
		addRowToTable(schema, rowOneValues);
		
		// Wait for the table and check the results.
		String sql = "select * from " + tableId;
		waitForConsistentQuery(adminUserInfo, sql, null, null, (queryResult) -> {			
			List<Row> queryRows = queryResult.getQueryResults().getRows();
			assertEquals(1, queryRows.size());
		});
	}
	
	@Test
	public void testPLFM_4186() throws Exception {
		// Create one column of each type
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// Add null rows
		rows.addAll(TableModelTestUtils.createNullRows(schema, 2));
		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		long start = System.currentTimeMillis();
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		System.out.println("Appended "+rowSet.getRows().size()+" rows in: "+(System.currentTimeMillis()-start)+" MS");
		
		// delete the table schema
		tableEntityManager.setTableAsDeleted(tableId);
		String localTableId = tableId;
		// Get the table status
		TableStatus status = waitForTableProcessing(localTableId);
		assertNotNull(status);
		assertEquals(TableState.AVAILABLE, status.getState());
	}
	
	/**
	 * Test for PLFM-4216, PLFM-4088, PLFM-4203
	 * @throws IOException 
	 * @throws NotFoundException 
	 * @throws Exception 
	 */
	@Test
	public void testEntityIdColumns() throws Exception {
		// setup an EntityId column.
		ColumnModel entityIdColumn = new ColumnModel();
		entityIdColumn.setColumnType(ColumnType.ENTITYID);
		entityIdColumn.setName("anEntityId");
		entityIdColumn.setFacetType(FacetType.enumeration);
		entityIdColumn = columnManager.createColumnModel(adminUserInfo, entityIdColumn);
		schema = Lists.newArrayList(entityIdColumn);
		// build a table with this column.
		createTableWithSchema();
		// add rows to the table.
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "syn123"),
				TableModelTestUtils.createRow(null, null, "syn456"),
				TableModelTestUtils.createRow(null, null, "syn789"),
				TableModelTestUtils.createRow(null, null, "syn123")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		
		String sql = "select * from " + tableId;
		
		waitForConsistentQuery(adminUserInfo, sql, null, 1L, (response) -> {
			assertNotNull(response);
		});
		
		// setup and run a faceted query
		List<FacetColumnRequest> selectedFacets = new ArrayList<>();
		FacetColumnRequest selectedColumn = new FacetColumnValuesRequest();
		selectedColumn.setColumnName(entityIdColumn.getName());
		Set<String> facetValues = new HashSet<>();
		facetValues.add("syn123");
		((FacetColumnValuesRequest)selectedColumn).setFacetValues(facetValues);
		selectedFacets.add(selectedColumn);
		
		query.setSql(sql);
		query.setSelectedFacets(selectedFacets);
		queryOptions.withReturnFacets(true);
		
		waitForConsistentQueryBundle(adminUserInfo, query, queryOptions, (results) -> {			
			assertNotNull(results);
			assertNotNull(results);
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			List<Row> rows = results.getQueryResult().getQueryResults().getRows();
			assertEquals(2, rows.size());
			Row row = rows.get(0);
			assertNotNull(row);
			assertNotNull(row.getValues());
			assertEquals(1, row.getValues().size());
			assertEquals("syn123", row.getValues().get(0));
		});
	
	}
	
	/**
	 * PLFM-4254 & PLFM-4155 occur when a column change is made to a table with
	 * no rows.  The problem only manifests after the table is rebuilt.
	 * @throws Exception 
	 */
	@Test
	public void testPLFM_4245() throws Exception{
		// setup an EntityId column.
		ColumnModel startColumn = new ColumnModel();
		startColumn.setColumnType(ColumnType.INTEGER);
		startColumn.setName("startColumn");
		startColumn = columnManager.createColumnModel(adminUserInfo, startColumn);
		schema = Lists.newArrayList(startColumn);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));
		// now rename the column
		ColumnModel updateColumn = new ColumnModel();
		updateColumn.setColumnType(ColumnType.INTEGER);
		updateColumn.setName("updatedColumn");
		updateColumn = columnManager.createColumnModel(adminUserInfo, updateColumn);
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(startColumn.getId());
		change.setNewColumnId(updateColumn.getId());
		schemaChangeRequest.setChanges(Lists.newArrayList(change));
		schemaChangeRequest.setEntityId(tableId);
		updateTable(mockProgressCallback, adminUserInfo, schemaChangeRequest);
		
		// wait for the table.
		status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// Trigger a full rebuild of the table
		TableIndexDAO dao = tableConnectionFactory.getConnection(idAndVersion);
		dao.deleteTable(idAndVersion);
		// The rebuild should not fails
		status = waitForTableProcessing(tableId);
		if(status.getErrorDetails() != null){
			System.out.println(status.getErrorDetails());
		}
		assertTrue(TableState.AVAILABLE.equals(status.getState()));
	}
	
	/**
	 * PLFM-4303 occurs when a column name either is a reserved word
	 * or contains a reserved word.
	 * @throws InterruptedException 
	 * 
	 */
	@Test
	public void testPLFM4303() throws Exception {
		// faceted range column with a keyword name.
		ColumnModel rangeColumn = new ColumnModel();
		rangeColumn.setColumnType(ColumnType.INTEGER);
		rangeColumn.setName("year");
		rangeColumn.setFacetType(FacetType.range);
		rangeColumn = columnManager.createColumnModel(adminUserInfo, rangeColumn);
		// faceted enum column with a keyword name.
		ColumnModel enumColumn = new ColumnModel();
		enumColumn.setColumnType(ColumnType.STRING);
		enumColumn.setMaximumSize(50L);
		enumColumn.setName("day");
		enumColumn.setFacetType(FacetType.enumeration);
		enumColumn = columnManager.createColumnModel(adminUserInfo, enumColumn);
		schema = Lists.newArrayList(rangeColumn, enumColumn);
		// build a table with this column.
		createTableWithSchema();
		// add some rows
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "1970", "Monday"),
				TableModelTestUtils.createRow(null, null, "1990", "Tuesday")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		// query for results and include facets
		FacetColumnRangeRequest range = new FacetColumnRangeRequest();
		range.setColumnName(rangeColumn.getName());
		range.setMin("1980");
		range.setMax("2000");
		// enum request
		FacetColumnValuesRequest enumRequest = new FacetColumnValuesRequest();
		enumRequest.setColumnName(enumColumn.getName());
		enumRequest.setFacetValues(Sets.newHashSet("Tuesday"));
		List<FacetColumnRequest> facetList = Lists.newArrayList((FacetColumnRequest)range, (FacetColumnRequest)enumRequest);
		Query query = new Query();
		query.setSql("select * from "+tableId);
		query.setSelectedFacets(facetList);
		queryOptions = new QueryOptions().withRunQuery(true).withRunCount(true).withReturnFacets(true);
		// call under test (this type of query would fail)
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (results) -> {			
			assertNotNull(results);
		});
	}
	
	/**
	 * Anonymous cannot query a sensitive table.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_5240Sensitive() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		grantReadToPublicOnTable();
		
		// Set the table to be sensitive
		DataType dataType = DataType.SENSITIVE_DATA;
		entityManager.changeEntityDataType(adminUserInfo, tableId, dataType);
		
		// Wait for the table to become available
		String sql = "select row_id from " + tableId;
		query.setSql(sql);
		query.setLimit(8L);
		assertThrows(UnauthorizedException.class, ()->{
			// cannot query a sensitive data table
			waitForConsistentQuery(anonymousUser, query, queryOptions, (response) -> {
				fail("Should have not receive a response");
			});
		});
	}
	
	/**
	 * Anonymous can query a OPEN_DATA table if PUBLIC is granted READ.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_5240Open() throws Exception {
		createSchemaOneOfEachType();
		createTableWithSchema();
		// make the table public read
		grantReadToPublicOnTable();
		
		// Set the table to be open
		DataType dataType = DataType.OPEN_DATA;
		entityManager.changeEntityDataType(adminUserInfo, tableId, dataType);
		
		// Wait for the table to become available
		String sql = "select row_id from " + tableId;
		query.setSql(sql);
		query.setLimit(8L);
		waitForConsistentQuery(anonymousUser, query, queryOptions, (results) -> {			
			assertNotNull(results);
		});
	}

	/**
	 * PLFM-6392
	 * @throws Exception
	 */
	@Test
	public void testQueryWithUnquotedKeyword() throws Exception {
		ColumnModel year = new ColumnModel();
		year.setColumnType(ColumnType.STRING);
		year.setMaximumSize(50L);
		year.setName("year");
		year = columnManager.createColumnModel(adminUserInfo, year);
		schema = Lists.newArrayList(year);
		// build a table with this column.
		createTableWithSchema();
		TableStatus status = waitForTableProcessing(tableId);
		assertTrue(TableState.AVAILABLE.equals(status.getState()));

		Throwable selectClauseException = assertThrows(IllegalArgumentException.class, ()->{
			waitForConsistentQuery(adminUserInfo, "select year from " + tableId, null, null, (queryResult) -> {
				fail("This should have failed with an IllegalArgumentException");
			});
		});
		assertEquals("Unknown column 'YEAR' in 'field list'"
				+ TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE, selectClauseException.getMessage());
		Throwable whereClauseException = assertThrows(IllegalArgumentException.class, ()->{
			waitForConsistentQuery(adminUserInfo, "select \"year\" from " + tableId + " where year = 2020",
					null, null, (queryResult) -> {
				fail("This should have failed with an IllegalArgumentException");
			});
		});
		assertEquals("Encountered \" <date_time_field> \"year \"\" at line 1, column 37.\n" +
				"Was expecting one of:\n" +
				"    \"\\\"\" ...\n" +
				"    \"`\" ...\n" +
				"    \"NOT\" ...\n" +
				"    \"ISNAN\" ...\n" +
				"    \"ISINFINITY\" ...\n" +
				"    <entity_id> ...\n" +
				"    <regular_identifier> ...\n" +
				"    \"(\" ...\n" +
				"    " + TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE, whereClauseException.getMessage());
		Throwable groupbyClauseException = assertThrows(IllegalArgumentException.class, ()->{
			waitForConsistentQuery(adminUserInfo, "select \"year\" from " + tableId + " where \"year\" = 2020 group" +
					" by year", null, null, (queryResult) -> {
				fail("This should have failed with an IllegalArgumentException");
			});
		});
		assertEquals("Unknown column 'YEAR' in 'group statement'" +
				TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE, groupbyClauseException.getMessage());
	}

	@Test
	public void testQueryGroupConcat() throws Exception {
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setMaximumSize(50L);
		foo.setName("foo");
		foo = columnManager.createColumnModel(adminUserInfo, foo);
		// faceted enum column with a keyword name.
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.STRING);
		bar.setMaximumSize(50L);
		bar.setName("bar");
		bar = columnManager.createColumnModel(adminUserInfo, bar);
		schema = Lists.newArrayList(foo, bar);
		// build a table with this column.
		createTableWithSchema();
		// add some rows
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "a", "one"),
				TableModelTestUtils.createRow(null, null, "a", "two"),
				TableModelTestUtils.createRow(null, null, "b", "four"),
				TableModelTestUtils.createRow(null, null, "b", "five")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);

		// Wait for the table to become available
		String sql = "select foo, group_concat(distinct bar order by bar asc separator '#') from " + tableId
				+ " group by foo order by foo desc";
		query.setSql(sql);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (results) -> {					
			assertNotNull(results);
			List<Row> queryRows = results.getQueryResults().getRows();
			assertEquals(2, queryRows.size());
			Row row = queryRows.get(0);
			assertEquals("b", row.getValues().get(0));
			assertEquals("five#four", row.getValues().get(1));
		});
	}
	
	/**
	 * Create a table with the maximum number of LARGE_TEXT columns and add data
	 * at the maximum size.
	 * @throws Exception 
	 */
	@Test
	public void testPLFM_5330() throws Exception {
		schema = new LinkedList<>();
		for(int i=0; i<ColumnConstants.MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setName("max"+i);
			cm.setColumnType(ColumnType.LARGETEXT);
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		// build a table with this column.
		createTableWithSchema();
		// Add one row with the max bytes
		List<String> values = new LinkedList<>();
		for(int i=0; i<ColumnConstants.MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE; i++) {
			/*
			 *  Note: cannot use the max size without hitting 'Packet for query is too large...'
			 *  We would need to increase the 'max_allowed_packet' variable to test at that scale.
			 */
			values.add(createStringOfSize((int) ColumnConstants.MAX_LARGE_TEXT_CHARACTERS/4));
		}
		Row row = new Row();
		row.setValues(values);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(row));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		
		String sql = "select * from " + tableId;
		query.setSql(sql);
		
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (results) -> {			
			assertNotNull(results);
		});
	}
	
	/**
	 * In the past, changing a table's schema via PUT /entity did not add the schema change to the
	 * tables history.  We have since corrected this, but there are still tables
	 * that had such schema changes in production.  When we changed the TableIndexWorker to build
	 * tables only from the table's history to support we discovered a few tables in this state.
	 * The worker would build such tables using the table's history and set its status to available.
	 * When a user would query the table, the query manager would detect the table's index schema
	 * did not match the bound schema and would trigger the table to rebuild.  The result was a table
	 * stuck in a perpetual state of processing.
	 * 
	 * This test creates the same setup for the bug.
	 * @throws Exception 
	 */
	@Test
	public void testPLFM_5639() throws Exception {
		ColumnModel columnOne = new ColumnModel();
		columnOne.setColumnType(ColumnType.INTEGER);
		columnOne.setName("one");
		columnOne = columnManager.createColumnModel(adminUserInfo, columnOne);
		ColumnModel columnTwo = new ColumnModel();
		columnTwo.setColumnType(ColumnType.STRING);
		columnTwo.setMaximumSize(50L);
		columnTwo.setName("two");
		columnTwo = columnManager.createColumnModel(adminUserInfo, columnTwo);
		schema = Lists.newArrayList(columnOne, columnTwo);
		// build a table with this column.
		createTableWithSchema();
		// add a row
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(
				TableModelTestUtils.createRow(null, null, "1", "foo"),
				TableModelTestUtils.createRow(null, null, "2", "bar")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		/* Directly remove one of the columns from the table's bound columns
		 * to simulate the bug state.
		 */
		columnManager.bindColumnsToDefaultVersionOfObject(Lists.newArrayList(columnOne.getId()), tableId);
		// Should be able to query the table
		query.setSql("select * from " + tableId);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {		
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
	}
	
	/**
	 * With PLFM-6305 the index of a table had a higher change number applied to it
	 * than was available in the table's history. This likely occurred by modifying
	 * the table on staging, followed by a migration event that removed the change.
	 * Since the index last change number did not match the latest actual change
	 * number, any query against the table triggered the worker to rebuild the
	 * index. However, the rebuild did not detect the problem and set the index back
	 * to available. The query then triggers the rebuild again, and the thrashing
	 * continues with the query message remaining in the queue indefinitely.
	 * @throws Exception 
	 */
	@Test
	public void testPLFM_6305() throws Exception {
		ColumnModel columnOne = new ColumnModel();
		columnOne.setColumnType(ColumnType.INTEGER);
		columnOne.setName("one");
		columnOne = columnManager.createColumnModel(adminUserInfo, columnOne);
		ColumnModel columnTwo = new ColumnModel();
		columnTwo.setColumnType(ColumnType.STRING);
		columnTwo.setMaximumSize(50L);
		columnTwo.setName("two");
		columnTwo = columnManager.createColumnModel(adminUserInfo, columnTwo);
		schema = Lists.newArrayList(columnOne, columnTwo);
		// build a table with this column.
		createTableWithSchema();
		// add one row to the table as the first change.
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "1", "foo")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		// add a second row as the second change.
		rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(TableModelTestUtils.createRow(null, null, "2", "bar")));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId, rowSet, mockProgressCallback);
		// wait for both rows to appear in the index.
		query.setSql("select * from " + tableId);
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {
			assertEquals(2, queryResult.getQueryResults().getRows().size());
		});
		// Simulate the migration event by deleting the second change from the table's history
		long changeNumber = 2;
		tableRowTruthDAO.deleteChangeNumber(tableId, changeNumber);
		// at this point the table index must get rebuilt to repair
		waitForConsistentQuery(adminUserInfo, query, queryOptions, (queryResult) -> {
			List<Row> queryRows = queryResult.getQueryResults().getRows();
			assertEquals(1, queryRows.size());
			Row row = queryRows.get(0);
			assertEquals("1", row.getValues().get(0));
			assertEquals("foo", row.getValues().get(1));
		});
	}

	/**
	 * Create a string of the given size.
	 * @param numberOfCharacters
	 * @return
	 */
	public String createStringOfSize(int numberOfCharacters) {
		char[] chars = new char[numberOfCharacters];
		Arrays.fill(chars, 'a');
		return new String(chars);
	}

	/**
	 * Helper to grant the read permission to PUBLIC for the current table.
	 * @throws ACLInheritanceException
	 */
	void grantReadToPublicOnTable() throws ACLInheritanceException {
		AccessControlList acl = entityAclManager.getACL(projectId, adminUserInfo);
		acl.setId(tableId);
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		acl = entityAclManager.getACL(tableId, adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		acl.getResourceAccess().add(ra);
		acl = entityAclManager.updateACL(acl, adminUserInfo);
	}
	
	/**
	 * Wait for tables status to change from processing.
	 * 
	 * @param tableId
	 * @return
	 * @throws InterruptedException
	 */
	public TableStatus waitForTableProcessing(String tableId) throws InterruptedException{
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		long start = System.currentTimeMillis();
		while(true){
			TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion);
			if(TableState.PROCESSING.equals(status.getState())){
				assertTrue((System.currentTimeMillis()-start) <  MAX_WAIT_MS, "Timed out waiting for table status to change.");
				System.out.println("Waiting for table status to be available...");
				Thread.sleep(1000);
			}else{
				return status;
			}
		}
	}
	
	/**
	 * Stolen from testLimitOffset()
	 */
	private void facetTestSetup() throws Exception{
		createSchemaOneOfEachType();
		createTableWithSchema();
		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 6));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		referenceSet = appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
		simpleSql = "select * from " + tableId;
		
		waitForConsistentQuery(adminUserInfo, simpleSql, null, 7L, (response) -> {
			assertNotNull(response);
		});
		
		rowSet = tableEntityManager.getCellValues(adminUserInfo, tableId, referenceSet.getRows(),
				schema);
	}
	
	/**
	 * Add a row to the table with the given columns and values.
	 * 
	 * @param columns
	 * @param values
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	RowReferenceSet addRowToTable(List<ColumnModel> columns, List<String> values) throws DatastoreException, NotFoundException, IOException{
		Row row = new Row();
		row.setValues(values);
		RowSet rowSet = new RowSet();
		rowSet.setRows(Lists.newArrayList(row));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(columns));
		rowSet.setTableId(tableId);
		return appendRows(adminUserInfo, tableId,
				rowSet, mockProgressCallback);
	}
	
	/**
	 * Helper to append rows to to a table.
	 * @param user
	 * @param tableId
	 * @param delta
	 * @param progressCallback
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowReferenceSet appendRows(UserInfo user, String tableId, RowSet delta, ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException {
		long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		return tableEntityManager.appendRows(user, tableId,
				delta, mockProgressCallback, transactionId);
	}
	
	/**
	 * Helper to update a table with a transaction.
	 * @param callback
	 * @param userInfo
	 * @param change
	 * @return
	 */
	public TableUpdateResponse updateTable(ProgressCallback callback,
			UserInfo userInfo, TableUpdateRequest change) {
		long transactionId = tableTransactionDao.startTransaction(tableId, userInfo.getId());
		return tableEntityManager.updateTable(callback, userInfo, change, transactionId);
	}
	
	/**
	 * Helper to append rows with a transaction.
	 * @param user
	 * @param tableId
	 * @param rowsToAppendOrUpdateOrDelete
	 * @param progressCallback
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId,
			PartialRowSet rowsToAppendOrUpdateOrDelete, ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException{
		long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		return tableEntityManager.appendPartialRows(user, tableId, rowsToAppendOrUpdateOrDelete, progressCallback, transactionId);
	}
	
	/**
	 * Helper to append rows with a transaction.
	 * @param user
	 * @param tableId
	 * @param columns
	 * @param rowStream
	 * @param etag
	 * @param results
	 * @param progressCallback
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	TableUpdateResponse appendRowsAsStream(UserInfo user, String tableId, List<ColumnModel> columns, Iterator<SparseRowDto> rowStream, String etag,
			RowReferenceSet results, ProgressCallback progressCallback) throws DatastoreException, NotFoundException, IOException{
		long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		return tableEntityManager.appendRowsAsStream(user, tableId, columns, rowStream, etag, results, progressCallback, transactionId);
	}

	private RowSet createRowSet(List<String> headers) {
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		return rowSet;
	}

	/**
	 * Attempt to run a query. If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * 
	 * @param user
	 * @param sql
	 * @return
	 * @throws Exception 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private QueryResult waitForConsistentQuery(UserInfo user, String sql, List<SortItem> sortItems, Long limit, Consumer<QueryResult> responseConsumer) throws Exception {
		Query query = new Query();
		query.setSql(sql);
		query.setSort(sortItems);
		query.setLimit(limit);
		return waitForConsistentQuery(user, query, queryOptions, responseConsumer);
	}
	
	private QueryResult waitForConsistentQuery(UserInfo user, Query query, QueryOptions options, Consumer<QueryResult> responseConsumer) throws Exception {
		return waitForConsistentQueryBundle(user, query, options, (response) -> {
			responseConsumer.accept(response.getQueryResult());
		}).getQueryResult();
	}
	
	private QueryResultBundle waitForConsistentQueryBundle(UserInfo user, String sql, Long offset, Long limit, Consumer<QueryResultBundle> responseConsumer)
			throws Exception {
		Query query = new Query();
		query.setSql(sql);
		query.setOffset(offset);
		query.setLimit(limit);
		
		QueryOptions queryOptions = new QueryOptions();
		queryOptions.withMask(-1L);
		return waitForConsistentQueryBundle(user, query, queryOptions, responseConsumer);
	}
	
	private QueryResultBundle waitForConsistentQueryBundle(UserInfo user, Query query, QueryOptions options, Consumer<QueryResultBundle> responseConsumer) throws Exception {
		return asyncHelper.assertQueryResult(user, query, options, responseConsumer, MAX_WAIT_MS);
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
	DownloadFromTableResult waitForConsistentStreamQuery(String sql, List<FacetColumnRequest> selectedFacets, boolean includeRowIdAndVersion,
			boolean writeHeader, Consumer<DownloadFromTableResult> responseConsumer) throws Exception {
		
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setSql(sql);
		request.setIncludeRowIdAndRowVersion(includeRowIdAndVersion);
		request.setSelectedFacets(selectedFacets);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(writeHeader);
		request.setCsvTableDescriptor(descriptor);
		
		return asyncHelper.assertJobResponse(adminUserInfo, request, responseConsumer, MAX_WAIT_MS).getResponse();
	}

	private void compareValues(RowSet expected, int offset, int count, RowSet actual) {
		assertEquals(count, actual.getRows().size());
		int expectedIndex = offset;
		int actualIndex = 0;
		for (int i = 0; i < count; i++, expectedIndex++, actualIndex++) {
			assertEquals(expected.getRows().get(expectedIndex).getValues().toString(), actual.getRows().get(actualIndex)
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

}
