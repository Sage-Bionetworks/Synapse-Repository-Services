package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.springframework.beans.factory.annotation.Autowired;

public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {
	AmazonKinesisFirehose kinesisFirehoseClient;

	@Autowired
	public AwsKinesisFirehoseLoggerImpl(AmazonKinesisFirehose kinesisFirehoseClient) {
		this.kinesisFirehoseClient = kinesisFirehoseClient;
	}



	@Override
	public void log(AwsKinesisLogRecord logRecord){
		kinesisFirehoseClient.putRecord(
				new PutRecordRequest()
						.withDeliveryStreamName(logRecord.kinesisDataStreamSuffix())
						.withRecord(new Record()
								.withData(ByteBuffer.wrap(KinesisRecordToJSON.toBytes(logRecord)))
						)
		);
	}
}

