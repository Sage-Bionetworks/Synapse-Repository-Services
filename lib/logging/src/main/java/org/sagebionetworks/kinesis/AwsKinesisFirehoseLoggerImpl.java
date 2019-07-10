package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.apache.commons.collections4.ListUtils;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	@Autowired
	AmazonKinesisFirehose kinesisFirehoseClient;
	@Autowired
	StackConfiguration stackConfiguration;

	@Override
	public void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecordList){

		//Kinesis has a record limit of 500
		List<? extends List<? extends AwsKinesisLogRecord>> partitionedRecords = ListUtils.partition(logRecordList, 500);


		for(List<? extends AwsKinesisLogRecord> recordList : partitionedRecords) {
			kinesisFirehoseClient.putRecordBatch(
					new PutRecordBatchRequest()
							.withDeliveryStreamName(kinesisStreamName(kinesisDataStreamSuffix))
							.withRecords(
									recordList.stream()
											.map(this::updateWithStackInfoAndConvertToRecord)
											.collect(Collectors.toList()))
			);
		}
	}

	private String kinesisStreamName(String kinesisDataStreamSuffix){
		return stackConfiguration.getStack() + stackConfiguration.getStackInstance() + kinesisDataStreamSuffix;
	}

	private Record updateWithStackInfoAndConvertToRecord(AwsKinesisLogRecord logRecord){
		logRecord.withStack(stackConfiguration.getStack())
				.withInstance(stackConfiguration.getStackInstance());
		return new Record().withData(ByteBuffer.wrap(logRecord.toBytes()));
	}
}

