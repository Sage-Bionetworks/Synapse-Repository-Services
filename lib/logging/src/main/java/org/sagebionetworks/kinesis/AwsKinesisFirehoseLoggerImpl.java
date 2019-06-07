package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	@Autowired
	AmazonKinesisFirehose kinesisFirehoseClient;
	@Autowired
	StackConfiguration stackConfiguration;

	@Override
	public void logBatch(String kinesisDataStreamSuffix, Stream<? extends AwsKinesisLogRecord> logRecordStream){
		kinesisFirehoseClient.putRecordBatch(
				new PutRecordBatchRequest()
						.withDeliveryStreamName(kinesisStreamName(kinesisDataStreamSuffix))
						.withRecords(
								logRecordStream
										.map((logRecord) -> {
											logRecord.withStack(stackConfiguration.getStack())
													.withInstance(stackConfiguration.getStackInstance());
											return new Record().withData(ByteBuffer.wrap(logRecord.toBytes()));})
										.collect(Collectors.toList()))
		);
	}

	private String kinesisStreamName(String kinesisDataStreamSuffix){
		return stackConfiguration.getStack() + stackConfiguration.getStackInstance() + kinesisDataStreamSuffix;
	}

}

