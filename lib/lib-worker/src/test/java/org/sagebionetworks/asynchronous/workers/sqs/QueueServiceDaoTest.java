package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class QueueServiceDaoTest {
	
	AmazonSQS mockSQSClient;
	QueueServiceDaoImpl queueServiceDao;
	int maxRequestSize;
	int messageIdSequence;
	LinkedList<Message> messageQueue;
	
	@Before
	public void before(){
		mockSQSClient = Mockito.mock(AmazonSQS.class);
		queueServiceDao = new QueueServiceDaoImpl();
		maxRequestSize = 2;
		ReflectionTestUtils.setField(queueServiceDao, "AmazonSQS", mockSQSClient);
		ReflectionTestUtils.setField(queueServiceDao, "maxSQSRequestSize", maxRequestSize);
		messageIdSequence = 0;
		// Create messages for the queue
		messageQueue = new LinkedList<Message>();
		for(int i=0; i<10; i++){
			messageQueue.push(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
		}
		when(mockSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(new Answer<ReceiveMessageResult>() {

			@Override
			public ReceiveMessageResult answer(InvocationOnMock invocation)
					throws Throwable {
				ReceiveMessageRequest request= (ReceiveMessageRequest) invocation.getArguments()[0];
				if(request.getMaxNumberOfMessages()> maxRequestSize) throw new IllegalArgumentException("Maximum number of messages exceeded");
				List<Message> messages= new ArrayList<Message>();
				for(int i=0; i<request.getMaxNumberOfMessages(); i++){
					Message message = messageQueue.poll();
					if(message == null){
						// queue is now empty
						break;
					}
					messages.add(message);
				}
				ReceiveMessageResult results = new ReceiveMessageResult().withMessages(messages);
				return results;
			}
		});
	}
	
	
	@Test
	public void testReceiveMessagesBatch(){
		String url = "url";
		int visibiltyTimeout = 100;
		List<Message> list = queueServiceDao.receiveMessages(url, 100, 5);
		assertNotNull(list);
		assertEquals(5, list.size());
		// It should take a total three calls to get all five messages.  The first two calls get two messages, while the last call gets one.
		verify(mockSQSClient, times(2)).receiveMessage(new ReceiveMessageRequest(url).withVisibilityTimeout(visibiltyTimeout).withMaxNumberOfMessages(2));
		verify(mockSQSClient, times(1)).receiveMessage(new ReceiveMessageRequest(url).withVisibilityTimeout(visibiltyTimeout).withMaxNumberOfMessages(1));
	}

	@Test
	public void testReceiveMessagesBatchAll(){
		String url = "url";
		int visibiltyTimeout = 100;
		// get more messages than are in the queue.
		int startQueueSize =  messageQueue.size();
		List<Message> list = queueServiceDao.receiveMessages(url, 100,startQueueSize+1);
		assertNotNull(list);
		// The queue should be empty
		assertEquals(0, messageQueue.size());
		assertEquals("All messages on the queue should have been fetched",startQueueSize, list.size());
	}
	
	@Test
	public void testDeleteMessages(){
		List<Message> toDelete = new ArrayList<Message>(5);
		for(int i=0; i<5; i++){
			toDelete.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
		}
		String url = "url";
		queueServiceDao.deleteMessages(url, toDelete);
		// It should talke three calls to delete all three.
		verify(mockSQSClient, times(3)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}
	@Test
	public void testDeleteMessagesDuplicate(){
		List<Message> toDelete = new ArrayList<Message>(5);
		for(int i=0; i<5; i++){
			toDelete.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
			// Add a duplicate for each message.
			toDelete.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
		}
		String url = "url";
		queueServiceDao.deleteMessages(url, toDelete);
		// It should talke three calls to delete all three.
		verify(mockSQSClient, times(3)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}
	
	@Test
	public void testResetMessageVisibility(){
		List<Message> toReset = new ArrayList<Message>(5);
		for(int i=0; i<5; i++){
			toReset.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
		}
		String url = "url";
		queueServiceDao.resetMessageVisibility(url, 99, toReset);
		// It should take three calls to change the visibility of all five messages.
		verify(mockSQSClient, times(3)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));
	}
	
	@Test
	public void testResetMessageVisibilityDuplicate(){
		List<Message> toReset = new ArrayList<Message>(5);
		for(int i=0; i<5; i++){
			toReset.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
			// Add a duplicate for each message.
			toReset.add(new Message().withMessageId("id"+i).withReceiptHandle("handle"+i));
		}
		String url = "url";
		queueServiceDao.resetMessageVisibility(url, 99, toReset);
		// It should take three calls to change the visibility of all five messages.
		verify(mockSQSClient, times(3)).changeMessageVisibilityBatch(any(ChangeMessageVisibilityBatchRequest.class));
	}
}
