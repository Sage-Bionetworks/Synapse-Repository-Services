package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TokenPaginationIterator;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatistics;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.Column;
import software.amazon.awssdk.services.glue.model.Database;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.GetDatabaseRequest;
import software.amazon.awssdk.services.glue.model.GetDatabaseResponse;
import software.amazon.awssdk.services.glue.model.GetDatabasesRequest;
import software.amazon.awssdk.services.glue.model.GetDatabasesResponse;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.GetTableResponse;
import software.amazon.awssdk.services.glue.model.GetTablesRequest;
import software.amazon.awssdk.services.glue.model.GetTablesResponse;
import software.amazon.awssdk.services.glue.model.Table;
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
	private GlueClient mockGlueClient;

	@Mock
	private AthenaClient mockAthenaClient;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private GetDatabasesResponse mockDatabasesResults;

	@Mock
	private GetTablesResponse mockTablesResults;

	@Mock
	private GetTableResponse mockTableResult;

	@Mock
	private GetDatabaseResponse mockDatabaseResult;

	@Mock
	private StartQueryExecutionResponse mockStartQueryResult;

	@Mock
	private GetQueryExecutionResponse mockQueryExecutionResult;

	@Mock
	private GetQueryResultsResponse mockQueryResult;

	private AthenaSupportImpl athenaSupport;

	private RowMapper<String> rowMapper = (Row row) -> {
		return row.data().get(0).varCharValue();
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
		Database database = Database.builder().name(TEST_DB).build();
		List<Database> mockDatabases = Collections.singletonList(database);

		when(mockDatabasesResults.databaseList()).thenReturn(mockDatabases);
		when(mockGlueClient.getDatabases((GetDatabasesRequest) any())).thenReturn(mockDatabasesResults);

		Iterator<Database> databases = athenaSupport.getDatabases();

		assertTrue(databases.hasNext());
		assertEquals(database, databases.next());

		verify(mockGlueClient).getDatabases((GetDatabasesRequest) any());
	}

	@Test
	public void testGetPartitionedTablesEmpty() {
		Database database = Database.builder().name(TEST_DB).build();

		List<Table> mockTables = Collections.singletonList(Table.builder().name(TEST_TABLE).databaseName(TEST_DB).build());

		when(mockTablesResults.tableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables((GetTablesRequest) any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables((GetTablesRequest) any());
	}

	@Test
	public void testGetPartitionedTables() {
		Database database = Database.builder().name(TEST_DB).build();

		Table table = Table.builder()
				.name(TEST_TABLE)
				.databaseName(TEST_DB)
				.partitionKeys(Column.builder().name(TEST_COLUMN).build())
				.build();

		List<Table> mockTables = Collections.singletonList(table);

		when(mockTablesResults.tableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables((GetTablesRequest) any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertTrue(tables.hasNext());
		assertEquals(table, tables.next());
		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables((GetTablesRequest) any());
	}
	
	@Test
	public void testGetPartitionedTablesSkipUnpartitionedTables() {
		Database database = Database.builder().name(TEST_DB).build();
		
		Table table1 = Table.builder()
				.name(TEST_TABLE)
				.databaseName(TEST_DB)
				.partitionKeys(Column.builder().name(TEST_COLUMN).build())
				.build();

		Table table2 = Table.builder().name(TEST_TABLE)
				.databaseName(TEST_DB)
				.partitionKeys(Collections.emptyList())
				.build();

		Table table3 = Table.builder().name(TEST_TABLE)
				.databaseName(TEST_DB)
				.build();

		List<Table> mockTables = ImmutableList.of(table1, table2, table3);

		when(mockTablesResults.tableList()).thenReturn(mockTables);
		when(mockGlueClient.getTables((GetTablesRequest) any())).thenReturn(mockTablesResults);

		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);

		assertTrue(tables.hasNext());
		assertEquals(table1, tables.next());
		assertFalse(tables.hasNext());
		verify(mockGlueClient).getTables((GetTablesRequest) any());
	}

	@Test
	public void testGetTable() {

		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);

		Database database = Database.builder().name(databaseName).build();

		GetTableRequest request = GetTableRequest.builder().databaseName(databaseName).name(tableName).build();

		when(mockTableResult.table()).thenReturn(Table.builder().databaseName(databaseName).name(tableName).build());

		when(mockGlueClient.getTable(eq(request))).thenReturn(mockTableResult);

		// Call under test
		Table table = athenaSupport.getTable(database, TEST_TABLE);

		assertNotNull(table);
		assertEquals(tableName, table.name());
		assertEquals(databaseName, table.databaseName());
		verify(mockGlueClient).getTable(eq(request));
	}

	@Test
	public void testRepairTable() {

		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);

		StartQueryExecutionRequest expectedRequest = getStartQueryExecutionRequest(databaseName, "MSCK REPAIR TABLE " + tableName);

		String queryId = "abcd";

		GetQueryExecutionRequest queryExecutionRequest = getQueryExecutionRequest(queryId);

		when(mockStartQueryResult.queryExecutionId()).thenReturn(queryId);

		QueryExecutionStatistics expectedStats = QueryExecutionStatistics.builder().dataScannedInBytes(1000L)
				.engineExecutionTimeInMillis(1000L).build();

		when(mockQueryExecutionResult.queryExecution()).thenReturn(QueryExecution.builder()
				.status(QueryExecutionStatus.builder().state(QueryExecutionState.SUCCEEDED).build()).statistics(expectedStats).build());

		when(mockAthenaClient.startQueryExecution(eq(expectedRequest))).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(eq(queryExecutionRequest))).thenReturn(mockQueryExecutionResult);

		Table table = Table.builder().databaseName(databaseName).name(tableName).build();

		// Call under test
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(new AthenaQueryStatisticsAdapter(expectedStats), queryStats);
		verify(mockAthenaClient).startQueryExecution(eq(expectedRequest));
		verify(mockAthenaClient).getQueryExecution(eq(queryExecutionRequest));
	}

	@Test
	public void testGetTableNotFound() {

		Database database = Database.builder().name(prefixWithInstance(TEST_DB)).build();

		when(mockGlueClient.getTable((GetTableRequest) any())).thenThrow(EntityNotFoundException.class);

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

		Database database = Database.builder().name(prefixWithInstance(databaseName)).build();

		GetDatabaseRequest request = GetDatabaseRequest.builder().name(database.name().toLowerCase()).build();

		when(mockDatabaseResult.database()).thenReturn(database);
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

		Database database = Database.builder().name(databaseName).build();

		when(mockStartQueryResult.queryExecutionId()).thenReturn(queryId);
		when(mockAthenaClient.startQueryExecution((StartQueryExecutionRequest) any())).thenReturn(mockStartQueryResult);

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

		when(mockQueryExecutionResult.queryExecution()).thenReturn(queryExecution);
		when(mockAthenaClient.getQueryExecution(getQueryExecutionRequest(queryId))).thenReturn(mockQueryExecutionResult);

		AthenaQueryExecution result = athenaSupport.getQueryExecutionStatus(queryId);

		assertEquals(new AthenaQueryExecutionAdapter(queryExecution), result);

		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockQueryExecutionResult).queryExecution();
	}

	@Test
	public void testGetQueryResults() {
		String queryId = "abcd";
		boolean excludeHeader = true;
		String value = "Some Value";

		when(mockQueryResult.resultSet()).thenReturn(getResultSet("Header", value));
		when(mockQueryExecutionResult.queryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockAthenaClient.getQueryExecution((GetQueryExecutionRequest) any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.getQueryResults(queryId, rowMapper, excludeHeader);

		AthenaQueryResult<String> expected = getQueryResult(false, queryId);
		
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));

		// The results are actually fetched from the iterator
		verify(mockAthenaClient, never()).getQueryResults((GetQueryResultsRequest) any());

		assertTrue(result.getQueryResultsIterator().hasNext());

		// Now the fetch is fired
		verify(mockAthenaClient).getQueryResults((GetQueryResultsRequest) any());

	}
	
	@Test
	public void testGetQueryResultsWhenStateNotSucceeded() {
		String queryId = "abcd";
		boolean excludeHeader = true;
		
		GetQueryExecutionRequest queryExecutionRequest = getQueryExecutionRequest(queryId);
		QueryExecution queryExecution = getQueryExecution(queryId);
		// Update state - v2 models are immutable so we need to rebuild
		queryExecution = queryExecution.toBuilder().status(queryExecution.status().toBuilder().state(QueryExecutionState.FAILED).build()).build();

		when(mockQueryExecutionResult.queryExecution()).thenReturn(queryExecution);
		when(mockAthenaClient.getQueryExecution((GetQueryExecutionRequest) any())).thenReturn(mockQueryExecutionResult);

		Assertions.assertThrows(IllegalStateException.class, ()->{
			// Call under test
			athenaSupport.getQueryResults(queryId, rowMapper, excludeHeader);
		});

		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults((GetQueryResultsRequest) any());
	}
	
	@Test
	public void testGetQueryResultsPageWithNoToken() {
		String queryId = "abcd";
		String value = "Some Value";
		int limit = 100;
		String pageToken = null;

		when(mockQueryResult.nextToken()).thenReturn("next");
		when(mockQueryResult.resultSet()).thenReturn(getResultSet("Header", value, value));
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResult);
		
		AthenaQueryResultPage<String> expected = new AthenaQueryResultPage<String>()
				.withQueryExecutionId(queryId)
				.withNextPageToken("next")
				// Note that since the pageToken is null the header is skipped
				.withResults(Arrays.asList(value, value));
		
		GetQueryResultsRequest expectedRequest = GetQueryResultsRequest.builder()
				.queryExecutionId(queryId)
				.maxResults(limit)
				.nextToken(pageToken)
				.build();

		// Call under test
		AthenaQueryResultPage<String> result = athenaSupport.getQueryResultsPage(queryId, rowMapper, pageToken, limit);
		
		assertEquals(expected, result);

		// Now the fetch is fired
		verify(mockAthenaClient).getQueryResults(expectedRequest);

	}
	
	@Test
	public void testGetQueryResultsPageWithPageToken() {
		String queryId = "abcd";
		String value = "Some Value";
		int limit = 100;
		String pageToken = "someToken";

		when(mockQueryResult.nextToken()).thenReturn("next");
		when(mockQueryResult.resultSet()).thenReturn(getResultSet(value, value));
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResult);

		AthenaQueryResultPage<String> expected = new AthenaQueryResultPage<String>()
				.withQueryExecutionId(queryId)
				.withNextPageToken("next")
				.withResults(Arrays.asList(value, value));
		
		GetQueryResultsRequest expectedRequest = GetQueryResultsRequest.builder()
				.queryExecutionId(queryId)
				.maxResults(limit)
				.nextToken(pageToken)
				.build();

		// Call under test
		AthenaQueryResultPage<String> result = athenaSupport.getQueryResultsPage(queryId, rowMapper, pageToken, limit);
		
		assertEquals(expected, result);

		// Now the fetch is fired
		verify(mockAthenaClient).getQueryResults(expectedRequest);

	}
	
	@Test
	public void testGetQueryResultsPageWithNoQueryId() {
		String queryId = null;
		int limit = 100;
		String pageToken = "someToken";

		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			athenaSupport.getQueryResultsPage(queryId, rowMapper, pageToken, limit);
		}).getMessage();
		
		assertEquals("executionQueryId is required.", message);

		// Now the fetch is fired
		verifyNoMoreInteractions(mockAthenaClient);

	}
	
	@Test
	public void testGetQueryResultsPageWithNoRowMapper() {
		String queryId = "abc";
		int limit = 100;
		String pageToken = "someToken";

		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			athenaSupport.getQueryResultsPage(queryId, null, pageToken, limit);
		}).getMessage();
		
		assertEquals("rowMapper is required.", message);

		// Now the fetch is fired
		verifyNoMoreInteractions(mockAthenaClient);

	}
	
	@Test
	public void testGetQueryResultsPageWithNegativeLimit() {
		String queryId = "abcd";
		int limit = -1;
		String pageToken = "someToken";

		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			athenaSupport.getQueryResultsPage(queryId, rowMapper, pageToken, limit);
		}).getMessage();
		
		assertEquals("The limit must be greater than 0 and less or equal than 1000", message);

		// Now the fetch is fired
		verifyNoMoreInteractions(mockAthenaClient);

	}
	
	@Test
	public void testGetQueryResultsPageWithExceedLimit() {
		String queryId = "abcd";
		int limit = AthenaResultsProvider.MAX_PAGE_SIZE + 1;
		String pageToken = "someToken";

		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			athenaSupport.getQueryResultsPage(queryId, rowMapper, pageToken, limit);
		}).getMessage();
		
		assertEquals("The limit must be greater than 0 and less or equal than 1000", message);

		// Now the fetch is fired
		verifyNoMoreInteractions(mockAthenaClient);

	}

	@Test
	public void testExecuteQueryWithIncludeHeader() {
		String databaseName = prefixWithInstance(TEST_DB);
		String tableName = prefixWithInstance(TEST_TABLE);
		String query = "SELECT count(*) FROM " + tableName;
		String countResult = "1000";
		String queryId = "abcd";
		boolean excludeHeader = false;

		Database database = Database.builder().name(databaseName).build();

		when(mockStartQueryResult.queryExecutionId()).thenReturn(queryId);
		when(mockQueryExecutionResult.queryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockQueryResult.resultSet()).thenReturn(getResultSet("Header", countResult));
		when(mockAthenaClient.startQueryExecution((StartQueryExecutionRequest) any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution((GetQueryExecutionRequest) any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.executeQuery(database, query, rowMapper, excludeHeader);
		
		AthenaQueryResult<String> expected = getQueryResult(excludeHeader, queryId);

		assertEquals(expected.includeHeader(), result.includeHeader());
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).startQueryExecution(getStartQueryExecutionRequest(databaseName, query));
		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults((GetQueryResultsRequest) any());

		assertTrue(result.getQueryResultsIterator().hasNext());

		verify(mockAthenaClient).getQueryResults((GetQueryResultsRequest) any());

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

		Database database = Database.builder().name(databaseName).build();

		when(mockStartQueryResult.queryExecutionId()).thenReturn(queryId);
		when(mockQueryExecutionResult.queryExecution()).thenReturn(getQueryExecution(queryId));
		when(mockQueryResult.resultSet()).thenReturn(getResultSet("Header", countResult));
		when(mockAthenaClient.startQueryExecution((StartQueryExecutionRequest) any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution((GetQueryExecutionRequest) any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<String> result = athenaSupport.executeQuery(database, query, rowMapper, excludeHeader);
		
		AthenaQueryResult<String> expected = getQueryResult(excludeHeader, queryId);

		assertEquals(expected.includeHeader(), result.includeHeader());
		assertEquals(expected.getQueryExecutionId(), result.getQueryExecutionId());
		assertEquals(expected.getQueryExecutionStatistics(), result.getQueryExecutionStatistics());

		verify(mockAthenaClient).startQueryExecution(getStartQueryExecutionRequest(databaseName, query));
		verify(mockAthenaClient).getQueryExecution(getQueryExecutionRequest(queryId));
		verify(mockAthenaClient, never()).getQueryResults((GetQueryResultsRequest) any());

		assertTrue(result.getQueryResultsIterator().hasNext());

		verify(mockAthenaClient).getQueryResults((GetQueryResultsRequest) any());

		assertEquals(countResult, result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());

	}

	private ResultSet getResultSet(String... values) {
		ResultSet.Builder resultSetBuilder = ResultSet.builder();
		List<Row> rows = Arrays.stream(values)
				.map(this::getRow)
				.toList();
		resultSetBuilder.rows(rows);
		return resultSetBuilder.build();
	}

	private Row getRow(String value) {
		return Row.builder().data(Datum.builder().varCharValue(value).build()).build();
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
				return new AthenaQueryStatisticsAdapter(getQueryExecution(queryId).statistics());
			}

			@Override
			public String getQueryExecutionId() {
				return queryId;
			}
		};
	}

	private QueryExecution getQueryExecution(String queryId) {
		QueryExecutionStatistics queryStats = QueryExecutionStatistics.builder().dataScannedInBytes(1000L)
				.engineExecutionTimeInMillis(1000L).build();

		QueryExecution queryExecution = QueryExecution.builder().status(QueryExecutionStatus.builder().state(QueryExecutionState.SUCCEEDED).build())
				.statistics(queryStats).build();

		return queryExecution;
	}

	private GetQueryExecutionRequest getQueryExecutionRequest(String queryId) {
		return GetQueryExecutionRequest.builder().queryExecutionId(queryId).build();
	}

	private StartQueryExecutionRequest getStartQueryExecutionRequest(String databaseName, String query) {
		QueryExecutionContext queryContext = QueryExecutionContext.builder().database(databaseName.toLowerCase()).build();

		ResultConfiguration resultConfiguration = ResultConfiguration.builder().outputLocation(TEST_OUTPUT_RESULTS_LOCATION).build();

		StartQueryExecutionRequest request = StartQueryExecutionRequest.builder().queryExecutionContext(queryContext)
				.resultConfiguration(resultConfiguration).queryString(query).build();

		return request;
	}

	private String prefixWithInstance(String value) {
		return (TEST_STACK + TEST_INSTANCE + value).toLowerCase();
	}

}
