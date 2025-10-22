package org.sagebionetworks.repo.manager.grid;

public class PatchUtils {

	/**
	 * This is a limit set by AWS <a href=
	 * "https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-execution-service-websocket-limits-table.html">apigateway-execution-service-websocket-limits-table</a>
	 */
	public static long MAX_BYTES_PER_PATCH = 128_000L;
	
	// 1MB is the max size for SQS messages, See
	// https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/quotas-messages.html
	// Set it to 768KB so that we do not have to worry about the overhead of the message wrapper.
	public static final int MAX_CHANGE_SET_SIZE = (1024 * 1024) - (256 * 1024); // 768KB

	/**
	 * Given an expected maximum row size, calculate the number of rows that can be
	 * added to a patch while remaining under the AWS websocket limits.
	 * 
	 * @param maxRowSizeBytes
	 * @return
	 */
	public static int calculateRowsPerPatch(Long maxRowSizeBytes) {
		// Note: We estimate a 10% overhead to serialize a row as a patch.
		return maxRowSizeBytes >= MAX_BYTES_PER_PATCH ? 1
				: Math.max(1, Long.valueOf(MAX_BYTES_PER_PATCH / plusTenPerent(maxRowSizeBytes)).intValue());
	}

	/**
	 * Add 10% to the provided value.
	 * 
	 * @param value
	 * @return
	 */
	public static Long plusTenPerent(Long value) {
		return value + Double.valueOf(value * 0.1).longValue();
	}

}
