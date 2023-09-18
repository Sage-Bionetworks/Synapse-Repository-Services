package org.sagebionetworks.warehouse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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

	public static final long MAX_WAIT_MS = 60_000L;
	public static final long WAIT_INTERAVAL_MS = 2000;
	public static final String BUCKET_NAME = "dev.testdata.sagebase.org";
	public static final int MAX_NUMBER_HOURS = 0;

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
		String callersPath = String.format("%s/%s/%s/%d", stackConfig.getStackInstance(),
				callersElement.getClassName().replaceAll("\\.", "/"), callersElement.getMethodName(),
				callersElement.getLineNumber());

		saveQueryToS3(queryString, now, callersPath);

		List<String> previousQueryKeysToCheck = s3Client.listObjectsV2(BUCKET_NAME, callersPath).getObjectSummaries()
				.stream().filter(s -> now.toEpochMilli() >= getExpiresOnFromKey(s.getKey())).map(s -> s.getKey())
				.collect(Collectors.toList());

		for (String previousKeyToCheck : previousQueryKeysToCheck) {
			String previousQueryString = s3Client.getObjectAsString(BUCKET_NAME, previousKeyToCheck);
			String queryExecutionId = athenaClient.startQueryExecution(new StartQueryExecutionRequest()
					.withQueryExecutionContext(
							new QueryExecutionContext().withCatalog("AwsDataCatalog").withDatabase("datawarehouse"))
					.withQueryString(previousQueryString)).getQueryExecutionId();
			
			waitForQueryToFinish(queryExecutionId);
			
			ResultSet queryResults = athenaClient
					.getQueryResults(new GetQueryResultsRequest().withQueryExecutionId(queryExecutionId)).getResultSet();
			System.out.println(queryResults);
//			assertNotNull(queryResults);
//			assertNotNull(queryResults.getRows());
//			assertEquals(1, queryResults.getRows().size());

		}

//		assertAll(
//				  "Previous queries for path: "+callersPath,
//				  () -> assertEquals("admin", user.getUsername(), "Username should be admin"),
//				  () -> assertEquals("admin@baeldung.com", user.getEmail(), "Email should be admin@baeldung.com"),
//				  () -> assertTrue(user.getActivated(), "User should be activated")
//		);

	}

	private void waitForQueryToFinish(String queryExecutionId) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, WAIT_INTERAVAL_MS, () -> {
			QueryExecution execution = athenaClient
					.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(queryExecutionId))
					.getQueryExecution();
			QueryExecutionState state = QueryExecutionState.valueOf(execution.getStatus().getState());
			switch (state) {
			case CANCELLED:
				throw new IllegalStateException(
						"Athena query canceled: " + execution.getStatus().getStateChangeReason());
			case FAILED:
				throw new IllegalStateException(
						"Athena query failed: " + execution.getStatus().getStateChangeReason());
			case QUEUED:
			case RUNNING:
				System.out.println("Waiting for Athena query to finish...");
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
	void saveQueryToS3(String queryString, Instant now, String callersPath) {
		Instant expiresOn = now.plus(MAX_NUMBER_HOURS, ChronoUnit.HOURS);
		String key = String.format("%s/%d.sql", callersPath, expiresOn.toEpochMilli());
		this.s3Client.putObject(BUCKET_NAME, key, queryString);
	}

}
