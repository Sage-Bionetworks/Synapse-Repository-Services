package org.sagebionetworks.kinesis;

import java.nio.charset.StandardCharsets;

public class AwsKinesisFirehoseConstants {

	public static final int ONE_KiB = 1024;
	
	// 1000 KiB limit for a single record
	public static final int RECORD_SIZE_LIMIT = ONE_KiB * 1000;
	
	// 4MiB limit for a single request
	public static final int REQUEST_SIZE_LIMIT = ONE_KiB * ONE_KiB * 4;
	
	// Kinesis has a record limit of 500 for each batch
	public static final int PUT_BATCH_MAX_RECORD_LIMIT = 500;
	
	// Separator for each json object in a record
	public static final byte[] NEW_LINE_BYTES = "\n".getBytes(StandardCharsets.UTF_8);
	
	public static final int MAX_RETRY_NUMBER = 10;
	
	public static final long BASE_RETRY_DELAY = 250;

	public static final long MAX_RETRY_DELAY = 10000;
	
	public static final double BACKOFF_FACTOR = 2.0;
	
}
