package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TokenPaginationIterator;

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
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class AthenaSupportImplTest {

	private static final String TEST_STACK = "test";
	private static final String TEST_INSTANCE = "123";
	private static final String TEST_DB = "db";
	private static final String TEST_TABLE = "table";
	private static final String TEST_COLUMN = "column";
	private static final String TEST_LOG_BUCKET_NAME = "test.log.bucket";
	private static final String TEST_OUTPUT_RESULTS_LOCATION = "s3://test.log.bucket/athena/000000123";

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
	
	private RowMapper<String> rowMapper = (Row row) -> {
		return row.getData().get(0).getVarCharValue();
	};

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

		assertEquals("s3://logbucket/athena/000000123", athenaSupport.getOutputResultLocation());
	}

	@Test
	public void testGetDatabases() {
		Database database = new Database().withName(TEST_DB);
		List<Database> mockDatabases = Collections.singletonList(database);

		when(mockDatabasesResults.getDatabaseList()).thenReturn(mockDatabases);
		when(mockGlueClient.getDatabases(any())).thenReturn(mockDatabasesResults);

		Iterator<Database> databases = athenaSupport.getDatabases();

		assertTrue(databases.hasNext());
		assertEquals(database, databases.next());

		verify(mockGlueClient).getDatabases(any());
	}

	@Test
	public void testGetPartitionedTablesEmpty() {
		Database database = new Database().withName(TEST_DB);

		List<Table> mockTables = Collections.singletonList(new Table().withName(TEST_TABLE).withDatabaseName(TEST_DB));

		when(mockTablesResults.getTableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables(any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables(any());
	}

	@Test
	public void testGetPartitionedTables() {
		Database database = new Database().withName(TEST_DB);

		Table table = new Table()
				.withName(TEST_TABLE)
				.withDatabaseName(TEST_DB)
				.withPartitionKeys(new Column().withName(TEST_COLUMN));

		List<Table> mockTables = Collections.singletonList(table);

		when(mockTablesResults.getTableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables(any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertTrue(tables.hasNext());
		assertEquals(table, tables.next());
		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables(any());
	}
	
	@Test
	public void testGetPartitionedTablesSkipUnpartitionedTables() {
		Database database = new Database().withName(TEST_DB);
		
		Table table1 = new Table()
				.withName(TEST_TABLE)
				.withDatabaseName(TEST_DB)
				.withPartitionKeys(new Column().withName(TEST_COLUMN));

		Table table2 = new Table().withName(TEST_TABLE)
				.withDatabaseName(TEST_DB)
				.withPartitionKeys(Collections.emptyList());
		
		Table table3 = new Table().withName(TEST_TABLE)
				.withDatabaseName(TEST_DB);

		List<Table> mockTables = ImmutableList.of(table1, table2, table3);

		when(mockTablesResults.getTableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables(any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertTrue(tables.hasNext());
		assertEquals(table1, tables.next());
		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables(any());
	}

	@Test
	public void testGetTable() {

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
	public void testRepairTable() {

		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);

		StartQueryExecutionRequest expectedRequest = getStartQueryExecutionRequest(databaseName, "MSCK REPAIR TABLE " + tableName);

		String queryId = "abcd";

		GetQueryExecutionRequest queryExecutionRequest = getQueryExecutionRequest(queryId);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		when(mockAthenaClient.startQueryExecution(eq(expectedRequest))).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(eq(queryExecutionRequest))).thenReturn(mockQueryExecutionResult);

		Table table = new Table().withDatabaseName(databaseName).withName(tableName);

		// Call under test
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(new AthenaQueryStatisticsAdapter(expectedStats), queryStats);
		verify(mockAthenaClient).startQueryExecution(eq(expectedRequest));
		verify(mockAthenaClient).getQueryExecution(eq(queryExecutionRequest));
	}

	@Test
	public void testGetTableNotFound() {

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
	public void testGetDatabase() {
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
	public void testGetDatabaseNameInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getDatabaseName(null);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getDatabaseName("");
		});
	}

	@Test
	public void testGetDatabaseName() {
		String databaseName = "someDatabase";

		// Call under test
		String result = athenaSupport.getDatabaseName(databaseName);

		assertEquals(prefixWithInstance(databaseName), result);
	}

	@Test
	public void testGetTableNameInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getTableName(null);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			athenaSupport.getTableName("");
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
	public void testSubmitQuery() {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT count(*) FROM " + tableName;
		String queryId = "abcd";

		Database database = new Database().withName(databaseName);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);
		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);

		// Call under test
		String queryExecutionId = athenaSupport.submitQuery(database, query);

		assertEquals(queryId, queryExecutionId);

		StartQueryExecutionRequest expectedRequest = getStartQueryExecutionRequest(databaseName, query);

		verify(mockAthenaClient).startQueryExecution(expectedRequest);
	}

	@Test
	public void testGetQueryExecutionStatus() {
		String queryId = "abcd";

		QueryExecution queryExecution = getQueryExecution(queryId);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(queryExecution);
		when(mockAthenaClient.getQueryExecution(getQueryExecutionRequest(queryId))).thenReturn(mockQueryExecutionResult);

		AthenaQueryExecution result = athenaSupport.getQueryExecutionStatus(queryId);

		assertEquals(new AthenaQueryExecutionAdapter(queryExecution), result);

		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockQueryExecutionResult).getQueryExecution();
	}

	@Test
	public void testGetQueryResults() {
		String queryId = "abcd";
		boolean excludeHeader = true;
		String value = "Some Value";

		when(mockQueryResult.getResultSet()).thenReturn(getResultSet("Header", value));
		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.getQueryResults(queryId, rowMapper, excludeHeader);

		AthenaQueryResult<String> expected = getQueryResult(false, queryId);
		
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));

		// The results are actually fetched from the iterator
		verify(mockAthenaClient, never()).getQueryResults(any());

		assertTrue(result.getQueryResultsIterator().hasNext());

		// Now the fetch is fired
		verify(mockAthenaClient).getQueryResults(any());

	}
	
	@Test
	public void testGetQueryResultsWhenStateNotSucceeded() {
		String queryId = "abcd";
		boolean excludeHeader = true;
		
		GetQueryExecutionRequest queryExecutionRequest = getQueryExecutionRequest(queryId);
		QueryExecution queryExecution = getQueryExecution(queryId);
		queryExecution.getStatus().withState(QueryExecutionState.FAILED);

		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(queryExecution);
		when(mockAthenaClient.getQueryExecution(queryExecutionRequest)).thenReturn(mockQueryExecutionResult);
		
		Assertions.assertThrows(IllegalStateException.class, ()->{
			// Call under test
			athenaSupport.getQueryResults(queryId, rowMapper, excludeHeader);
		});
		
		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults(any());
	}

	@Test
	public void testExecuteQueryWithIncludeHeader() {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT count(*) FROM " + tableName;
		String countResult = "1000";
		String queryId = "abcd";
		boolean excludeHeader = false;

		Database database = new Database().withName(databaseName);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);
		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockQueryResult.getResultSet()).thenReturn(getResultSet("Header", countResult));
		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.executeQuery(database, query, rowMapper, excludeHeader);
		
		AthenaQueryResult<String> expected = getQueryResult(excludeHeader, queryId);

		assertEquals(expected.includeHeader(), result.includeHeader());
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).startQueryExecution(getStartQueryExecutionRequest(databaseName, query));
		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults(any());

		assertTrue(result.getQueryResultsIterator().hasNext());
		
		verify(mockAthenaClient).getQueryResults(any());
		
		assertEquals("Header", result.getQueryResultsIterator().next());
		assertEquals(countResult, result.getQueryResultsIterator().next());
		
		assertFalse(result.getQueryResultsIterator().hasNext());

	}
	
	@Test
	public void testExecuteQueryWithExcludeHeader() {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT count(*) FROM " + tableName;
		String countResult = "1000";
		String queryId = "abcd";
		boolean excludeHeader = true;

		Database database = new Database().withName(databaseName);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);
		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockQueryResult.getResultSet()).thenReturn(getResultSet("Header", countResult));
		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.executeQuery(database, query, rowMapper, excludeHeader);
		
		AthenaQueryResult<String> expected = getQueryResult(excludeHeader, queryId);

		assertEquals(expected.includeHeader(), result.includeHeader());
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).startQueryExecution(getStartQueryExecutionRequest(databaseName, query));
		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults(any());

		assertTrue(result.getQueryResultsIterator().hasNext());
		
		verify(mockAthenaClient).getQueryResults(any());
		
		assertEquals(countResult, result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());

	}

	private ResultSet getResultSet(String... values) {
		ResultSet resultSet = new ResultSet();

		for (String value : values) {
			resultSet.withRows(getRow(value));
		}

		return resultSet;
	}

	private Row getRow(String value) {
		return new Row().withData(new Datum().withVarCharValue(value));
	}

	private AthenaQueryResult<String> getQueryResult(boolean excludeHeader, String queryId) {
		return new AthenaQueryResult<String>() {

			@Override
			public boolean includeHeader() {
				return !excludeHeader;
			}

			@Override
			public Iterator<String> getQueryResultsIterator() {
				return new TokenPaginationIterator<>(new AthenaResultsProvider<>(mockAthenaClient, queryId, rowMapper, excludeHeader));
			}

			@Override
			public AthenaQueryStatistics getQueryExecutionStatistics() {
				return new AthenaQueryStatisticsAdapter(getQueryExecution(queryId).getStatistics());
			}

			@Override
			public String getQueryExecutionId() {
				return queryId;
			}
		};
	}

	private QueryExecution getQueryExecution(String queryId) {
		QueryExecutionStatistics queryStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		QueryExecution queryExecution = new QueryExecution().withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED))
				.withStatistics(queryStats);

		return queryExecution;
	}

	private GetQueryExecutionRequest getQueryExecutionRequest(String queryId) {
		return new GetQueryExecutionRequest().withQueryExecutionId(queryId);
	}

	private StartQueryExecutionRequest getStartQueryExecutionRequest(String databaseName, String query) {
		QueryExecutionContext queryContext = new QueryExecutionContext().withDatabase(databaseName.toLowerCase());

		ResultConfiguration resultConfiguration = new ResultConfiguration().withOutputLocation(TEST_OUTPUT_RESULTS_LOCATION);

		StartQueryExecutionRequest request = new StartQueryExecutionRequest().withQueryExecutionContext(queryContext)
				.withResultConfiguration(resultConfiguration).withQueryString(query);

		return request;
	}

	private String prefixWithInstance(String value) {
		return (TEST_STACK + TEST_INSTANCE + value).toLowerCase();
	}

}
