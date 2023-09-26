package org.sagebionetworks.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
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

	@Captor
	ArgumentCaptor<String> stringCaptor;

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
		when(mockStackConfig.getStackInstance()).thenReturn("anInstance");
		when(mockClock.currentTimeMillis()).thenReturn(1001L);
		when(mockS3Client.listObjectsV2(any(), any())).thenReturn(createListObjectV2Result("one/123.sql", "two/"+Integer.MAX_VALUE+".sql", "three/456.sql"));
		int maxNumberHours = 2;
		
		String query = "select count(*) from foo";
		
		// call under test
		warehouseHelper.assertWarehouseQuery(query, maxNumberHours);
		
		verify(mockS3Client).putObject(eq(WarehouseTestHelperImpl.BUCKET_NAME),stringCaptor.capture(),eq(query));
		
		assertTrue(stringCaptor.getValue().startsWith("anInstance/org/sagebionetworks/warehouse/WarehouseTestHelperImplTest/testAssertWarehouseQuery/"));
		assertTrue(stringCaptor.getValue().endsWith("/7201001.sql"));

		verify(mockClock).currentTimeMillis();
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
