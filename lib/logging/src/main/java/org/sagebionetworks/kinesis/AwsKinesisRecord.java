package org.sagebionetworks.kinesis;

import com.amazonaws.services.kinesisfirehose.model.Record;

/**
 * Wrapper for a kinesis firehose {@link Record} that includes the size in bytes of the record
 */
public class AwsKinesisRecord {
	
	private Record record;
	private int size;

	public AwsKinesisRecord(Record record, int size) {
		this.record = record;
		this.size = size;
	}
	
	public Record getRecord() {
		return record;
	}
	
	public int size() {
		return size;
	}

}
