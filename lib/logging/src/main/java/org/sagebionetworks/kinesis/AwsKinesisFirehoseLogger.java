package org.sagebionetworks.kinesis;

public interface AwsKinesisFirehoseLogger {

	void log(AwsKinesisLogRecord logRecord);
}
