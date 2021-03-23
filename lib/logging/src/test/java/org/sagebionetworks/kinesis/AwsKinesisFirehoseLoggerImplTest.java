package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
class AwsKinesisFirehoseLoggerImplTest {

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private AwsKinesisFirehoseBatchClient mockKinesisFirehoseBatchClient;

	@InjectMocks
	private AwsKinesisFirehoseLoggerImpl kinesisFirehoseLogger;

	@Mock
	private AwsKinesisLogRecordSerializer mockRecordSerializer;
	
	@Mock
	private AwsKinesisLogRecord mockRecord1;

	@Mock
	private AwsKinesisLogRecord mockRecord2;

	@Captor
	private ArgumentCaptor<List<AwsKinesisRecord>> batchCaptor;

	private String kinesisStreamSuffix;
	private String stack;
	private String instance;

	@BeforeEach
	public void setup() {
		kinesisStreamSuffix = "myKinesisStream";
		stack = "dev";
		instance = "test";

		when(mockStackConfig.getStack()).thenReturn(stack);
		when(mockStackConfig.getStackInstance()).thenReturn(instance);

		kinesisFirehoseLogger.configure(mockStackConfig);
	}

	@Test
	public void testLogBatchWithNoStream() {

		kinesisStreamSuffix = null;
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);
		}).getMessage();

		assertEquals("The kinesisDataStreamSuffix is required.", errorMessage);

		verifyZeroInteractions(mockKinesisFirehoseBatchClient);
	}

	@Test
	public void testLogBatchWithNoRecords() {

		List<AwsKinesisLogRecord> mockRecordList = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);
		}).getMessage();

		assertEquals("The logRecords is required.", errorMessage);

		verifyZeroInteractions(mockKinesisFirehoseBatchClient);
	}

	@Test
	public void testLogBatchWithEmptyRecords() {

		List<AwsKinesisLogRecord> mockRecordList = Collections.emptyList();

		// method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verifyZeroInteractions(mockKinesisFirehoseBatchClient);
	}

	@Test
	public void testLogBatch() {
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);

		byte[] mockRecord1Bytes = new byte[1];
		byte[] mockRecord2Bytes = new byte[2];

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		when(mockRecordSerializer.toBytes(mockRecord1)).thenReturn(mockRecord1Bytes);
		when(mockRecordSerializer.toBytes(mockRecord2)).thenReturn(mockRecord2Bytes);

		// method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockRecord1).withStack(stack);
		verify(mockRecord1).withInstance(instance);
		verify(mockRecord2).withStack(stack);
		verify(mockRecord2).withInstance(instance);

		verify(mockKinesisFirehoseBatchClient).sendBatch(eq("devtestmyKinesisStream"), batchCaptor.capture());

		List<AwsKinesisRecord> batch = batchCaptor.getValue();

		assertEquals(1, batch.size());

		verifyNoMoreInteractions(mockKinesisFirehoseBatchClient);
	}

	@Test
	public void testLogBatchWithMultipleRecords() {
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);

		byte[] mockRecord1Bytes = new byte[AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length];
		byte[] mockRecord2Bytes = new byte[2];

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		when(mockRecordSerializer.toBytes(mockRecord1)).thenReturn(mockRecord1Bytes);
		when(mockRecordSerializer.toBytes(mockRecord2)).thenReturn(mockRecord2Bytes);

		// method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockRecord1).withStack(stack);
		verify(mockRecord1).withInstance(instance);

		verify(mockRecord2).withStack(stack);
		verify(mockRecord2).withInstance(instance);

		verify(mockKinesisFirehoseBatchClient).sendBatch(eq("devtestmyKinesisStream"), batchCaptor.capture());

		List<AwsKinesisRecord> batch = batchCaptor.getValue();

		assertEquals(2, batch.size());

		verifyNoMoreInteractions(mockKinesisFirehoseBatchClient);
	}

	@Test
	public void testLogBatchWithMultipleBatches() {
		// First 4 fit in the first batch request, the last one needs to end up in a new batch request
		List<AwsKinesisLogRecord> mockRecordList = Collections.nCopies(5, mockRecord1);

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);

		// Max size for each record
		byte[] mockRecordBytes = new byte[AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length];

		when(mockRecordSerializer.toBytes(any())).thenReturn(mockRecordBytes);

		// method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockKinesisFirehoseBatchClient, times(2)).sendBatch(eq("devtestmyKinesisStream"), batchCaptor.capture());

		List<List<AwsKinesisRecord>> batches = batchCaptor.getAllValues();

		assertEquals(4, batches.get(0).size());
		assertEquals(1, batches.get(1).size());

		verifyNoMoreInteractions(mockKinesisFirehoseBatchClient);
	}

}