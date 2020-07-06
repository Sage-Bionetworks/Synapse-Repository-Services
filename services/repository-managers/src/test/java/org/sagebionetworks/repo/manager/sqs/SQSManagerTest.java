package org.sagebionetworks.repo.manager.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.SQSSendMessageRequest;
import org.sagebionetworks.repo.model.message.SQSSendMessageResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageResult;

@ExtendWith(MockitoExtension.class)
public class SQSManagerTest {
	
	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private AmazonSQS mockSqsClient;
	
	@Mock
	private GetQueueUrlResult mockQueueUrlResult;
	
	@Mock
	private SendMessageResult mockSendMessageResult;
	
	@InjectMocks
	private SQSManagerImpl manager;
	
	private static final String QUEUE_NAME = "queue";
	private static final String STACK_QUEUE_NAME = "stack-queue";
	private static final String STACK_QUEUE_URL = "stack-queueUrl";
	private static final String MESSAGE_ID = "messageId";
	
	@Test
	public void testGetStackQueueName() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		// Call under test
		String result = manager.getStackQueueName(QUEUE_NAME);
		
		assertEquals(STACK_QUEUE_NAME, result);
		
		verify(mockConfig).getQueueName(QUEUE_NAME);
	}
	
	@Test
	public void testGetStackQueueUrl() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(STACK_QUEUE_URL);
		
		// Call under test
		String result = manager.getStackQueueUrl(QUEUE_NAME);
		
		assertEquals(STACK_QUEUE_URL, result);
		
		verify(mockConfig).getQueueName(QUEUE_NAME);
		verify(mockSqsClient).getQueueUrl(STACK_QUEUE_NAME);
	}
	
	@Test
	public void testGetStackQueueUrlWithNonExistingQueue() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		
		QueueDoesNotExistException expected = new QueueDoesNotExistException("Some error");
		
		doThrow(expected).when(mockSqsClient).getQueueUrl(anyString());
		
		NotFoundException ex = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.getStackQueueUrl(QUEUE_NAME);
		});
		
		assertEquals("The queue " + QUEUE_NAME + " does not exist", ex.getMessage());
		assertEquals(expected, ex.getCause());
		
	}
	
	@Test
	public void testGetStackQueueUrlWithSQSException() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		
		AmazonSQSException expected = new AmazonSQSException("Some error");
		
		doThrow(expected).when(mockSqsClient).getQueueUrl(anyString());
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getStackQueueUrl(QUEUE_NAME);
		});
		
		assertEquals(expected.getMessage(), ex.getMessage());
		assertEquals(expected, ex.getCause());
	}
	
	@Test
	public void testGetStackQueueUrlWithClientException() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		
		AmazonClientException expected = new AmazonClientException("Some error");
		
		doThrow(expected).when(mockSqsClient).getQueueUrl(anyString());
		
		TemporarilyUnavailableException ex = assertThrows(TemporarilyUnavailableException.class, () -> {			
			// Call under test
			manager.getStackQueueUrl(QUEUE_NAME);
		});
		
		assertEquals("Could not fetch the queue URL: Some error", ex.getMessage());
		assertEquals(expected, ex.getCause());
	}
	
	@Test
	public void testSendMessage() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(STACK_QUEUE_URL);
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);
		when(mockSendMessageResult.getMessageId()).thenReturn(MESSAGE_ID);
		when(mockSqsClient.sendMessage(any(), any())).thenReturn(mockSendMessageResult);

		SQSSendMessageResponse expected = new SQSSendMessageResponse();
		expected.setMessageId(MESSAGE_ID);
		
		SQSSendMessageRequest request = createRequest(QUEUE_NAME, "message body");
		// Call under test
		SQSSendMessageResponse response = manager.sendMessage(request);

		assertEquals(expected, response);
		
		verify(mockConfig).getQueueName(QUEUE_NAME);
		verify(mockSqsClient).getQueueUrl(STACK_QUEUE_NAME);
		verify(mockQueueUrlResult).getQueueUrl();
		verify(mockSqsClient).sendMessage(STACK_QUEUE_URL, "message body");
		verify(mockSendMessageResult).getMessageId();
	}
	
	@Test
	public void testSendMessageWithNullRequest() {
		SQSSendMessageRequest request = null;
		
		// Call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			manager.sendMessage(request);
		});
		
		assertEquals("messageRequest is required.", ex.getMessage());
	}
	
	@Test
	public void testSendMessageWithEmptyQueue() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest(null, "body"));
		});
		
		assertEquals("messageRequest.queueName is required and must not be the empty string.", ex.getMessage());
		
		ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest("", "body"));
		});
		
		assertEquals("messageRequest.queueName is required and must not be the empty string.", ex.getMessage());
		
		ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest(" ", "body"));
		});
		
		assertEquals("messageRequest.queueName is required and must not be a blank string.", ex.getMessage());
	}
	
	@Test
	public void testSendMessageWithEmptyBody() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest(QUEUE_NAME, null));
		});
		
		assertEquals("messageRequest.messageBody is required and must not be the empty string.", ex.getMessage());
		
		ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest(QUEUE_NAME, ""));
		});
		
		assertEquals("messageRequest.messageBody is required and must not be the empty string.", ex.getMessage());
		
		ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(createRequest(QUEUE_NAME, " "));
		});
		
		assertEquals("messageRequest.messageBody is required and must not be a blank string.", ex.getMessage());
	}
	
	@Test
	public void testSendMessageWithNonExistingQueueUrl() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(STACK_QUEUE_URL);
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);

		QueueDoesNotExistException expected = new QueueDoesNotExistException("Some error");
		
		doThrow(expected).when(mockSqsClient).sendMessage(any(), any());
		
		SQSSendMessageRequest request = createRequest(QUEUE_NAME, "body");
		
		NotFoundException ex = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			manager.sendMessage(request);
		});
		
		assertEquals("The queue referenced by " + STACK_QUEUE_URL + " does not exist", ex.getMessage());
		assertEquals(expected, ex.getCause());
	}
	
	@Test
	public void testSendMessageWithSQSException() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(STACK_QUEUE_URL);
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);

		AmazonSQSException expected = new AmazonSQSException("Some error");
		
		doThrow(expected).when(mockSqsClient).sendMessage(any(), any());
		
		SQSSendMessageRequest request = createRequest(QUEUE_NAME, "body");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.sendMessage(request);
		});
		
		assertEquals(expected.getMessage(), ex.getMessage());
		assertEquals(expected, ex.getCause());
	}
	
	@Test
	public void testSendMessageWithClientException() {
		when(mockConfig.getQueueName(any())).thenReturn(STACK_QUEUE_NAME);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(STACK_QUEUE_URL);
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);

		AmazonClientException expected = new AmazonClientException("Some error");
		
		doThrow(expected).when(mockSqsClient).sendMessage(any(), any());
		
		SQSSendMessageRequest request = createRequest(QUEUE_NAME, "body");
		
		TemporarilyUnavailableException ex = assertThrows(TemporarilyUnavailableException.class, () -> {			
			// Call under test
			manager.sendMessage(request);
		});
		
		assertEquals("Could not send message: Some error", ex.getMessage());
		assertEquals(expected, ex.getCause());
	}
	
	private SQSSendMessageRequest createRequest(String queue, String body) {
		SQSSendMessageRequest request = new SQSSendMessageRequest();
		
		request.setQueueName(queue);
		request.setMessageBody(body);
		
		return request;
	}

}
