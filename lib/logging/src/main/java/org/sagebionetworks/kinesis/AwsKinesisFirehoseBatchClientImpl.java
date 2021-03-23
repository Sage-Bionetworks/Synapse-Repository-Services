package org.sagebionetworks.kinesis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

@Service
public class AwsKinesisFirehoseBatchClientImpl implements AwsKinesisFirehoseBatchClient {
	
	private static final Logger LOG = LogManager.getLogger(AwsKinesisFirehoseBatchClientImpl.class);

	private AmazonKinesisFirehose kinesisFirehoseClient;
	
	private long maxRetryDelay;
	
	@Autowired
	public AwsKinesisFirehoseBatchClientImpl(AmazonKinesisFirehose kinesisFirehoseClient) {
		this.kinesisFirehoseClient = kinesisFirehoseClient;
	}

	@Autowired
	public void configure(StackConfiguration stackConfiguration) {
		Long maxDelayConfig = stackConfiguration.getKinesisMaxRetryDelay();
		
		if (maxDelayConfig == null) {
			this.maxRetryDelay = AwsKinesisFirehoseConstants.MAX_RETRY_DELAY;
		} else {
			this.maxRetryDelay = maxDelayConfig;
		}
	}
	
	@Override
	public void sendBatch(String streamName, List<AwsKinesisRecord> batch) throws AwsKinesisDeliveryException {
		ValidateArgument.required(streamName, "The streamName");
		ValidateArgument.required(batch, "The batch");
		ValidateArgument.requirement(batch.size() > 0 && batch.size() <= AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT, "The batch size must be greater than 0 and and less or equal than " + AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT);
		
		List<Record> records = new ArrayList<>(batch.size());
		int totalSize = 0;
		
		for (AwsKinesisRecord record : batch) {
			records.add(record.getRecord());
			totalSize += record.size();
			
			if (totalSize > AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT) {
				throw new IllegalArgumentException("The batch of records exceeds the maximum allowed request size of " + AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT);
			}
		}
		
		sendBatchRequest(streamName, records, 0);
	}
	
	private void sendBatchRequest(String streamName, List<Record> records, int retryNumber) {
		
		// Our base case is that we reached the max number of retries
		if (retryNumber >= AwsKinesisFirehoseConstants.MAX_RETRY_NUMBER) {
			LOG.error("Failed to deliver {} kinesis records to stream {}, gave up after {} retries.", records.size(), streamName, retryNumber);
			throw new AwsKinesisDeliveryException(String.format("Failed to deliver a batch of %d kinesis records to stream %s after %d retries.", records.size(), streamName, retryNumber));
		}
		
		long delay = getRetryDelay(retryNumber, maxRetryDelay);
		
		if (retryNumber > 0 && LOG.isDebugEnabled()) {
			LOG.debug("Retrying batch delivery after {} ms (Retry number: {})", delay, retryNumber);
		};
		
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		PutRecordBatchRequest request = new PutRecordBatchRequest()
				.withDeliveryStreamName(streamName)
				.withRecords(records);
		
		PutRecordBatchResult result = kinesisFirehoseClient.putRecordBatch(request);
	
		// Note that even though the batch request succeeded, some puts might have failed (e.g. some limit exceeded) we retry only the failed
		// records
		if (result.getFailedPutCount() > 0) {
			// The list of results is always the same size of the sent one to correlate with the sent list
			List<Record> failedRecords = new ArrayList<>(result.getFailedPutCount());
			
			int index = 0;
			
			for (PutRecordBatchResponseEntry recordResult : result.getRequestResponses()) {
				if (recordResult.getErrorCode() != null) {
					LOG.warn("Record delivery in a batch did not succeed, will retry: {} (Error Code: {}, Stream: {})", recordResult.getErrorMessage(), recordResult.getErrorCode(), streamName);
					failedRecords.add(records.get(index));
				}
				++index;
			}
			
			sendBatchRequest(streamName, failedRecords, retryNumber + 1);
			
		}
		
	}
	
	// Exponential backoff strategy with equal jitter, reference from AWS recommendation: https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
	// The conclusion is that full jitter is better, but it probably depends on the context. Since we don't have enough data we follow the reference implementation 
	// used by the AWS SDK: See PredefinedBackoffStrategies
	private static long getRetryDelay(int retryNumber, long maxRetryDelay) {
		if (retryNumber == 0) {
			return 0;
		}
		
		// Standard back-off strategy used by AWS, min(max_delay, startingDelay * (factor^retryNumber) 
		long delay = (long) Math.min(maxRetryDelay, AwsKinesisFirehoseConstants.BASE_RETRY_DELAY * Math.pow(AwsKinesisFirehoseConstants.BACKOFF_FACTOR, retryNumber - 1));
		
		// Add equal jitter, note that the SDK switches between a full jitter and an equal jitter according to the type of exception (e.g. equal for throttling, full for non-throttling).
		// Here we simply use equal because we expect to not being able to deliver a record mostly because of throttling
		return  (delay / 2) + ThreadLocalRandom.current().nextLong(delay / 2 + 1);
		
	}

}
