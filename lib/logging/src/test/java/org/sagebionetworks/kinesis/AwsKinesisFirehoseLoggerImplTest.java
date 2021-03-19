package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
class AwsKinesisFirehoseLoggerImplTest {

	@Mock
	StackConfiguration mockStackConfig;

	@Mock
	AmazonKinesisFirehoseClient mockKinesisFirehoseClient;

	@InjectMocks
	AwsKinesisFirehoseLoggerImpl kinesisFirehoseLogger;

	@Mock
	AwsKinesisLogRecord mockRecord1;

	@Mock
	AwsKinesisLogRecord mockRecord2;
	
	@Mock
	PutRecordBatchResult mockRecordResult;
	
	@Mock
	AwsKinesisLogRecordSerializer mockRecordSerializer;
	
	@Captor
	ArgumentCaptor<PutRecordBatchRequest> requestCaptor;


	String kinesisStreamSuffix = "myKinesisStream";
	String stack = "dev";
	String instance = "test";


	@BeforeEach
	public void setup(){
		when(mockStackConfig.getStack()).thenReturn(stack);
		when(mockStackConfig.getStackInstance()).thenReturn(instance);
		// Small delay so the test goes fast
		when(mockStackConfig.getKinesisMaxRetryDelay()).thenReturn(10L);
		
		kinesisFirehoseLogger.configure(mockStackConfig);
	}


	@Test
	public void testLogBatch(){
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);

		byte[] mockRecord1Bytes = new byte[1];
		byte[] mockRecord2Bytes = new byte[2];
		
		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		when(mockRecordSerializer.toBytes(mockRecord1)).thenReturn(mockRecord1Bytes);
		when(mockRecordSerializer.toBytes(mockRecord2)).thenReturn(mockRecord2Bytes);
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockRecord1).withStack(stack);
		verify(mockRecord1).withInstance(instance);
		verify(mockRecord2).withStack(stack);
		verify(mockRecord2).withInstance(instance);

		verify(mockKinesisFirehoseClient).putRecordBatch(requestCaptor.capture());
		
		PutRecordBatchRequest request = requestCaptor.getValue();
		
		assertEquals("devtestmyKinesisStream", request.getDeliveryStreamName());
		assertEquals(1, request.getRecords().size());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
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
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockRecord1).withStack(stack);
		verify(mockRecord1).withInstance(instance);
		
		verify(mockRecord2).withStack(stack);
		verify(mockRecord2).withInstance(instance);

		verify(mockKinesisFirehoseClient).putRecordBatch(requestCaptor.capture());
		
		PutRecordBatchRequest request = requestCaptor.getValue();
		
		assertEquals("devtestmyKinesisStream", request.getDeliveryStreamName());
		assertEquals(2, request.getRecords().size());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithMultipleBatches() {
		// First 4 fit in the first batch request, the last one needs to end up in a new batch request
		List<AwsKinesisLogRecord> mockRecordList = Collections.nCopies(5, mockRecord1);

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		
		// Max size for each record
		byte[] mockRecordBytes = new byte[AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length];
		
		when(mockRecordSerializer.toBytes(any())).thenReturn(mockRecordBytes);
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockKinesisFirehoseClient, times(2)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		requests.forEach( r-> {
			assertEquals("devtestmyKinesisStream", r.getDeliveryStreamName());
		});
		
		assertEquals(4, requests.get(0).getRecords().size());
		assertEquals(1, requests.get(1).getRecords().size());
		
		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithRetry(){
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);

		byte[] mockRecord1Bytes = new byte[1];
		byte[] mockRecord2Bytes = new byte[2];
		
		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		when(mockRecordSerializer.toBytes(mockRecord1)).thenReturn(mockRecord1Bytes);
		when(mockRecordSerializer.toBytes(mockRecord2)).thenReturn(mockRecord2Bytes);
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);
		
		// First time all failed, second time got through
		when(mockRecordResult.getFailedPutCount()).thenReturn(1, 0);
		
		when(mockRecordResult.getRequestResponses()).thenReturn(Arrays.asList(
				new PutRecordBatchResponseEntry().withErrorCode("error").withErrorMessage("error message")
		));

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockKinesisFirehoseClient, times(2)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		assertEquals(1, requests.get(0).getRecords().size());
		assertEquals(requests.get(0), requests.get(1));

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithRetryPartial(){
		List<AwsKinesisLogRecord> mockRecordList = Collections.nCopies(2, mockRecord1);

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		
		// Max size for each record
		byte[] mockRecordBytes = new byte[AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length];
		
		when(mockRecordSerializer.toBytes(any())).thenReturn(mockRecordBytes);
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);
		
		// First time 1 failed, second time got through
		when(mockRecordResult.getFailedPutCount()).thenReturn(1, 0);
		
		when(mockRecordResult.getRequestResponses()).thenReturn(Arrays.asList(
				new PutRecordBatchResponseEntry(), // first suceeded
				new PutRecordBatchResponseEntry().withErrorCode("error").withErrorMessage("error message")
		));

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockKinesisFirehoseClient, times(2)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		// First request sent both records
		assertEquals(2, requests.get(0).getRecords().size());
		// The retry only sent the failed record
		assertEquals(Arrays.asList(requests.get(0).getRecords().get(1)), requests.get(1).getRecords());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithRetryMultiple(){
		List<AwsKinesisLogRecord> mockRecordList = Collections.nCopies(3, mockRecord1);

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		
		// Max size for each record
		byte[] mockRecordBytes = new byte[AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.NEW_LINE_BYTES.length];
		
		when(mockRecordSerializer.toBytes(any())).thenReturn(mockRecordBytes);
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(
				// First request, 2 error
				new PutRecordBatchResult().withFailedPutCount(2).withRequestResponses(
						new PutRecordBatchResponseEntry(),
						new PutRecordBatchResponseEntry().withErrorCode("error"),
						new PutRecordBatchResponseEntry().withErrorCode("error").withErrorMessage("error message")
				),
				// Second request, 1 error
				new PutRecordBatchResult().withFailedPutCount(1).withRequestResponses(
						new PutRecordBatchResponseEntry().withErrorCode("some other error"),
						new PutRecordBatchResponseEntry()
				),
				// Went through finally
				new PutRecordBatchResult().withFailedPutCount(0).withRequestResponses(
						new PutRecordBatchResponseEntry()
				)
		);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

		verify(mockKinesisFirehoseClient, times(3)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		PutRecordBatchRequest firstRequest = requests.get(0);
		
		// First request sent all records
		assertEquals(3, firstRequest.getRecords().size());
		
		PutRecordBatchRequest secondRequest = requests.get(1);
		
		assertEquals(2, secondRequest.getRecords().size());
		
		// The first retry only sent 2 failed records
		assertEquals(Arrays.asList(firstRequest.getRecords().get(1), firstRequest.getRecords().get(2)), secondRequest.getRecords());
		
		PutRecordBatchRequest thirdRequest = requests.get(2);
		
		// The last retry only sent the last record
		assertEquals(Arrays.asList(secondRequest.getRecords().get(0)), thirdRequest.getRecords());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithMaxRetryExceeded(){
		List<AwsKinesisLogRecord> mockRecordList = Collections.singletonList(mockRecord1);

		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		
		byte[] mockRecordBytes = new byte[1];
		
		when(mockRecordSerializer.toBytes(any())).thenReturn(mockRecordBytes);
		
		// Always fail
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(
				new PutRecordBatchResult().withFailedPutCount(1).withRequestResponses(
						new PutRecordBatchResponseEntry().withErrorCode("Some error")
				)
		);

		String message = assertThrows(AwsKinesisDeliveryException.class, () -> {			
			//method under test
			kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);
		}).getMessage();
		
		assertEquals("Failed to deliver a batch of 1 kinesis records to stream devtestmyKinesisStream after 10 retries.", message);

		verify(mockKinesisFirehoseClient, times(10)).putRecordBatch(requestCaptor.capture());
		
	}
}