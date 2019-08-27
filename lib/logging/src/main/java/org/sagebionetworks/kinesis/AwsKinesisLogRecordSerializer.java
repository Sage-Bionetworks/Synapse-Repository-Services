package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;

public interface AwsKinesisLogRecordSerializer {

	ByteBuffer toBytes(AwsKinesisLogRecord record);
	
}
