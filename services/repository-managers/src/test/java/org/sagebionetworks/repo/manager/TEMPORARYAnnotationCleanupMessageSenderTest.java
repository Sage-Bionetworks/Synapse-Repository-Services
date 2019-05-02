package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;

@ExtendWith(MockitoExtension.class)
class TEMPORARYAnnotationCleanupMessageSenderTest {

	@Mock
	AmazonSQSClient mockSqsClient;

	@Mock
	StackConfiguration mockStackConfiguration;

	@InjectMocks
	TEMPORARYAnnotationCleanupMessageSender sender;

	final String stackQueueName = "STACK-QUEUE-NAME";
	final String queueURL = "queue.url";

	List<Long> nodeIds;


	@BeforeEach
	void setUp(){
		when(mockStackConfiguration.getQueueName(anyString())).thenReturn(stackQueueName);
		when(mockSqsClient.createQueue(stackQueueName)).thenReturn(new CreateQueueResult().withQueueUrl(queueURL));

		nodeIds = Arrays.asList(123L, 456L);

		sender.init();
	}

	@Test
	void sendMessages_singleBatch() {

		sender.sendMessages(nodeIds);

		verify(mockSqsClient).sendMessage(expectedMessage("123\n456"));
		verifyNoMoreInteractions(mockSqsClient);
	}


	@Test
	void init() {
		sender.init();
		assertEquals(queueURL, sender.queueUrl);
	}

	@Test
	void sendMessages_MultipleMessageBatches() throws NoSuchFieldException, IllegalAccessException {
		//change max value
		sender.setMaxBytesPerBatch(5);

		sender.sendMessages(nodeIds);

		verify(mockSqsClient).sendMessage(expectedMessage("123"));
		verify(mockSqsClient).sendMessage(expectedMessage("456"));

		verifyNoMoreInteractions(mockSqsClient);

		sender.setMaxBytesPerBatch(TEMPORARYAnnotationCleanupMessageSender.SQS_MESSAGE_MAX_BYTES);
	}


	SendMessageRequest expectedMessage(String message){
		return new SendMessageRequest().withQueueUrl(queueURL).withMessageBody(message).withDelaySeconds(0);
	}
}