package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatistics;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.Column;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.EntityNotFoundException;
import com.amazonaws.services.glue.model.GetDatabaseRequest;
import com.amazonaws.services.glue.model.GetDatabaseResult;
import com.amazonaws.services.glue.model.GetDatabasesResult;
import com.amazonaws.services.glue.model.GetTableRequest;
import com.amazonaws.services.glue.model.GetTableResult;
import com.amazonaws.services.glue.model.GetTablesResult;
import com.amazonaws.services.glue.model.Table;

@ExtendWith(MockitoExtension.class)
public class AthenaSupportImplTest {

	private static final String TEST_STACK = "test";
	private static final String TEST_INSTANCE = "123";
	private static final String TEST_DB = "db";
	private static final String TEST_TABLE = "table";
	private static final String TEST_COLUMN = "column";
	private static final String TEST_LOG_BUCKET_NAME = "test.log.bucket";
	private static final String TEST_OUTPUT_RESULTS_LOCATION = "s3://test.log.bucket/000000123/athena";

	@Mock
	private AWSGlue mockGlueClient;

	@Mock
	private AmazonAthena mockAthenaClient;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private GetDatabasesResult mockDatabasesResults;

	@Mock
	private GetTablesResult mockTablesResults;

	@Mock
	private GetTableResult mockTableResult;

	@Mock
	private GetDatabaseResult mockDatabaseResult;

	@Mock
	private StartQueryExecutionResult mockStartQueryResult;

	@Mock
	private GetQueryExecutionResult mockQueryExecutionResult;

	@Mock
	private GetQueryResultsResult mockQueryResult;

	private AthenaSupportImpl athenaSupport;

	@BeforeEach
	public void before() {
		when(mockConfig.getStack()).thenReturn(TEST_STACK);
		when(mockConfig.getLogBucketName()).thenReturn(TEST_LOG_BUCKET_NAME);
		when(mockConfig.getStackInstance()).thenReturn(TEST_INSTANCE);
		when(mockConfig.getStackInstanceNumber()).thenReturn(Integer.valueOf(TEST_INSTANCE));

		athenaSupport = new AthenaSupportImpl(mockGlueClient, mockAthenaClient, mockConfig);
	}

	@Test
	public void testOutputResultLocation() {
		when(mockConfig.getStack()).thenReturn("Stack");
		when(mockConfig.getStackInstance()).thenReturn("Instance");
		when(mockConfig.getLogBucketName()).thenReturn("LogBucket");
		when(mockConfig.getStackInstanceNumber()).thenReturn(Integer.valueOf(123));

		athenaSupport = new AthenaSupportImpl(mockGlueClient, mockAthenaClient, mockConfig);

		assertEquals("s3://logbucket/000000123/athena", athenaSupport.getOutputResultLocation());
	}

	@Test
	public void testGetPartitionedTablesEmpty() throws ServiceUnavailableException {
		List<Database> mockDatabases = Collections.singletonList(new Database().withName(TEST_DB));

		List<Table> mockTables = Collections.singletonList(new Table().withName(TEST_TABLE).withDatabaseName(TEST_DB));

		when(mockDatabasesResults.getDatabaseList()).thenReturn(mockDatabases);
		when(mockTablesResults.getTableList()).thenReturn(mockTables);

		when(mockGlueClient.getDatabases(any())).thenReturn(mockDatabasesResults);
		when(mockGlueClient.getTables(any())).thenReturn(mockTablesResults);

		// Call under test
		List<Table> tables = athenaSupport.getPartitionedTables();

		assertTrue(tables.isEmpty());
		verify(mockGlueClient).getDatabases(any());
		verify(mockGlueClient).getTables(any());
	}

	@Test
	public void testGetPartitionedTables() throws ServiceUnavailableException {
		List<Database> mockDatabases = Collections.singletonList(new Database().withName(TEST_DB));

		List<Table> mockTables = Collections.singletonList(
				new Table().withName(TEST_TABLE).withDatabaseName(TEST_DB).withPartitionKeys(new Column().withName(TEST_COLUMN)));

		when(mockDatabasesResults.getDatabaseList()).thenReturn(mockDatabases);
		when(mockTablesResults.getTableList()).thenReturn(mockTables);

		when(mockGlueClient.getDatabases(any())).thenReturn(mockDatabasesResults);
		when(mockGlueClient.getTables(any())).thenReturn(mockTablesResults);

		// Call under test
		List<Table> tables = athenaSupport.getPartitionedTables();

		assertEquals(mockTables, tables);
		verify(mockGlueClient).getDatabases(any());
		verify(mockGlueClient).getTables(any());
	}

	@Test
	public void testGetTable() throws ServiceUnavailableException {

		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);

		Database database = new Database().withName(databaseName);

		GetTableRequest request = new GetTableRequest().withDatabaseName(databaseName).withName(tableName);

		when(mockTableResult.getTable()).thenReturn(new Table().withDatabaseName(databaseName).withName(tableName));

		when(mockGlueClient.getTable(eq(request))).thenReturn(mockTableResult);

		// Call under test
		Table table = athenaSupport.getTable(database, TEST_TABLE);

		assertNotNull(table);
		assertEquals(tableName, table.getName());
		assertEquals(databaseName, table.getDatabaseName());
		verify(mockGlueClient).getTable(eq(request));
	}

	@Test
	public void testRepairTable() throws ServiceUnavailableException {

		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);

		StartQueryExecutionRequest startQueryRequest = getStartQueryExecutionRequest(databaseName, tableName);

		String queryId = "abcd";

		GetQueryExecutionRequest queryExecutionRequest = getQueryExecutionRequest(queryId);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		when(mockAthenaClient.startQueryExecution(eq(startQueryRequest))).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(eq(queryExecutionRequest))).thenReturn(mockQueryExecutionResult);

		Table table = new Table().withDatabaseName(databaseName).withName(tableName);

		// Call under test
		QueryExecutionStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(expectedStats, queryStats);
		verify(mockAthenaClient).startQueryExecution(eq(startQueryRequest));
		verify(mockAthenaClient).getQueryExecution(eq(queryExecutionRequest));
	}

	@Test
	public void testGetTableNotFound() throws ServiceUnavailableException {

		Database database = new Database().withName(prefixWithInstance(TEST_DB));

		when(mockGlueClient.getTable(any())).thenThrow(EntityNotFoundException.class);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(database, TEST_TABLE);
		});
	}

	@Test
	public void testGetDatabaseInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getDatabase(null);
		});
	}

	@Test
	public void testGetDatabase() throws ServiceUnavailableException {
		String databaseName = "someDatabase";

		Database database = new Database().withName(prefixWithInstance(databaseName));

		GetDatabaseRequest request = new GetDatabaseRequest().withName(database.getName().toLowerCase());

		when(mockDatabaseResult.getDatabase()).thenReturn(database);
		when(mockGlueClient.getDatabase(request)).thenReturn(mockDatabaseResult);

		// Call under test
		Database result = athenaSupport.getDatabase(databaseName);

		assertEquals(database, result);
		verify(mockGlueClient).getDatabase(eq(request));
	}

	@Test
	public void testGetTableNameInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getTableName(null);
		});
	}

	@Test
	public void testGetTableName() {
		String tableName = "someTable";

		// Call under test
		String result = athenaSupport.getTableName(tableName);

		assertEquals(prefixWithInstance(tableName), result);
	}

	@Test
	public void testExecuteQuery() throws ServiceUnavailableException {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT count(*) FROM " + tableName;
		String countResult = "1000";
		String queryId = "abcd";

		Database database = new Database().withName(databaseName);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		ResultSet resultSet = new ResultSet().withRows(new Row().withData(new Datum().withVarCharValue("Count")),
				new Row().withData(new Datum().withVarCharValue(countResult)));

		when(mockQueryResult.getResultSet()).thenReturn(resultSet);
		when(mockQueryResult.getNextToken()).thenReturn(null);

		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return row.getData().get(0).getVarCharValue();
		}, 1, true);

		assertNotNull(result);
		assertEquals(queryId, result.getQueryExecutionId());
		assertEquals(expectedStats, result.getQueryExecutionStatistics());

		verify(mockQueryResult, never()).getResultSet();

		assertTrue(result.iterator().hasNext());

		assertEquals(countResult, result.iterator().next());

		verify(mockQueryResult).getResultSet();
		assertFalse(result.iterator().hasNext());

	}

	@Test
	public void testExecuteQueryMultiplePages() throws ServiceUnavailableException {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT * FROM " + tableName;
		String queryId = "abcd";
		int batchSize = 10;
		int secondPageResults = 5;

		Database database = new Database().withName(databaseName);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		// The first page includes the header
		ResultSet firstPage = new ResultSet().withRows(new Row().withData(new Datum().withVarCharValue("Column")));
		ResultSet secondPage = new ResultSet();

		for (int i = 0; i < batchSize; i++) {
			firstPage.withRows(new Row().withData(new Datum().withVarCharValue(String.valueOf(i))));
		}

		for (int i = batchSize; i < batchSize + secondPageResults; i++) {
			secondPage.withRows(new Row().withData(new Datum().withVarCharValue(String.valueOf(i))));
		}

		when(mockQueryResult.getResultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResult.getNextToken()).thenReturn("secondPageToken", new String[] { null });

		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, batchSize, true);

		assertNotNull(result);
		assertEquals(queryId, result.getQueryExecutionId());
		assertEquals(expectedStats, result.getQueryExecutionStatistics());

		verify(mockQueryResult, never()).getResultSet();

		assertTrue(result.iterator().hasNext());
		// First page should be loaded
		verify(mockQueryResult).getResultSet();

		int i = 0;

		while (result.iterator().hasNext()) {
			int value = result.iterator().next();
			assertEquals(i, value);
			i++;
		}
		// Should load only another page (e.g. only invoked another time)
		verify(mockQueryResult, times(2)).getResultSet();

	}

	private GetQueryExecutionRequest getQueryExecutionRequest(String queryId) {
		return new GetQueryExecutionRequest().withQueryExecutionId(queryId);
	}

	private StartQueryExecutionRequest getStartQueryExecutionRequest(String databaseName, String tableName) {
		QueryExecutionContext queryContext = new QueryExecutionContext().withDatabase(databaseName.toLowerCase());

		ResultConfiguration resultConfiguration = new ResultConfiguration().withOutputLocation(TEST_OUTPUT_RESULTS_LOCATION);

		StartQueryExecutionRequest request = new StartQueryExecutionRequest().withQueryExecutionContext(queryContext)
				.withResultConfiguration(resultConfiguration).withQueryString("MSCK REPAIR TABLE " + tableName);

		return request;
	}

	private String prefixWithInstance(String value) {
		return (TEST_STACK + TEST_INSTANCE + value).toLowerCase();
	}

}
