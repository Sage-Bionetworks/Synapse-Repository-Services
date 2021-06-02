package org.sagebionetworks.kinesis;

import java.util.List;

/**
 * Abstraction over the kinesis firehose client to send a batch of records in a single request, handling failure and retries
 */
public interface AwsKinesisFirehoseBatchClient {

	/**
	 * Sends the given batch of {@link AwsKinesisRecord} to the stream with the given name
	 *  
	 * @param batch A batch of {@link AwsKinesisRecord}, must be less than 500 and the total size must be less than 4MiB
	 * @throws AwsKinesisDeliveryException If not all the records in the batch could be delivered to the stream
	 */
	void sendBatch(String streamName, List<AwsKinesisRecord> batch) throws AwsKinesisDeliveryException;
	
}
