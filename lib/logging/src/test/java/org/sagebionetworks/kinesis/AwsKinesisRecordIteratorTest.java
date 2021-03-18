package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AwsKinesisRecordIteratorTest {
	
	@Mock
	private AwsKinesisLogRecordSerializer mockSerializer;
	
	private int recordSizeLimit;
	
	@BeforeEach
	public void before() {
		recordSizeLimit = 1000;
	}

	@Test
	public void testIteratorWithNoRecords() {
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(Collections.emptyList(), mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> expected = Collections.emptyList();
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testIteratorWithSingleRecord() {
		
		when(mockSerializer.toBytes(any())).thenReturn(new byte[recordSizeLimit/2]);
		
		TestRecord record = TestRecord.generateRecords(1).get(0);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(Collections.singletonList(record), mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(1, result.size());
		
		verify(mockSerializer).toBytes(record);
	}
	
	@Test
	public void testIteratorWithMultipleRecordsUnderLimit() {
		
		int recordCount = 10;
		
		when(mockSerializer.toBytes(any())).thenReturn(new byte[recordSizeLimit / recordCount - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length * recordCount]);
		
		List<TestRecord> records = TestRecord.generateRecords(recordCount);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(records, mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(1, result.size());
		
		records.forEach( r-> {
			verify(mockSerializer).toBytes(r);
		});
	}
	
	@Test
	public void testIteratorWithMultipleRecordsOverLimit() {
		
		when(mockSerializer.toBytes(any())).thenReturn(
				new byte[recordSizeLimit/3],
				new byte[recordSizeLimit/3],
				new byte[recordSizeLimit/3]
		);
		
		List<TestRecord> records = TestRecord.generateRecords(3);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(records, mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(2, result.size());
		
		int index = 0;
		for (TestRecord record : records) {
			if (index == 2) {				
				// The last record in this case didn't fit and we had to go to another record
				verify(mockSerializer, times(2)).toBytes(record);
			} else {
				verify(mockSerializer).toBytes(record);
			}
			
			++index;
		}
	}
		
	
	@Test
	public void testIteratorWithMultipleRecordsOverLimitWithFit() {
		
		// 2 records fits perfectly into one
		when(mockSerializer.toBytes(any())).thenReturn(
				new byte[recordSizeLimit/2 - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length],
				new byte[recordSizeLimit/2 - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length],
				// The last one does not fit anymore
				new byte[recordSizeLimit/2]
		);
		
		List<TestRecord> records = TestRecord.generateRecords(3);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(records, mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(2, result.size());
		
		for (TestRecord record : records) {
			// All records are serialized once as the first one is a perfect fit
			verify(mockSerializer).toBytes(record);
		}
		
	}
	
	@Test
	public void testIteratorWithMultipleRecordsOverLimitSingle() {
		
		// Each record is of half the size of the limit, but we also have a newline character so each record will at max contain a single object (+newline)
		when(mockSerializer.toBytes(any())).thenReturn(new byte[recordSizeLimit/2]);
		
		List<TestRecord> records = TestRecord.generateRecords(3);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(records, mockSerializer, recordSizeLimit);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(3, result.size());
		
		int index = 0;
		
		for (TestRecord record : records) {
			// Only the first record is serialized once, the next records will always exceed the limit
			if (index == 0) {
				verify(mockSerializer).toBytes(record);
			} else {
				verify(mockSerializer, times(2)).toBytes(record);	
			}
				
			++index;
		}
		
	}
	
	@Test
	public void testIteratorWithLimitExceeded() {
		
		when(mockSerializer.toBytes(any())).thenReturn(StringUtils.repeat("a", recordSizeLimit).getBytes(StandardCharsets.UTF_8));
		
		TestRecord record = TestRecord.generateRecords(1).get(0);
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(Collections.singletonList(record), mockSerializer, recordSizeLimit);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			List<AwsKinesisRecord> result = new ArrayList<>();
			// Consume the whole iterator
			recordIterator.forEachRemaining(result::add);
		}).getMessage();
		
		assertEquals("A single record cannot exceed the limit of " + recordSizeLimit + " bytes.", errorMessage);
		
		verify(mockSerializer).toBytes(record);
	}
	
}
