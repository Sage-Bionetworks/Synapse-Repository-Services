package org.sagebionetworks.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.ColumnInfo;
import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.ResultSetMetadata;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@ExtendWith(MockitoExtension.class)
public class WarehouseTestHelperImplTest {

	@Mock
	private AmazonS3 mockS3Client;
	@Mock
	private AmazonAthena mockAthenaClient;
	@Mock
	private Clock mockClock;
	@Mock
	private StackConfiguration mockStackConfig;

	@Spy
	@InjectMocks
	private WarehouseTestHelperImpl warehouseHelper;

	@Test
	public void testSaveQueryToS3() {
		String query = "select count(*) from foo";
		Instant now = Instant.ofEpochMilli(1001L);
		String path = "instance/foo/bar";
		int maxNumberHours = 2;

		// call under test
		warehouseHelper.saveQueryToS3(query, now, path, maxNumberHours);

		verify(mockS3Client).putObject(WarehouseTestHelperImpl.BUCKET_NAME, "instance/foo/bar/7201001.sql", query);
	}

	@Test
	public void testGetExpiresOnFromKey() {
		// call under test
		assertEquals(123L, WarehouseTestHelperImpl.getExpiresOnFromKey("istance/org/sage/44/123.sql"));
	}

	@Test
	public void testAssertWarehouseQuery() throws Exception {
		long nowMS = 1001L;
		Instant nowInstant = Instant.ofEpochMilli(nowMS);
		when(mockClock.currentTimeMillis()).thenReturn(nowMS);
		String keyOne = "one/999.sql";
		String keyTwo = "two/1001.sql";
		String keyThree = "three/1002.sql";
		when(mockS3Client.listObjectsV2(any(), any())).thenReturn(createListObjectV2Result(keyOne, keyTwo, keyThree));

		doReturn(Mockito.mock(Executable.class)).when(warehouseHelper).executeQueryAndAssertResults(any());
		String callersPath = "org/sage/bar/12";
		doReturn(callersPath).when(warehouseHelper).getCallersPath(any());
		doNothing().when(warehouseHelper).saveQueryToS3(any(), any(), any(), anyInt());

		String query = "select count(*) from foo";

		// call under test
		warehouseHelper.assertWarehouseQuery(query);

		verify(mockClock).currentTimeMillis();
		verify(mockS3Client).listObjectsV2(WarehouseTestHelperImpl.BUCKET_NAME, callersPath);

		verify(warehouseHelper).saveQueryToS3(query, nowInstant, callersPath, WarehouseTestHelperImpl.WAREHOUSE_QUERY_EXPIRATION_HOURS);
		verify(warehouseHelper).executeQueryAndAssertResults(keyOne);
		verify(warehouseHelper).executeQueryAndAssertResults(keyTwo);
		verify(warehouseHelper, never()).executeQueryAndAssertResults(keyThree);
		verify(warehouseHelper, times(2)).executeQueryAndAssertResults(any());

		verify(mockS3Client).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyOne);
		verify(mockS3Client).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyTwo);
		verify(mockS3Client, never()).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyThree);
		verify(mockS3Client, times(2)).deleteObject(any(String.class), any(String.class));

	}

	@Test
	public void testAssertWarehouseQueryWithDeleteFilesOnException() throws Exception {
		long nowMS = 1001L;
		when(mockClock.currentTimeMillis()).thenReturn(nowMS);
		String keyOne = "one/999.sql";
		String keyTwo = "two/1001.sql";
		String keyThree = "three/1002.sql";
		when(mockS3Client.listObjectsV2(any(), any())).thenReturn(createListObjectV2Result(keyOne, keyTwo, keyThree));
		
		Exception exception = new IllegalArgumentException("wrong");
		doThrow(exception).when(warehouseHelper).executeQueryAndAssertResults(any());
		String callersPath = "org/sage/bar/12";
		doReturn(callersPath).when(warehouseHelper).getCallersPath(any());
		doNothing().when(warehouseHelper).saveQueryToS3(any(), any(), any(), anyInt());

		String query = "select count(*) from foo";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			warehouseHelper.assertWarehouseQuery(query);
		});

		verify(mockClock).currentTimeMillis();
		verify(mockS3Client).listObjectsV2(WarehouseTestHelperImpl.BUCKET_NAME, callersPath);

		verify(warehouseHelper).executeQueryAndAssertResults(any());
		// both expired keys should still be deleted.
		verify(mockS3Client).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyOne);
		verify(mockS3Client).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyTwo);
		verify(mockS3Client, never()).deleteObject(WarehouseTestHelperImpl.BUCKET_NAME, keyThree);
		verify(mockS3Client, times(2)).deleteObject(any(String.class), any(String.class));
	}

	@Test
	public void testGetCallersPath() {
		String declaringClass = "org.example.TheClass";
		String methodName = "getMethod";
		String fileName = "foo.bar";
		int lineNumber = 12;
		StackTraceElement element = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
		when(mockStackConfig.getStackInstance()).thenReturn("anInstance");

		// call under test
		String path = warehouseHelper.getCallersPath(element);
		assertEquals("anInstance/org/example/TheClass/getMethod/12", path);

		verify(mockStackConfig).getStackInstance();
	}

	@Test
	public void testExecuteQueryAndAssertResults() throws Throwable {
		String key = "foo/bar/1/123/sql";
		String query = "select count(*) from goo";

		when(mockS3Client.getObjectAsString(any(), any())).thenReturn(query);
		int count = 12;
		doReturn(count).when(warehouseHelper).executeCountQuery(any());

		// call under test
		Executable e = warehouseHelper.executeQueryAndAssertResults(key);
		assertNotNull(e);
		e.execute();

		verify(mockS3Client).getObjectAsString(WarehouseTestHelperImpl.BUCKET_NAME, key);
		verify(warehouseHelper).executeCountQuery(query);
	}

	@Test
	public void testExecuteCountQuery() throws Exception {
		String query = "select count(*) from foo";
		String executionId = "executionId";
		int count = 12;

		doNothing().when(warehouseHelper).waitForQueryToFinish(any(), anyInt(), anyInt());

		when(mockAthenaClient.startQueryExecution(any()))
				.thenReturn(new StartQueryExecutionResult().withQueryExecutionId(executionId));
		when(mockAthenaClient.getQueryResults(any()))
				.thenReturn(new GetQueryResultsResult().withResultSet(setupResultSet(count)));

		// call under test
		int result = warehouseHelper.executeCountQuery(query);
		assertEquals(count, result);

		verify(warehouseHelper).waitForQueryToFinish(executionId, WarehouseTestHelperImpl.MAX_WAIT_MS,
				WarehouseTestHelperImpl.WAIT_INTERAVAL_MS);
		verify(mockAthenaClient)
				.startQueryExecution(new StartQueryExecutionRequest().withQueryString(query).withQueryExecutionContext(
						new QueryExecutionContext().withCatalog("AwsDataCatalog").withDatabase("datawarehouse")));
		verify(mockAthenaClient).getQueryResults(new GetQueryResultsRequest().withQueryExecutionId(executionId));
	}

	ResultSet setupResultSet(int count) {
		Row header = new Row().withData(new Datum().withVarCharValue("_c_"));
		Row value = new Row().withData(new Datum().withVarCharValue("" + count));
		return new ResultSet().withResultSetMetadata(new ResultSetMetadata().withColumnInfo(new ColumnInfo()))
				.withRows(header, value);
	}

	@Test
	public void testToDateStringBetweenPlusAndMinusFiveSeconds() {
		Instant instant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2022-12-31T23:59:59.605Z"));
		assertEquals("between date('2022-12-31') and date('2023-01-01')",
				warehouseHelper.toDateStringBetweenPlusAndMinusFiveSeconds(instant));
	}

	@Test
	public void testToIsoTimestampStringBetweenPlusAndMinusFiveSeconds() {
		Instant instant = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2022-12-31T23:59:59.605Z"));
		assertEquals(
				"between from_iso8601_timestamp('2022-12-31T23:59:54.605Z') and from_iso8601_timestamp('2023-01-01T00:00:04.605Z')",
				warehouseHelper.toIsoTimestampStringBetweenPlusAndMinusFiveSeconds(instant));
	}

	@Test
	public void testWaitForQueryToFinishWithTimeoutWithRunning() throws Exception {
		String queryId = "123";
		int maxWaitMS = 300;
		int intervaleMS = 30;

		when(mockAthenaClient.getQueryExecution(any())).thenReturn(new GetQueryExecutionResult().withQueryExecution(
				new QueryExecution().withStatus(new QueryExecutionStatus().withState(QueryExecutionState.RUNNING))));

		String message = assertThrows(TimeoutException.class, () -> {
			// call under test
			warehouseHelper.waitForQueryToFinish(queryId, maxWaitMS, intervaleMS);
		}).getMessage();
		assertTrue(message.startsWith("Waited"));

		verify(mockAthenaClient, atLeast(5))
				.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryId));
	}

	@Test
	public void testWaitForQueryToFinishWithTimeoutWithQueued() throws Exception {
		String queryId = "123";
		int maxWaitMS = 300;
		int intervaleMS = 30;

		when(mockAthenaClient.getQueryExecution(any())).thenReturn(new GetQueryExecutionResult().withQueryExecution(
				new QueryExecution().withStatus(new QueryExecutionStatus().withState(QueryExecutionState.QUEUED))));

		String message = assertThrows(TimeoutException.class, () -> {
			// call under test
			warehouseHelper.waitForQueryToFinish(queryId, maxWaitMS, intervaleMS);
		}).getMessage();
		assertTrue(message.startsWith("Waited"));

		verify(mockAthenaClient, atLeast(5))
				.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryId));
	}

	@Test
	public void testWaitForQueryToFinishWithFailed() throws Exception {
		String queryId = "123";
		int maxWaitMS = 300;
		int intervaleMS = 30;

		when(mockAthenaClient.getQueryExecution(any())).thenReturn(new GetQueryExecutionResult()
				.withQueryExecution(new QueryExecution().withStatus(new QueryExecutionStatus()
						.withState(QueryExecutionState.FAILED).withStateChangeReason("wrong"))));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			warehouseHelper.waitForQueryToFinish(queryId, maxWaitMS, intervaleMS);
		}).getMessage();
		assertEquals("Athena query failed: wrong", message);

		verify(mockAthenaClient).getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryId));
	}

	@Test
	public void testWaitForQueryToFinishWithCanceled() throws Exception {
		String queryId = "123";
		int maxWaitMS = 300;
		int intervaleMS = 30;

		when(mockAthenaClient.getQueryExecution(any())).thenReturn(new GetQueryExecutionResult()
				.withQueryExecution(new QueryExecution().withStatus(new QueryExecutionStatus()
						.withState(QueryExecutionState.CANCELLED).withStateChangeReason("canned"))));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			warehouseHelper.waitForQueryToFinish(queryId, maxWaitMS, intervaleMS);
		}).getMessage();
		assertEquals("Athena query canceled: canned", message);

		verify(mockAthenaClient).getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryId));
	}

	@Test
	public void testWaitForQueryToFinishWithSucceeded() throws Exception {
		String queryId = "123";
		int maxWaitMS = 300;
		int intervaleMS = 30;

		when(mockAthenaClient.getQueryExecution(any())).thenReturn(
				new GetQueryExecutionResult().withQueryExecution(new QueryExecution()
						.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.QUEUED))),
				new GetQueryExecutionResult().withQueryExecution(new QueryExecution()
						.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.RUNNING))),
				new GetQueryExecutionResult().withQueryExecution(new QueryExecution()
						.withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED))));

		// call under test
		warehouseHelper.waitForQueryToFinish(queryId, maxWaitMS, intervaleMS);

		verify(mockAthenaClient, atLeast(3))
				.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryId));
	}

	static ListObjectsV2Result createListObjectV2Result(String... keys) {
		ListObjectsV2Result listObjects = new ListObjectsV2Result();
		for (String key : keys) {
			S3ObjectSummary summary = new S3ObjectSummary();
			summary.setKey(key);
			listObjects.getObjectSummaries().add(summary);
		}
		return listObjects;
	}
}
