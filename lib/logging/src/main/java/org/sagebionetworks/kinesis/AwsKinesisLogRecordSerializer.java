package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;

public interface AwsKinesisLogRecordSerializer {

	ByteBuffer toByteBuffer(AwsKinesisLogRecord record);
	
	byte[] toBytes(AwsKinesisLogRecord record);
}
