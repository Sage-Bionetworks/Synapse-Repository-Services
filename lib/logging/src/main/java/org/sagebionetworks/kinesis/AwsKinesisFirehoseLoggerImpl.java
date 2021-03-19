package org.sagebionetworks.kinesis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

@Component
public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	private static final Logger LOG = LogManager.getLogger(AwsKinesisFirehoseLoggerImpl.class);

	private AmazonKinesisFirehose kinesisFirehoseClient;
	
	private AwsKinesisLogRecordSerializer kinesisRecordSerializer;
	
	private String stack;
	
	private String instance;
	
	private long maxRetryDelay;

	@Autowired
	public AwsKinesisFirehoseLoggerImpl(AmazonKinesisFirehose kinesisFirehoseClient,  AwsKinesisLogRecordSerializer kinesisRecordSerializer) {
		this.kinesisFirehoseClient = kinesisFirehoseClient;
		this.kinesisRecordSerializer = kinesisRecordSerializer;
	}
	
	@Autowired
	public void configure(StackConfiguration stackConfiguration) {
		this.stack = stackConfiguration.getStack();
		this.instance = stackConfiguration.getStackInstance();
		
		Long maxDelayConfig = stackConfiguration.getKinesisMaxRetryDelay();
		
		if (maxDelayConfig == null) {
			this.maxRetryDelay = AwsKinesisFirehoseConstants.MAX_RETRY_DELAY;
		} else {
			this.maxRetryDelay = maxDelayConfig;
		}
	}

	@Override
	public void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecords) {
		ValidateArgument.required(kinesisDataStreamSuffix, "The kinesisDataStreamSuffix");
		ValidateArgument.required(logRecords, "The logRecords");
		
		if (logRecords.isEmpty()) {
			return;
		}
		
		// Override the stack and instance if needed
		logRecords.forEach( record-> {
			if (record.getStack() == null) {
				record.withStack(stack);
			}
			if (record.getInstance() == null) {
				record.withInstance(instance);
			}
		});

		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(logRecords, kinesisRecordSerializer, AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT);

		String streamName = kinesisStreamName(kinesisDataStreamSuffix);
		List<AwsKinesisRecord> batch = new LinkedList<>();
		int batchSize = 0;
		
		while (recordIterator.hasNext()) {
			AwsKinesisRecord next = recordIterator.next();
			
			if (batchSize + next.size() > AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT || batch.size() == AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT) {
				sendBatchRequest(streamName, batch);
				batch.clear();
				batchSize = 0;
			}

			// Always add to the next batch
			batch.add(next);
			batchSize += next.size();
			
		}
		
		if (!batch.isEmpty()) {
			sendBatchRequest(streamName, batch);
		}
		
	}
	
	private void sendBatchRequest(String streamName, List<AwsKinesisRecord> batch) {
		sendBatchRequest(streamName, batch, 0);
	}
	
	private void sendBatchRequest(String streamName, List<AwsKinesisRecord> batch, int retryNumber) {
		
		// Our base case is that we reached the max number of retries
		if (retryNumber >= AwsKinesisFirehoseConstants.MAX_RETRY_NUMBER) {
			LOG.error("Failed to deliver {} kinesis records to stream {}, gave up after {} retries.", batch.size(), streamName, retryNumber);
			throw new AwsKinesisDeliveryException(String.format("Failed to deliver a batch of %d kinesis records to stream %s after %d retries.", batch.size(), streamName, retryNumber));
		}
		
		long delay = getRetryDelay(retryNumber, maxRetryDelay);
		
		if (delay > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Retrying batch delivery after {} ms (Retry number: {})", delay, retryNumber);
			}
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		List<Record> records = batch.stream().map(AwsKinesisRecord::getRecord).collect(Collectors.toList()); 
		
		PutRecordBatchRequest request = new PutRecordBatchRequest()
				.withDeliveryStreamName(streamName)
				.withRecords(records);
		
		PutRecordBatchResult result = kinesisFirehoseClient.putRecordBatch(request);
	
		// Note that even though the batch request succeeded, some puts might have failed (e.g. some limit exceeded) we retry only the failed
		// records
		if (result.getFailedPutCount() > 0) {
			// The list of results is always the same size of the sent one to correlate with the sent list
			List<AwsKinesisRecord> failedRecords = new ArrayList<>(result.getFailedPutCount());
			
			int index = 0;
			
			for (PutRecordBatchResponseEntry recordResult : result.getRequestResponses()) {
				if (recordResult.getErrorCode() != null) {
					LOG.warn("Record delivery in a batch did not succeed, will retry: {} (Error Code: {}, Stream: {})", recordResult.getErrorMessage(), recordResult.getErrorCode(), streamName);
					failedRecords.add(batch.get(index));
				}
				++index;
			}
			
			sendBatchRequest(streamName, failedRecords, retryNumber + 1);
			
		}
		
	}
	
	// Exponential backoff strategy with equal jitter, reference from AWS recommendation: https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
	// Also see the reference implementation in the AWS SDK: PredefinedBackoffStrategies
	private static long getRetryDelay(int retryNumber, long maxRetryDelay) {
		if (retryNumber == 0) {
			return 0;
		}
		
		// Standard back-off strategy used by AWS, min(max_delay, startingDelay * (factor^retryNumber) 
		long delay = (long) Math.min(maxRetryDelay, AwsKinesisFirehoseConstants.BASE_RETRY_DELAY * Math.pow(AwsKinesisFirehoseConstants.BACKOFF_FACTOR, retryNumber - 1));
		
		// Add equal jitter
		return  (delay / 2) + ThreadLocalRandom.current().nextLong(delay / 2 + 1);
		
	}

	private String kinesisStreamName(String kinesisDataStreamSuffix) {
		return stack + instance + kinesisDataStreamSuffix;
	}

}
