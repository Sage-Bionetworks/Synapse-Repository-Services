package org.sagebionetworks.kinesis;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface AwsKinesisFirehoseLogger {

	void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecords);
}
