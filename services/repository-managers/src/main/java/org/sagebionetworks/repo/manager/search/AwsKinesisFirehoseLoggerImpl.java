package org.sagebionetworks.repo.manager.search;

import java.nio.ByteBuffer;
import javax.annotation.PostConstruct;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.CompressionFormat;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamType;
import com.amazonaws.services.kinesisfirehose.model.ExtendedS3DestinationConfiguration;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.sagebionetworks.StackConfiguration;
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
						.withDeliveryStreamName(logRecord.kinesisDataStreamName())
						.withRecord(new Record()
								.withData(ByteBuffer.wrap(logRecord.toBytes()))
						)
		);
	}



}

