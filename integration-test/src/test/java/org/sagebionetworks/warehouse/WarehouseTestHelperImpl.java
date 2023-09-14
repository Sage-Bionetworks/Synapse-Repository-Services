package org.sagebionetworks.warehouse;

import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.s3.AmazonS3;

public class WarehouseTestHelperImpl implements WarehouseTestHelper {
	
	public static final String BUCKET_NAME = "dev.testdata.sagebase.org";
	
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
	public void assertWarehouseQuery(String queryString, int maxNumberOfHours) {
		
		Instant now = Instant.ofEpochMilli(clock.currentTimeMillis());
		// this method call is at index zero so the caller's index is one.
		StackTraceElement callersElement = Thread.currentThread().getStackTrace()[1];
		String callersPath = String.format("%s/%s/%s/%d", stackConfig.getStackInstance(),
				callersElement.getClassName().replaceAll("\\.", "/"), callersElement.getMethodName(),
				callersElement.getLineNumber());
		
		saveQueryToS3(queryString, maxNumberOfHours, now, callersPath);
		
		
		s3Client.listObjectsV2(BUCKET_NAME, callersPath).getObjectSummaries().stream().forEach(s->{
			String fileName = s.getKey().substring(callersPath.length());
		});
		
//		assertAll(
//				  "Previous queries for path: "+callersPath,
//				  () -> assertEquals("admin", user.getUsername(), "Username should be admin"),
//				  () -> assertEquals("admin@baeldung.com", user.getEmail(), "Email should be admin@baeldung.com"),
//				  () -> assertTrue(user.getActivated(), "User should be activated")
//		);
		
	}

	/**
	 * Save the provided query to S3.
	 * 
	 * @param queryString
	 * @param maxNumberOfHours
	 * @param now
	 */
	void saveQueryToS3(String queryString, int maxNumberOfHours, Instant now, String callersPath) {
		Instant expiresOn = now.plus(maxNumberOfHours, ChronoUnit.HOURS);
		String key = String.format("%s/%d.sql", callersPath, expiresOn.toEpochMilli());
		this.s3Client.putObject(BUCKET_NAME, key, queryString);
	}
	
	/**
	 * Create a path from the caller's class/method/lineNumber
	 * @return
	 */
	static String getStackTracePath() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		StackTraceElement e = elements[3];
		return String.format("%s/%s/%d", e.getClassName().replaceAll("\\.","/"), e.getMethodName(), e.getLineNumber());
	}
	
}
