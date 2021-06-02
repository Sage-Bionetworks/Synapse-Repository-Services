package org.sagebionetworks.kinesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
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

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

@ExtendWith(MockitoExtension.class)
public class AwsKinesisFirehoseBatchClientTest {
	
	@Mock
	private StackConfiguration mockStackConfig;
	
	@Mock
	private AmazonKinesisFirehose mockKinesisFirehoseClient;

	@InjectMocks
	private AwsKinesisFirehoseBatchClientImpl kinesisFirehoseBatchClient;

	@Mock
	private PutRecordBatchResult mockRecordResult;
	
	@Mock
	private AwsKinesisRecord mockRecord;

	@Captor
	private ArgumentCaptor<PutRecordBatchRequest> requestCaptor;

	private String streamName;
	private long maxRetryDelay = 1;
	
	@BeforeEach
	public void before() {
		
		streamName = "someStream";
		when(mockStackConfig.getKinesisMaxRetryDelay()).thenReturn(maxRetryDelay);
		
		kinesisFirehoseBatchClient.configure(mockStackConfig);
	}

	@Test
	public void testSendBatch() {
		
		Record record = new Record();
		
		when(mockRecord.getRecord()).thenReturn(record);
		
		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);
		
		when(mockRecordResult.getFailedPutCount()).thenReturn(0);
		
		//method under test
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		
		verify(mockKinesisFirehoseClient).putRecordBatch(new PutRecordBatchRequest()
				.withDeliveryStreamName(streamName)
				.withRecords(record)
		);
	}
	
	@Test
	public void testSendBatchWithNoStream() {
		
		streamName = null;
		
		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("The streamName is required.", errorMessage);
		
		verifyZeroInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testSendBatchWithNoBatch() {
		
		List<AwsKinesisRecord> batch = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("The batch is required.", errorMessage);
		
		verifyZeroInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testSendBatchWithEmptyBatch() {
		
		List<AwsKinesisRecord> batch = Collections.emptyList();
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("The batch size must be greater than 0 and and less or equal than 500", errorMessage);
		
		verifyZeroInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testSendBatchWithTooBigBatch() {
		
		List<AwsKinesisRecord> batch = Collections.nCopies(AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT + 1, mockRecord);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("The batch size must be greater than 0 and and less or equal than 500", errorMessage);
		
		verifyZeroInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testSendBatchWithTooBigRequest() {
		
		List<AwsKinesisRecord> batch = Collections.nCopies(AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT, mockRecord);
		
		when(mockRecord.size()).thenReturn(AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("The batch of records exceeds the maximum allowed request size of " + AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT, errorMessage);
		
		verifyZeroInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testSendBatchWithRetryPartial(){
		
		Record blob1 = new Record().withData(ByteBuffer.wrap("a".getBytes()));
		Record blob2 = new Record().withData(ByteBuffer.wrap("b".getBytes()));
		
		when(mockRecord.getRecord()).thenReturn(blob1, blob2);
		
		List<AwsKinesisRecord> batch = Collections.nCopies(2, mockRecord);
		
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(mockRecordResult);
		
		// First time 1 failed, second time got through
		when(mockRecordResult.getFailedPutCount()).thenReturn(1, 0);
		
		when(mockRecordResult.getRequestResponses()).thenReturn(Arrays.asList(
				new PutRecordBatchResponseEntry(), // first suceeded
				new PutRecordBatchResponseEntry().withErrorCode("error").withErrorMessage("error message")
		));

		//method under test
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);

		verify(mockKinesisFirehoseClient, times(2)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		PutRecordBatchRequest firstRequest = requests.get(0);
		
		// First request sent both records
		assertEquals(Arrays.asList(blob1, blob2), firstRequest.getRecords());
		
		PutRecordBatchRequest secondRequest = requests.get(1);
		
		// The retry only sent the failed record
		assertEquals(Arrays.asList(blob2), secondRequest.getRecords());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithRetryMultiple(){
		Record blob1 = new Record().withData(ByteBuffer.wrap("a".getBytes()));
		Record blob2 = new Record().withData(ByteBuffer.wrap("b".getBytes()));
		Record blob3 = new Record().withData(ByteBuffer.wrap("c".getBytes()));
		
		when(mockRecord.getRecord()).thenReturn(blob1, blob2, blob3);
		
		List<AwsKinesisRecord> batch = Collections.nCopies(3, mockRecord);
		
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
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);

		verify(mockKinesisFirehoseClient, times(3)).putRecordBatch(requestCaptor.capture());
		
		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();
		
		PutRecordBatchRequest firstRequest = requests.get(0);
		
		// First request sent all records
		assertEquals(Arrays.asList(blob1, blob2, blob3), firstRequest.getRecords());
		
		PutRecordBatchRequest secondRequest = requests.get(1);

		// The first retry only sent 2 failed records
		assertEquals(Arrays.asList(blob2, blob3), secondRequest.getRecords());
		
		PutRecordBatchRequest thirdRequest = requests.get(2);
		
		// The last retry only sent the last record
		assertEquals(Arrays.asList(blob2), thirdRequest.getRecords());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
	
	@Test
	public void testLogBatchWithMaxRetryExceeded(){
		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);
		
		// Always fail
		when(mockKinesisFirehoseClient.putRecordBatch(any())).thenReturn(
				new PutRecordBatchResult().withFailedPutCount(1).withRequestResponses(
						new PutRecordBatchResponseEntry().withErrorCode("Some error")
				)
		);

		String message = assertThrows(AwsKinesisDeliveryException.class, () -> {			
			//method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();
		
		assertEquals("Failed to deliver a batch of 1 kinesis records to stream someStream after 10 retries.", message);

		verify(mockKinesisFirehoseClient, times(10)).putRecordBatch(any());
		
	}
	
}
