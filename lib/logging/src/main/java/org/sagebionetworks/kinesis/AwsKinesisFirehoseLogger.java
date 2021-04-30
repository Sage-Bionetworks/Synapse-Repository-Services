package org.sagebionetworks.kinesis;

import java.util.List;

public interface AwsKinesisFirehoseLogger {

	void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecords) throws AwsKinesisDeliveryException;
}
