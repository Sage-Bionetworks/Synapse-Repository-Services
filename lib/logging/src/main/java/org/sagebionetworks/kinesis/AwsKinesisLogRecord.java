package org.sagebionetworks.kinesis;

import java.nio.ByteBuffer;

public interface AwsKinesisLogRecord {

	/**
	 * The Kinesis datastream to which this record should be pushed
	 * @return
	 */
	public String kinesisDataStreamSuffix();
}
