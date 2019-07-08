package org.sagebionetworks.kinesis;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.StackConfiguration;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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


	String kinesisStreamSuffix = "myKinesisStream";
	String stack = "dev";
	String instance = "test";


	@BeforeEach
	public void setup(){
		when(mockStackConfig.getStack()).thenReturn(stack);
		when(mockRecord1.withStack(stack)).thenReturn(mockRecord1);
		when(mockRecord2.withStack(stack)).thenReturn(mockRecord2);
		when(mockStackConfig.getStackInstance()).thenReturn(instance);
	}


	@Test
	public void testLogBatch(){
		List<AwsKinesisLogRecord> mockRecordList = Lists.newArrayList(mockRecord1, mockRecord2);


		byte[] mockRecord1Bytes = new byte[1];
		byte[] mockRecord2Bytes = new byte[2];
		when(mockRecord1.toBytes()).thenReturn(mockRecord1Bytes);
		when(mockRecord2.toBytes()).thenReturn(mockRecord2Bytes);


		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, mockRecordList);

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

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}

	@Test
	public void testPartitioning(){
		List<AwsKinesisLogRecord> repeatList = Collections.nCopies(502, mockRecord1);

		byte[] mockRecord1Bytes = new byte[1];
		when(mockRecord1.toBytes()).thenReturn(mockRecord1Bytes);

		//method under test
		kinesisFirehoseLogger.logBatch(kinesisStreamSuffix, repeatList);


		//verify that 2 calls were made
		verify(mockKinesisFirehoseClient).putRecordBatch(
				new PutRecordBatchRequest()
						.withDeliveryStreamName("devtestmyKinesisStream")
						.withRecords(Collections.nCopies(500,
								new Record().withData(ByteBuffer.wrap(mockRecord1Bytes))))
		);

		verify(mockKinesisFirehoseClient).putRecordBatch(
				new PutRecordBatchRequest()
						.withDeliveryStreamName("devtestmyKinesisStream")
						.withRecords(Collections.nCopies(2,
								new Record().withData(ByteBuffer.wrap(mockRecord1Bytes))))
		);

		verifyNoMoreInteractions(mockKinesisFirehoseClient);
	}
}