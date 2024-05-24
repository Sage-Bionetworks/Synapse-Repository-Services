package org.sagebionetworks.workers.util.aws.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@ExtendWith(MockitoExtension.class)
public class QueueCleanerTest {

	@Mock
	private AmazonSQSClient mockAmazonSQSClient;
	@InjectMocks
	private QueueCleaner queueCleaner;

	@BeforeEach
	public void before() {
		when(mockAmazonSQSClient.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("aQueueUrl"));
	}

	@Test
	public void testAttemptToEmptyQueue() {
		// Simulate two batches to delete
		ReceiveMessageResult pageOne = new ReceiveMessageResult();
		pageOne.setMessages(Arrays.asList(new Message().withMessageId("id1")
				.withReceiptHandle("h1"), new Message().withMessageId("id2")
				.withReceiptHandle("h2")));
		ReceiveMessageResult pageTwo = new ReceiveMessageResult();
		pageTwo.setMessages(Arrays.asList(new Message().withMessageId("id3")
				.withReceiptHandle("h3")));
		// page three is empty.
		ReceiveMessageResult pageThree = new ReceiveMessageResult();
		pageThree.setMessages(new LinkedList<Message>());
		
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(pageOne, pageTwo, pageThree);
		// Simulate two batches to delete
		queueCleaner.purgeQueue("someQueue");
		// each page should be deleted as a batch.
		verify(mockAmazonSQSClient, times(2)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}
	
	@Test
	public void testQueueDoesNotExist(){
		when(mockAmazonSQSClient.getQueueUrl(anyString())).thenThrow(new QueueDoesNotExistException("Not found"));
		// should not fail.
		queueCleaner.purgeQueue("someQueue");
	}
}
