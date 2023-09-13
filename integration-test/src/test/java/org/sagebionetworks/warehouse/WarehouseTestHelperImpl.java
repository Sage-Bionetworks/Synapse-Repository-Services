package org.sagebionetworks.warehouse;

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
		saveQueryToS3(queryString, maxNumberOfHours, now);
		
		
		
	}

	/**
	 * Save the provided query to S3.
	 * 
	 * @param queryString
	 * @param maxNumberOfHours
	 * @param now
	 */
	void saveQueryToS3(String queryString, int maxNumberOfHours, Instant now) {
		Instant expiresOn = now.plus(maxNumberOfHours, ChronoUnit.HOURS);
		
		JSONObject json = new JSONObject();
		json.put("query", queryString);
		json.put("maxNumberOfHours", maxNumberOfHours);
		String jsonString = json.toString();
		
		StackTraceElement e = Thread.currentThread().getStackTrace()[2];
		
		String key =  String.format("%s/%s/%s/%d/%d.json", stackConfig.getStackInstance(), e.getClassName().replaceAll("\\.","/"), e.getMethodName(), e.getLineNumber(), expiresOn.toEpochMilli());
		
		this.s3Client.putObject(BUCKET_NAME, key, jsonString);
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
