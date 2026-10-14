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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.firehose.model.Record;

@ExtendWith(MockitoExtension.class)
public class AwsKinesisFirehoseBatchClientTest {

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private FirehoseClient mockKinesisFirehoseClient;

	@InjectMocks
	private AwsKinesisFirehoseBatchClientImpl kinesisFirehoseBatchClient;

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
		Record record = Record.builder().build();

		when(mockRecord.getRecord()).thenReturn(record);

		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);

		when(mockKinesisFirehoseClient.putRecordBatch(any(PutRecordBatchRequest.class))).thenReturn(
				PutRecordBatchResponse.builder().failedPutCount(0).build()
		);

		// method under test
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);

		verify(mockKinesisFirehoseClient).putRecordBatch(
				PutRecordBatchRequest.builder().deliveryStreamName(streamName).records(record).build()
		);
	}

	@Test
	public void testSendBatchWithNoStream() {
		streamName = null;

		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("The streamName is required.", errorMessage);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testSendBatchWithNoBatch() {
		List<AwsKinesisRecord> batch = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("The batch is required.", errorMessage);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testSendBatchWithEmptyBatch() {
		List<AwsKinesisRecord> batch = Collections.emptyList();

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("The batch size must be greater than 0 and and less or equal than 500", errorMessage);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testSendBatchWithTooBigBatch() {
		List<AwsKinesisRecord> batch = Collections.nCopies(AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT + 1, mockRecord);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("The batch size must be greater than 0 and and less or equal than 500", errorMessage);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testSendBatchWithTooBigRequest() {
		List<AwsKinesisRecord> batch = Collections.nCopies(AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT, mockRecord);

		when(mockRecord.size()).thenReturn(AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("The batch of records exceeds the maximum allowed request size of " + AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT, errorMessage);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testSendBatchWithRetryPartial() {
		Record blob1 = Record.builder().data(SdkBytes.fromByteArray("a".getBytes())).build();
		Record blob2 = Record.builder().data(SdkBytes.fromByteArray("b".getBytes())).build();

		when(mockRecord.getRecord()).thenReturn(blob1, blob2);

		List<AwsKinesisRecord> batch = Collections.nCopies(2, mockRecord);

		when(mockKinesisFirehoseClient.putRecordBatch(any(PutRecordBatchRequest.class))).thenReturn(
				// First time 1 failed
				PutRecordBatchResponse.builder()
						.failedPutCount(1)
						.requestResponses(
								PutRecordBatchResponseEntry.builder().build(),
								PutRecordBatchResponseEntry.builder().errorCode("error").errorMessage("error message").build()
						)
						.build(),
				// Second time got through
				PutRecordBatchResponse.builder().failedPutCount(0).build()
		);

		// method under test
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);

		verify(mockKinesisFirehoseClient, times(2)).putRecordBatch(requestCaptor.capture());

		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();

		PutRecordBatchRequest firstRequest = requests.get(0);

		// First request sent both records
		assertEquals(Arrays.asList(blob1, blob2), firstRequest.records());

		PutRecordBatchRequest secondRequest = requests.get(1);

		// The retry only sent the failed record
		assertEquals(Arrays.asList(blob2), secondRequest.records());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testLogBatchWithRetryMultiple() {
		Record blob1 = Record.builder().data(SdkBytes.fromByteArray("a".getBytes())).build();
		Record blob2 = Record.builder().data(SdkBytes.fromByteArray("b".getBytes())).build();
		Record blob3 = Record.builder().data(SdkBytes.fromByteArray("c".getBytes())).build();

		when(mockRecord.getRecord()).thenReturn(blob1, blob2, blob3);

		List<AwsKinesisRecord> batch = Collections.nCopies(3, mockRecord);

		when(mockKinesisFirehoseClient.putRecordBatch(any(PutRecordBatchRequest.class))).thenReturn(
				// First request, 2 error
				PutRecordBatchResponse.builder()
						.failedPutCount(2)
						.requestResponses(
								PutRecordBatchResponseEntry.builder().build(),
								PutRecordBatchResponseEntry.builder().errorCode("error").build(),
								PutRecordBatchResponseEntry.builder().errorCode("error").errorMessage("error message").build()
						)
						.build(),
				// Second request, 1 error
				PutRecordBatchResponse.builder()
						.failedPutCount(1)
						.requestResponses(
								PutRecordBatchResponseEntry.builder().errorCode("some other error").build(),
								PutRecordBatchResponseEntry.builder().build()
						)
						.build(),
				// Went through finally
				PutRecordBatchResponse.builder()
						.failedPutCount(0)
						.requestResponses(PutRecordBatchResponseEntry.builder().build())
						.build()
		);

		// method under test
		kinesisFirehoseBatchClient.sendBatch(streamName, batch);

		verify(mockKinesisFirehoseClient, times(3)).putRecordBatch(requestCaptor.capture());

		List<PutRecordBatchRequest> requests = requestCaptor.getAllValues();

		PutRecordBatchRequest firstRequest = requests.get(0);

		// First request sent all records
		assertEquals(Arrays.asList(blob1, blob2, blob3), firstRequest.records());

		PutRecordBatchRequest secondRequest = requests.get(1);

		// The first retry only sent 2 failed records
		assertEquals(Arrays.asList(blob2, blob3), secondRequest.records());

		PutRecordBatchRequest thirdRequest = requests.get(2);

		// The last retry only sent the last record
		assertEquals(Arrays.asList(blob2), thirdRequest.records());

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testLogBatchWithMaxRetryExceeded() {
		when(mockRecord.getRecord()).thenReturn(Record.builder().build());
		List<AwsKinesisRecord> batch = Collections.singletonList(mockRecord);

		when(mockKinesisFirehoseClient.putRecordBatch(any(PutRecordBatchRequest.class))).thenReturn(
				PutRecordBatchResponse.builder()
						.failedPutCount(1)
						.requestResponses(PutRecordBatchResponseEntry.builder().errorCode("Some error").build())
						.build()
		);

		String message = assertThrows(AwsKinesisDeliveryException.class, () -> {
			// method under test
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}).getMessage();

		assertEquals("Failed to deliver a batch of 1 kinesis records to stream someStream after 10 retries.", message);

		verify(mockKinesisFirehoseClient, times(10)).putRecordBatch(any(PutRecordBatchRequest.class));
	}

}
