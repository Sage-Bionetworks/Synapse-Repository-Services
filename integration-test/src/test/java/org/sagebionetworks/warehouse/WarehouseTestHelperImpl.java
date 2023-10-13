package org.sagebionetworks.warehouse;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.function.Executable;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.s3.AmazonS3;

public class WarehouseTestHelperImpl implements WarehouseTestHelper {

	public static final int MAX_WAIT_MS = 60_000;
	public static final int WAIT_INTERAVAL_MS = 1000;
	public static final String BUCKET_NAME = "dev.testdata.sagebase.org";
	public static final int WAREHOUSE_QUERY_EXPIRATION_HOURS = 2;

	private final AmazonS3 s3Client;
	private final AmazonAthena athenaClient;
	private final Clock clock;
	private final StackConfiguration stackConfig;

	public WarehouseTestHelperImpl(AmazonS3 s3Client, AmazonAthena athenaClient, Clock clock,
			StackConfiguration stackConfig) {
		super();
		this.s3Client = s3Client;
		this.athenaClient = athenaClient;
		this.clock = clock;
		this.stackConfig = stackConfig;
	}

	@Override
	public void assertWarehouseQuery(String queryString) throws Exception {

		Instant now = Instant.ofEpochMilli(clock.currentTimeMillis());

		StackTraceElement callersElement = Thread.currentThread().getStackTrace()[2];
		String callersPath = getCallersPath(callersElement);

		saveQueryToS3(queryString, now, callersPath, WAREHOUSE_QUERY_EXPIRATION_HOURS);

		List<String> previousQueryKeysToCheck = s3Client.listObjectsV2(BUCKET_NAME, callersPath).getObjectSummaries()
				.stream().filter(s -> now.toEpochMilli() >= getExpiresOnFromKey(s.getKey())).map(s -> s.getKey())
				.collect(Collectors.toList());
		
		try {
			assertAll("Group of all previous queries ready to be tested",
					previousQueryKeysToCheck.stream().map(this::executeQueryAndAssertResults));
		} finally {
			previousQueryKeysToCheck.forEach(key -> s3Client.deleteObject(BUCKET_NAME, key));
		}
	}

	String getCallersPath(StackTraceElement callersElement) {
		String callersPath = String.format("%s/%s/%s/%d", stackConfig.getStackInstance(),
				callersElement.getClassName().replaceAll("\\.", "/"), callersElement.getMethodName(),
				callersElement.getLineNumber());
		return callersPath;
	}
	
	Executable executeQueryAndAssertResults(String key) {
		return ()->{
			String previousQueryString = s3Client.getObjectAsString(BUCKET_NAME, key);
			int count = executeCountQuery(previousQueryString);
			assertTrue(count > 0, "Expected count > 0 for query: " + previousQueryString);
		};
	}
	
	int executeCountQuery(String previousQueryString) throws Exception {
		String queryExecutionId = athenaClient
				.startQueryExecution(new StartQueryExecutionRequest()
						.withQueryExecutionContext(new QueryExecutionContext()
								.withCatalog("AwsDataCatalog").withDatabase("datawarehouse"))
						.withQueryString(previousQueryString))
				.getQueryExecutionId();

		waitForQueryToFinish(queryExecutionId, MAX_WAIT_MS, WAIT_INTERAVAL_MS);

		ResultSet queryResults = athenaClient
				.getQueryResults(
						new GetQueryResultsRequest().withQueryExecutionId(queryExecutionId))
				.getResultSet();
		return extractCountFromResult(queryResults);
	}

	int extractCountFromResult(ResultSet queryResults) {
		assertNotNull(queryResults.getResultSetMetadata());
		assertNotNull(queryResults.getResultSetMetadata().getColumnInfo());
		assertEquals(1, queryResults.getResultSetMetadata().getColumnInfo().size());
		assertNotNull(queryResults.getRows());
		// first row is a header, second contains the count
		assertEquals(2, queryResults.getRows().size());
		assertNotNull(queryResults.getRows().get(1).getData());
		assertEquals(1, queryResults.getRows().get(1).getData().size());
		return Integer.parseInt(queryResults.getRows().get(1).getData().get(0).getVarCharValue());
	}

	void waitForQueryToFinish(String queryExecutionId, int maxWaitMS, int waitIntervalMs) throws Exception {
		TimeUtils.waitFor(maxWaitMS, waitIntervalMs, () -> {
			QueryExecution execution = athenaClient
					.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryExecutionId))
					.getQueryExecution();
			QueryExecutionState state = QueryExecutionState.valueOf(execution.getStatus().getState());
			switch (state) {
			case CANCELLED:
				throw new IllegalStateException(
						"Athena query canceled: " + execution.getStatus().getStateChangeReason());
			case FAILED:
				throw new IllegalStateException("Athena query failed: " + execution.getStatus().getStateChangeReason());
			case QUEUED:
			case RUNNING:
				System.out.println("Waiting for Athena query: " + queryExecutionId + " to finish..");
				return new Pair<Boolean, Void>(false, null);
			case SUCCEEDED:
				return new Pair<Boolean, Void>(true, null);
			default:
				throw new IllegalStateException("Unknown athena query state: " + state);
			}
		});
	}

	public static long getExpiresOnFromKey(String key) {
		String[] slashSplit = key.split("/");
		return Long.parseLong(slashSplit[slashSplit.length - 1].split("\\.")[0]);
	}

	/**
	 * Save the provided query to S3.
	 * 
	 * @param queryString
	 * @param maxNumberOfHours
	 * @param now
	 */
	void saveQueryToS3(String queryString, Instant now, String callersPath, int expirationHours) {
		Instant expiresOn = now.plus(expirationHours, ChronoUnit.HOURS);
		String key = String.format("%s/%d.sql", callersPath, expiresOn.toEpochMilli());
		this.s3Client.putObject(BUCKET_NAME, key, queryString);
	}

	@Override
	public String toDateStringBetweenPlusAndMinusFiveSeconds(Instant instant) {
		return String.format("between date('%s') and date('%s')",
				instant.minusSeconds(5).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE),
				instant.plusSeconds(5).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE));
	}

	@Override
	public String toIsoTimestampStringBetweenPlusAndMinusFiveSeconds(Instant instant) {
		return String.format("between from_iso8601_timestamp('%s') and from_iso8601_timestamp('%s')",
				instant.minusSeconds(5).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME),
				instant.plusSeconds(5).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME));
	}

}
