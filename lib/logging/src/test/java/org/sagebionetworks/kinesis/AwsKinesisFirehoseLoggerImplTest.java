package org.sagebionetworks.kinesis;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;

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


	@Test
	public void testLogBatch(){
		Stream<AwsKinesisLogRecord> mockRecordStream = Stream.of(mockRecord1, mockRecord2);

		String kinesisStreamSuffix = "myKinesisStream";

		byte[] mockRecord1Bytes = new byte[1];
		byte[] mockRecord2Bytes = new byte[2];
		when(mockRecord1.toBytes()).thenReturn(mockRecord1Bytes);
		when(mockRecord2.toBytes()).thenReturn(mockRecord2Bytes);

		String stack = "dev";
		when(mockStackConfig.getStack()).thenReturn(stack);
		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		String instance = "test";
		when(mockStackConfig.getStackInstance()).thenReturn(instance);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordStream);

		verify(mockRecord1).withStack(stack);
		verify(mockRecord1).withInstance(instance);
		verify(mockRecord2).withStack(stack);
		verify(mockRecord2).withInstance(instance);

		verify(mockKinesisFirehoseClient).putRecordBatch(
				new PutRecordBatchRequest()
						.withDeliveryStreamName("devtestmyKinesisStream")
						.withRecords(Arrays.asList(
								new Record().withData(ByteBuffer.wrap(mockRecord1Bytes)),
								new Record().withData(ByteBuffer.wrap(mockRecord2Bytes))))
		);
	}
}