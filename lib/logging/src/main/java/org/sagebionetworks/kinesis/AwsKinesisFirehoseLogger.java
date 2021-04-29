package org.sagebionetworks.kinesis;

import java.util.List;

public interface AwsKinesisFirehoseLogger {

	void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecords) throws AwsKinesisDeliveryException;
	
	// Allows to update the delivery time buffer for a kinesis stream, use only for testing
	int updateKinesisDeliveryTime(String kinesisDataStreamSuffix, int intervalInSeconds);
}
