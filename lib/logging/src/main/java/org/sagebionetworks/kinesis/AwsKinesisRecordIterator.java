package org.sagebionetworks.kinesis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.services.kinesisfirehose.model.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AwsKinesisRecordIterator implements Iterator<AwsKinesisRecord> {
	
	private static final int ONE_KiB = 1024;
	static final int RECORD_SIZE_LIMIT = ONE_KiB * 1000;
	private static final byte[] NEW_LINE_BYTES = "\n".getBytes(StandardCharsets.UTF_8);

	private ObjectMapper objectMapper;
	private List<? extends AwsKinesisLogRecord> records;
	private int currentIndex;

	public AwsKinesisRecordIterator(List<? extends AwsKinesisLogRecord> records, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.records = records;
		this.currentIndex = 0;
	}

	@Override
	public boolean hasNext() {
		return currentIndex < records.size();
	}

	@Override
	public AwsKinesisRecord next() {
		// Consume all the records up to the size limit
		AwsKinesisRecordBuilder builder = new AwsKinesisRecordBuilder(objectMapper, new ByteArrayOutputStream());

		for (; currentIndex < records.size(); currentIndex++) {
			boolean added = builder.putRecord(records.get(currentIndex));
			if (currentIndex == 0 && !added) {
				throw new IllegalStateException("A single record cannot exceed the limit of " + RECORD_SIZE_LIMIT + " bytes.");
			}
			if (!added) {
				// The last record could not be added, will retry in the next batch
				currentIndex--;
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
		
		private ObjectMapper objectMapper;
		private ByteArrayOutputStream byteArrayOutputStream;

		public AwsKinesisRecordBuilder(ObjectMapper objectMapper, ByteArrayOutputStream byteArrayOutputStream) {
			this.objectMapper = objectMapper;
			this.byteArrayOutputStream = byteArrayOutputStream;
		}
		
		/**
		 * Try to add the given record to this builder if it does exceed the record limit of kinesis firehose
		 * 
		 * @param record The record to add
		 * @return True if the record was added, false if the kinesis firehose records size limit is exceeded
		 */
		public boolean putRecord(AwsKinesisLogRecord record) {
			
			byte[] jsonBytes;
			
			try {
				jsonBytes = objectMapper.writeValueAsBytes(record);
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("Could not serialize record " + record, e);
			}
			
			// If we are over the limit don't add the record and return
			if (byteArrayOutputStream.size() + jsonBytes.length + NEW_LINE_BYTES.length > RECORD_SIZE_LIMIT) {
				return false;
			}

			try {
				byteArrayOutputStream.write(jsonBytes);
				byteArrayOutputStream.write(NEW_LINE_BYTES);
			} catch (IOException e) {
				throw new IllegalStateException("Could not serialize record " + record, e);
			}
			
			return true;
		}
		
		public AwsKinesisRecord build() {
			if (byteArrayOutputStream.size() == 0) {
				throw new IllegalStateException("No records were added to the builder.");
			}
			ByteBuffer buffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
			
			return new AwsKinesisRecord(new Record().withData(buffer), byteArrayOutputStream.size());
		}

	}

}
