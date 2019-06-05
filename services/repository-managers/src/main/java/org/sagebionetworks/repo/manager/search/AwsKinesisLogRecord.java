package org.sagebionetworks.repo.manager.search;

import java.nio.ByteBuffer;

public interface AwsKinesisLogRecord {

	/**
	 * Create bytes representing its generation.
	 * Implementations are responsible for including their desired record delimiters into the returned bytes.
	 * @return
	 */
	public byte[] toBytes();

	/**
	 * The Kinesis datastream to which this record should be pushed
	 * @return
	 */
	public String kinesisDataStreamName();
}
