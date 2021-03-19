package org.sagebionetworks.kinesis;

public interface AwsKinesisLogRecordSerializer {
	
	byte[] toBytes(AwsKinesisLogRecord record);
}
