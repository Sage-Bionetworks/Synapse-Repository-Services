package org.sagebionetworks.kinesis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.services.kinesisfirehose.model.Record;

/**
 * An iterator over {@link AwsKinesisLogRecord} that will produce aggregated {@link AwsKinesisRecord} containing as 
 * many serialized {@link AwsKinesisLogRecord} as possible till the specified limit. Each serialized {@link AwsKinesisLogRecord}
 * will be separated by a new line.
 */
public class AwsKinesisRecordIterator implements Iterator<AwsKinesisRecord> {
	
	private List<? extends AwsKinesisLogRecord> records;
	private AwsKinesisLogRecordSerializer recordSerializer;
	private int recordSizeLimit;
	private int currentIndex;

	public AwsKinesisRecordIterator(List<? extends AwsKinesisLogRecord> records, AwsKinesisLogRecordSerializer recordSerializer, int recordSizeLimit) {
		this.records = records;
		this.recordSerializer = recordSerializer;
		this.recordSizeLimit = recordSizeLimit;
		this.currentIndex = 0;
	}

	@Override
	public boolean hasNext() {
		return currentIndex < records.size();
	}

	@Override
	public AwsKinesisRecord next() {
		// Consume all the records up to the size limit
		AwsKinesisRecordBuilder builder = new AwsKinesisRecordBuilder(recordSerializer, recordSizeLimit);

		for (; currentIndex < records.size(); currentIndex++) {
			boolean added = builder.putRecord(records.get(currentIndex));
			if (currentIndex == 0 && !added) {
				throw new IllegalStateException("A single record cannot exceed the limit of " + recordSizeLimit + " bytes.");
			}
			if (!added) {
				break;
			}
		}

		return builder.build();
	}
	
	/**
	 * Helper to build a single kinesis firehose record from a set of {@link AwsKinesisLogRecord}. Will pack a serialized JSON records each separated by a newline
	 * 
	 */
	private static final class AwsKinesisRecordBuilder {
		
		private AwsKinesisLogRecordSerializer serializer;
		private int recordSizeLimit;
		private ByteArrayOutputStream byteArrayOutputStream;

		public AwsKinesisRecordBuilder(AwsKinesisLogRecordSerializer serializer, int recordSizeLimit) {
			this.serializer = serializer;
			this.recordSizeLimit = recordSizeLimit;
			this.byteArrayOutputStream = new ByteArrayOutputStream();
		}
		
		/**
		 * Try to add the given record to this builder if it does exceed the record limit of kinesis firehose
		 * 
		 * @param record The record to add
		 * @return True if the record was added, false if the kinesis firehose records size limit is exceeded
		 */
		public boolean putRecord(AwsKinesisLogRecord record) {
			if (byteArrayOutputStream.size() >= recordSizeLimit) {
				return false;
			}
			
			byte[] jsonBytes = serializer.toBytes(record);
			
			// If we are over the limit don't add the record and return
			if (byteArrayOutputStream.size() + jsonBytes.length + AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length > recordSizeLimit) {
				return false;
			}

			try {
				byteArrayOutputStream.write(jsonBytes);
				byteArrayOutputStream.write(AwsKinesisFirehoseConstants.NEW_LINE_BYTES);
			} catch (IOException e) {
				throw new IllegalStateException("Could not serialize record " + record, e);
			}
			
			return true;
		}
		
		public AwsKinesisRecord build() {
			if (byteArrayOutputStream.size() == 0) {
				throw new IllegalStateException("No records were added to the builder.");
			}
			
			byte[] data = byteArrayOutputStream.toByteArray();
			
			ByteBuffer buffer = ByteBuffer.wrap(data);
			
			return new AwsKinesisRecord(new Record().withData(buffer), data.length);
		}

	}

}
