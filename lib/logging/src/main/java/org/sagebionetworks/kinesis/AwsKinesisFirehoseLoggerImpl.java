package org.sagebionetworks.kinesis;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	private AwsKinesisFirehoseBatchClient kinesisFirehoseBatchClient;
	
	private AwsKinesisLogRecordSerializer kinesisRecordSerializer;
	
	private String stack;
	
	private String instance;

	@Autowired
	public AwsKinesisFirehoseLoggerImpl(AwsKinesisFirehoseBatchClient kinesisFirehoseClient,  AwsKinesisLogRecordSerializer kinesisRecordSerializer) {
		this.kinesisFirehoseBatchClient = kinesisFirehoseClient;
		this.kinesisRecordSerializer = kinesisRecordSerializer;
	}
	
	@Autowired
	public void configure(StackConfiguration stackConfiguration) {
		this.stack = stackConfiguration.getStack();
		this.instance = stackConfiguration.getStackInstance();
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
			
			if (batchSize + next.size() > AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT || batch.size() == AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT) {
				kinesisFirehoseBatchClient.sendBatch(streamName, batch);
				batch = new LinkedList<>();
				batchSize = 0;
			}

			// Always add to the next batch
			batch.add(next);
			batchSize += next.size();
			
		}
		
		if (!batch.isEmpty()) {
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}
		
	}
	
	private String kinesisStreamName(String kinesisDataStreamSuffix) {
		return stack + instance + kinesisDataStreamSuffix;
	}

}
