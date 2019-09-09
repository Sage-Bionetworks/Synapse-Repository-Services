package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
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
	public void testGetParitionedTables() throws ServiceUnavailableException {
		// Call under test
		List<Table> tables = athenaSupport.getPartitionedTables();
		assertFalse(tables.isEmpty());
	}

	@Test
	public void testGetDatabase() throws ServiceUnavailableException {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		assertNotNull(database);
	}

	@Test
	public void testGetTable() throws ServiceUnavailableException {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);
		// Call under test
		Table table = athenaSupport.getTable(database, TABLE_NAME);
		assertNotNull(table);
	}

	@Test
	public void testGetTableNotFound() throws ServiceUnavailableException {
		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(database, TABLE_NAME + System.currentTimeMillis());
		});
	}

	@Test
	public void testRepairTable() throws ServiceUnavailableException {

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
		QueryExecutionStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(expectedStats, queryStats);
		verify(mockAthenaClient).startQueryExecution(any());
		verify(mockAthenaClient).getQueryExecution(any());
	}

	@Test
	public void testExecuteQuery() throws ServiceUnavailableException {
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

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, 1, true);

		assertNotNull(result);
		assertEquals(queryId, result.getQueryExecutionId());
		assertEquals(expectedStats, result.getQueryExecutionStatistics());
		assertTrue(result.iterator().hasNext());
		assertEquals(Integer.valueOf(countQueryResult), result.iterator().next());
		assertFalse(result.iterator().hasNext());
	}

	// This can be useful to actually test queries
	@Test
	@Disabled("We do not want to run athena queries each time we run the tests")
	public void testExecuteQueryIntegration() throws ServiceUnavailableException {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		String query = "SELECT count(*) FROM " + athenaSupport.getTableName(TABLE_NAME);

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(database, query, (Row row) -> {
			return Integer.valueOf(row.getData().get(0).getVarCharValue());
		}, 1, true);

		assertNotNull(result);
		assertTrue(result.iterator().hasNext());
		result.iterator().next();
		assertFalse(result.iterator().hasNext());
	}

	// This can be useful to actually test that it works
	@Test
	@Disabled("We do not want to run athena repair each time we run the tests")
	public void testRepairIntegration() throws ServiceUnavailableException {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);

		Database database = athenaSupport.getDatabase(DATABASE_NAME);

		Table table = athenaSupport.getTable(database, TABLE_NAME);

		// Call under test
		QueryExecutionStatistics queryStats = athenaSupport.repairTable(table);

		assertNotNull(queryStats);
		assertNotNull(queryStats.getDataScannedInBytes());
		assertNotNull(queryStats.getEngineExecutionTimeInMillis());

	}

}
