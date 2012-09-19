package org.sagebionetworks.asynchronous.workers.sqs;

import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * Unit test for the MessageReceiverImpl.
 * 
 * @author John
 *
 */
public class MessageReceiverImplTest {
	
	MessageReceiverImpl messageReveiver;
	Integer maxNumberOfWorkerThreads = 5;
	Integer maxMessagePerWorker = 3;
	Integer visibilityTimeout = 5;
	String queueUrl = "queueUrl";
	MessageQueue mockQueue;
	MessageWorkerFactory stubFactory;
	AmazonSQSClient mockSQSClient;
	List<Message> messageList;
	ReceiveMessageResult results;
	
	@Before
	public void before(){
		mockSQSClient = Mockito.mock(AmazonSQSClient.class);
		mockQueue = Mockito.mock(MessageQueue.class);
		when(mockQueue.getQueueUrl()).thenReturn(queueUrl);
		// Inject all of the dependencies
		messageReveiver = new MessageReceiverImpl(mockSQSClient, maxNumberOfWorkerThreads, maxMessagePerWorker,visibilityTimeout, mockQueue, stubFactory);
		
		// Setup a list of messages.
		int maxMessages = maxNumberOfWorkerThreads*maxMessagePerWorker;
		messageList = new LinkedList<Message>();
		// We want fewer messages than the max
		for(int i=0; i<maxMessages-2; i++){
			messageList.add(new Message().withMessageId("id"+i).withReceiptHandle("handle1"+i));
		}
		// Setup the messages
		results = new ReceiveMessageResult();
		for(Message message: messageList){
			results.withMessages(message);
		}
		when(mockSQSClient.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(maxMessages).withVisibilityTimeout(visibilityTimeout))).thenReturn(results);

	}
	
	@Test (expected=IllegalStateException.class)
	public void testNullawsSQSClient() throws InterruptedException{
		messageReveiver.setAwsSQSClient(null);
		messageReveiver.triggerFired();
	}
	
	@Test (expected=IllegalStateException.class)
	public void testNullmaxNumberOfWorkerThreads() throws InterruptedException{
		messageReveiver.setMaxNumberOfWorkerThreads(null);
		messageReveiver.triggerFired();
	}
	
	@Test (expected=IllegalStateException.class)
	public void testNullVisibilityTimeout() throws InterruptedException{
		messageReveiver.setVisibilityTimeoutSec(null);
		messageReveiver.triggerFired();
	}
	
	@Test (expected=IllegalStateException.class)
	public void testNullMessageQueue() throws InterruptedException{
		messageReveiver.setMessageQueue(null);
		messageReveiver.triggerFired();
	}
	
	@Test
	public void testTrigerFiredMultipleMessagesSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for(int i=0; i<maxNumberOfWorkerThreads; i++){
			workerStack.push(new StubWorker(0, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		List<DeleteMessageBatchRequestEntry> deleteRequest = new LinkedList<DeleteMessageBatchRequestEntry>();
		// We all messages should get deleted.
		for(Message message: messageList){
			deleteRequest.add(new DeleteMessageBatchRequestEntry().withId(message.getMessageId()).withReceiptHandle(message.getReceiptHandle()));
		}
		DeleteMessageBatchRequest expectedBatch = new DeleteMessageBatchRequest(queueUrl, deleteRequest);
		// Verify that all were deleted
		verify(mockSQSClient, times(1)).deleteMessageBatch(expectedBatch);
	}
	@Test
	public void testTrigerFiredOneFailureMulitipleSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for(int i=0; i<maxNumberOfWorkerThreads-1; i++){
			workerStack.push(new StubWorker(0, null));
		}
		// Make the last worker throw an exception
		workerStack.push(new StubWorker(1000, new Exception("Simulated a failure")));
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		List<DeleteMessageBatchRequestEntry> deleteRequest = new LinkedList<DeleteMessageBatchRequestEntry>();
		// We all messages should get deleted.
		for(Message message: messageList){
			deleteRequest.add(new DeleteMessageBatchRequestEntry().withId(message.getMessageId()).withReceiptHandle(message.getReceiptHandle()));
		}
		// This time the first batch of messages should not get delete because of the failure
		for(int i=0; i<maxMessagePerWorker; i++){
			deleteRequest.remove(0);
		}
		DeleteMessageBatchRequest expectedBatch = new DeleteMessageBatchRequest(queueUrl, deleteRequest);
		// Verify that all were deleted
		verify(mockSQSClient, times(1)).deleteMessageBatch(expectedBatch);
	}
	
	@Test
	public void testTrigerFiredOneTimoutMulitipleSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for(int i=0; i<maxNumberOfWorkerThreads-1; i++){
			workerStack.push(new StubWorker(0, null));
		}
		// Make the first worker timeout
		workerStack.push(new StubWorker(visibilityTimeout*1000+100, null));
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		List<DeleteMessageBatchRequestEntry> deleteRequest = new LinkedList<DeleteMessageBatchRequestEntry>();
		// We all messages should get deleted.
		for(Message message: messageList){
			deleteRequest.add(new DeleteMessageBatchRequestEntry().withId(message.getMessageId()).withReceiptHandle(message.getReceiptHandle()));
		}
		// This time the first batch of messages should not get delete because of the timeout
		for(int i=0; i<maxMessagePerWorker; i++){
			deleteRequest.remove(0);
		}
		DeleteMessageBatchRequest expectedBatch = new DeleteMessageBatchRequest(queueUrl, deleteRequest);
		// Verify that all were deleted
		verify(mockSQSClient, times(1)).deleteMessageBatch(expectedBatch);
	}
	
	@Test
	public void testTrigerFiredStaggeredSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		long sleepTime = 0;
		for(int i=0; i<maxNumberOfWorkerThreads; i++){
			workerStack.push(new StubWorker(sleepTime+=500, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		// Since each worker has a different sleep time, the delete messages should be staggered over 5 calls.
		verify(mockSQSClient, times(maxNumberOfWorkerThreads)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}

}
