package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.AthenaSupportImpl;
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
	public void testGetParitionedTables() {
		// Call under test
		List<Table> tables = athenaSupport.getPartitionedTables();
		assertFalse(tables.isEmpty());
	}

	@Test
	public void testGetTable() {
		// Call under test
		Table table = athenaSupport.getTable(DATABASE_NAME, TABLE_NAME);
		assertNotNull(table);
	}

	@Test
	public void testGetTableNotFound() {
		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			athenaSupport.getTable(DATABASE_NAME, TABLE_NAME + System.currentTimeMillis());
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

		Table table = athenaSupport.getTable(DATABASE_NAME, TABLE_NAME);

		// Call under test
		QueryExecutionStatistics queryStats = athenaSupport.repairTable(table);

		assertEquals(expectedStats, queryStats);
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

		when(mockQueryResult.getResultSet())
				.thenReturn(new ResultSet().withRows(
						new Row().withData(new Datum().withVarCharValue("Count")),
						new Row().withData(new Datum().withVarCharValue(countQueryResult)))
				);

		when(mockAthenaClient.startQueryExecution(any())).thenReturn(mockStartQueryResult);
		when(mockAthenaClient.getQueryExecution(any())).thenReturn(mockQueryExecutionResult);
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResult);

		String databaseName = athenaSupport.getDatabaseName(DATABASE_NAME);
		String query = "SELECT count(*) FROM " + athenaSupport.getTableName(TABLE_NAME);

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(databaseName, query, (Row row) -> {
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
	public void testExecuteQueryIntegration() {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);
		
		String databaseName = athenaSupport.getDatabaseName(DATABASE_NAME);
		String query = "SELECT count(*) FROM " + athenaSupport.getTableName(TABLE_NAME);

		// Call under test
		AthenaQueryResult<Integer> result = athenaSupport.executeQuery(databaseName, query, (Row row) -> {
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
	public void testRepairIntegration() {
		athenaSupport = new AthenaSupportImpl(glueClient, athenaClient, stackConfig);

		Table table = athenaSupport.getTable(DATABASE_NAME, TABLE_NAME);

		// Call under test
		QueryExecutionStatistics queryStats = athenaSupport.repairTable(table);

		assertNotNull(queryStats);
		assertNotNull(queryStats.getDataScannedInBytes());
		assertNotNull(queryStats.getEngineExecutionTimeInMillis());

	}

}
