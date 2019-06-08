package org.sagebionetworks.kinesis;

import java.util.Collection;
import java.util.stream.Stream;

public interface AwsKinesisFirehoseLogger {

	void logBatch(String kinesisDataStreamSuffix, Stream<? extends AwsKinesisLogRecord> logRecords);
}
