package org.sagebionetworks.kinesis;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.AmazonKinesisFirehoseException;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

@Component
public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	private static final Logger LOG = LogManager.getLogger(AwsKinesisFirehoseLoggerImpl.class);

	private AmazonKinesisFirehose kinesisFirehoseClient;

	private AwsKinesisLogRecordSerializer kinesisRecordSerializer;

	private StackConfiguration stackConfiguration;
	
	private String stack;
	
	private String instance;	

	@Autowired
	public AwsKinesisFirehoseLoggerImpl(AmazonKinesisFirehose kinesisFirehoseClient, AwsKinesisLogRecordSerializer kinesisRecordSerializer, StackConfiguration stackConfiguration) {
		this.kinesisFirehoseClient = kinesisFirehoseClient;
		this.kinesisRecordSerializer = kinesisRecordSerializer;
		this.stackConfiguration = stackConfiguration;
		this.stack = stackConfiguration.getStack();
		this.instance = stackConfiguration.getStackInstance();
	}

	@Override
	public void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecordList) {

		List<? extends List<? extends AwsKinesisLogRecord>> partitionedRecords = ListUtils.partition(logRecordList,
				AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT);

		for (List<? extends AwsKinesisLogRecord> batch : partitionedRecords) {
			PutRecordBatchRequest batchRequest = buildBatchRequest(kinesisDataStreamSuffix, batch);
			try {
				PutRecordBatchResult result = kinesisFirehoseClient.putRecordBatch(batchRequest);
				if (result.getFailedPutCount() > 0) {
					LOG.warn("Could not send {} records to stream {}", result.getFailedPutCount(), kinesisDataStreamSuffix);
				}
			} catch (AmazonKinesisFirehoseException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private PutRecordBatchRequest buildBatchRequest(String kinesisDataStreamSuffix,
			List<? extends AwsKinesisLogRecord> batch) {
		return new PutRecordBatchRequest()
				.withDeliveryStreamName(kinesisStreamName(kinesisDataStreamSuffix))
				.withRecords(batch.stream().map(this::updateWithStackInfoAndConvertToRecord).collect(Collectors.toList()));
	}

	private String kinesisStreamName(String kinesisDataStreamSuffix) {
		return stackConfiguration.getStack() + stackConfiguration.getStackInstance() + kinesisDataStreamSuffix;
	}

	private Record updateWithStackInfoAndConvertToRecord(AwsKinesisLogRecord logRecord) {
		logRecord.withStack(stack).withInstance(instance);
		return new Record().withData(kinesisRecordSerializer.toBytes(logRecord));
	}

}
