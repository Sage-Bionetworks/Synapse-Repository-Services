package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AwsKinesisRecordIteratorTest {
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	public void testIteratorWithNoRecords() {
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(Collections.emptyList(), OBJECT_MAPPER);
		
		List<AwsKinesisRecord> expected = Collections.emptyList();
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testIteratorWithSingleRecord() {
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(TestRecord.generateRecords(1), OBJECT_MAPPER);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(1, result.size());
	}
	
	@Test
	public void testIteratorWithMultipleRecordsUnderLimit() {
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(TestRecord.generateRecords(1000), OBJECT_MAPPER);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(1, result.size());
	}
	
	@Test
	public void testIteratorWithMultipleRecordsOverLimit() {
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(TestRecord.generateRecords(15000), OBJECT_MAPPER);
		
		List<AwsKinesisRecord> result = new ArrayList<>();
		
		// Consume the whole iterator
		recordIterator.forEachRemaining(result::add);
		
		assertEquals(2, result.size());
	}
	
	@Test
	public void testIteratorWithLimitExceeded() {
		
		TestRecord record = TestRecord.generateRecords(1).get(0).withSomeField(StringUtils.repeat("a", AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT));
		
		// Call under test
		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(Collections.singletonList(record), OBJECT_MAPPER);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			List<AwsKinesisRecord> result = new ArrayList<>();
			// Consume the whole iterator
			recordIterator.forEachRemaining(result::add);
		}).getMessage();
		
		assertEquals("A single record cannot exceed the limit of 1024000 bytes.", errorMessage);
	}
	
}
