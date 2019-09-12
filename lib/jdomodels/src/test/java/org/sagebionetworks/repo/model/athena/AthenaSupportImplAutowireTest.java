package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatistics;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AthenaSupportImplAutowireTest {

	private static final String DATABASE_NAME = "firehoseLogs";
	private static final String TABLE_NAME = "fileDownloadsRecords";

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AWSGlue glueClient;

	@Autowired
	private AmazonAthena athenaClient;

	// We do not want to run the repair during tests
	@Mock
	private AmazonAthena mockAthenaClient;

	@Mock
	private StartQueryExecutionResult mockStartQueryResult;

	@Mock
	private GetQueryExecutionResult mockQueryExecutionResult;

	@Mock
	private GetQueryResultsResult mockQueryResult;

	private AthenaSupport athenaSupport;

	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
		athenaSupport = new AthenaSupportImpl(glueClient, mockAthenaClient, stackConfig);
	}
	
	@Test
	public void testGetDatabases() {
		// Call under test
		Iterator<Database> databases = athenaSupport.getDatabases();
		assertTrue(databases.hasNext());
	}

	@Test
	public void testGetParitionedTables() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		// Call under test
		Iterator<Table> tables = athenaSupport.getPartitionedTables(database);
		assertTrue(tables.hasNext());
	}

	@Test
	public void testGetDatabase() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		assertNotNull(database);
	}

	@Test
	public void testGetTable() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		// Call under test
		Table table = athenaSupport.getTable(database, TABLE_NAME);
		assertNotNull(table);
	}

	@Test
	public void testGetTableNotFound() {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(database, TABLE_NAME + System.currentTimeMillis());
		});
	}

	@Test
	public void testRepairTable() {

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn("abcd");
		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Table table = athenaSupport.getTable(database, TABLE_NAME);

		// Call under test
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(new AthenaQueryStatisticsAdapter(expectedStats), queryStats);
		verify(mockAthenaClient).startQueryExecution(any());
		verify(mockAthenaClient).getQueryExecution(any());
	}

	@Test
	public void testExecuteQuery() {
		String queryId = "abcd";
		String countQueryResult = "1000";

		QueryExecutionStatistics expectedStats = new QueryExecutionStatistics().withDataScannedInBytes(1000L)
				.withEngineExecutionTimeInMillis(1000L);

		when(mockStartQueryResult.getQueryExecutionId()).thenReturn(queryId);
		when(mockQueryExecutionResult.getQueryExecution()).thenReturn(new QueryExecution()
				.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED)).withStatistics(expectedStats));

		when(mockQueryResult.getResultSet()).thenReturn(new ResultSet().withRows(new Row().withData(new Datum().withVarCharValue("Count")),
				new Row().withData(new Datum().withVarCharValue(countQueryResult))));

		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		String query = "SELECT count(*) FROM " + athenaSupport.getTableName(TABLE_NAME);

		boolean excludeHeader = true;

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, excludeHeader);

		assertNotNull(result);
		assertEquals(queryId, result.getQueryExecutionId());
		assertEquals(new AthenaQueryStatisticsAdapter(expectedStats), result.getQueryExecutionStatistics());
		assertTrue(result.getQueryResultsIterator().hasNext());
		assertEquals(Integer.valueOf(countQueryResult), result.getQueryResultsIterator().next());
		assertFalse(result.getQueryResultsIterator().hasNext());
	}

	// This can be useful to actually test queries
	@Test
	@Disabled("We do not want to run athena queries each time we run the tests")
	public void testExecuteQueryIntegration() {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		String query = "SELECT count(*) FROM " + athenaSupport.getTableName(TABLE_NAME);

		boolean excludeHeader = true;

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, excludeHeader);

		assertNotNull(result);
		assertTrue(result.getQueryResultsIterator().hasNext());
		result.getQueryResultsIterator().next();
		assertFalse(result.getQueryResultsIterator().hasNext());
	}

	// This can be useful to actually test that it works
	@Test
	@Disabled("We do not want to run athena repair each time we run the tests")
	public void testRepairIntegration() {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Table table = athenaSupport.getTable(database, TABLE_NAME);

		// Call under test
		AthenaQueryStatistics queryStats = athenaSupport.repairTable(table);

		assertNotNull(queryStats);
		assertNotNull(queryStats.getDataScanned());
		assertNotNull(queryStats.getExecutionTime());

	}

}
