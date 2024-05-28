package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

@ExtendWith(MockitoExtension.class)
public class MessageQueueImplTest {

	@Mock
	AmazonSQSClient mockSQSClient;

	String queueUrl;
	MessageQueueConfiguration config;

	@BeforeEach
	public void setUp() throws Exception {
		queueUrl = "queueURL";

		// config
		config = new MessageQueueConfiguration();
		config.setQueueName("queueName");
		config.setEnabled(true);

		when(mockSQSClient.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
	}

	@Test
	public void testConstructor() {
		config = new MessageQueueConfiguration();
		String queueName = "queueName";
		config.setQueueName(queueName);
		config.setEnabled(true);
		MessageQueueImpl msgQImpl = new MessageQueueImpl(mockSQSClient, config);
		assertEquals(queueUrl, msgQImpl.getQueueUrl());
		verify(mockSQSClient).getQueueUrl(queueName);
		verifyNoMoreInteractions(mockSQSClient);
	}
}
