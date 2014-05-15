package org.sagebionetworks.asynchronous.workers.sqs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.sqs.model.Message;

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
	Integer visibilityTimeoutSecs = 5;
	String queueUrl = "queueUrl";
	MessageQueue mockQueue;
	MessageWorkerFactory stubFactory;
	QueueServiceDao mockSQSDao;
	List<Message> messageList;
	Queue<Message> messageQueue;
	
	@Before
	public void before(){
		mockSQSDao = Mockito.mock(QueueServiceDao.class);
		mockQueue = Mockito.mock(MessageQueue.class);
		when(mockQueue.getQueueUrl()).thenReturn(queueUrl);
		when(mockQueue.isEnabled()).thenReturn(true);
		// Inject all of the dependencies
		messageReveiver = new MessageReceiverImpl(mockSQSDao, maxNumberOfWorkerThreads, maxMessagePerWorker,visibilityTimeoutSecs, mockQueue, stubFactory);
		
		// Setup a list of messages.
		int maxMessages = maxNumberOfWorkerThreads*maxMessagePerWorker;
		messageList = new LinkedList<Message>();
		// We want fewer messages than the max
		for(int i=0; i<maxMessages-2; i++){
			messageList.add(new Message().withMessageId("id"+i).withReceiptHandle("handle1"+i));
		}
		// Setup the messages
		messageQueue = new LinkedList<Message>(messageList);
		when(mockSQSDao.receiveMessages(any(String.class), anyInt(), anyInt())).thenAnswer(new Answer<List<Message>>() {

			@Override
			public List<Message> answer(InvocationOnMock invocation)
					throws Throwable {
				String url = (String) invocation.getArguments()[0];
				Integer visibilityTimeout = (Integer) invocation.getArguments()[1];
				Integer maxReqeusts = (Integer) invocation.getArguments()[2];
				List<Message> results = new LinkedList<Message>();
				for (int i = 0; i < maxReqeusts && !messageQueue.isEmpty(); i++) {
					results.add(messageQueue.poll());
				}
				return results;
			}
			
		});
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
			workerStack.push(new StubWorker(0, 0, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		verify(mockSQSDao, times(1)).deleteMessages(queueUrl, messageList);
	}
	@Test
	public void testTrigerFiredOneFailureMulitipleSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for(int i=0; i<maxNumberOfWorkerThreads-1; i++){
			workerStack.push(new StubWorker(0, 0, null));
		}
		// Make the last worker throw an exception
		workerStack.push(new StubWorker(1000, 0, new Exception("Simulated a failure")));
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		List<Message> deleteRequest = new LinkedList<Message>(messageList);
		// This time the first batch of messages should not get delete because of the failure
		for(int i=0; i<maxMessagePerWorker; i++){
			deleteRequest.remove(0);
		}
		// Verify that all were deleted
		verify(mockSQSDao, times(1)).deleteMessages(queueUrl, deleteRequest);
	}
	
	@Test
	public void testTrigerFiredOneTimoutMulitipleSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for(int i=0; i<maxNumberOfWorkerThreads-1; i++){
			workerStack.push(new StubWorker(0, 0, null));
		}
		// Make the first worker timeout
		workerStack.push(new StubWorker(visibilityTimeoutSecs*1000+100, 0, null));
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		List<Message> deleteRequest = new LinkedList<Message>(messageList);
		// This time the first batch of messages should not get delete because of the timeout
		for(int i=0; i<maxMessagePerWorker; i++){
			deleteRequest.remove(0);
		}
		// Verify that all were deleted
		verify(mockSQSDao, times(1)).deleteMessages(queueUrl, deleteRequest);
	}
	
	@Test
	public void testTrigerFiredStaggeredSuccess() throws InterruptedException{
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		long sleepTime = 0;
		for(int i=0; i<maxNumberOfWorkerThreads; i++){
			workerStack.push(new StubWorker(sleepTime+=500, 0, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		// Since each worker has a different sleep time, the delete messages should be staggered over 5 calls.
		verify(mockSQSDao, times(maxNumberOfWorkerThreads)).deleteMessages(anyString(), any(List.class));
	}
	
	@Test
	public void testQueueDisabled() throws InterruptedException{
		when(mockQueue.isEnabled()).thenReturn(false);
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		long sleepTime = 0;
		for(int i=0; i<maxNumberOfWorkerThreads; i++){
			workerStack.push(new StubWorker(sleepTime+=500, 0, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		// Since each worker has a different sleep time, the delete messages should be staggered over 5 calls.
		verify(mockSQSDao, times(0)).deleteMessages(anyString(), any(List.class));
	}
	
	@Test
	public void testResetVisibility() throws InterruptedException{
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will take longer than the half-life of the timeout
		// which should trigger a reset of the visibility timeout.
		long visibilityTimeoutMS = visibilityTimeoutSecs*1000;
		long sleepTime = visibilityTimeoutMS;
		for(int i=0; i<maxNumberOfWorkerThreads; i++){
			workerStack.push(new StubWorker(sleepTime, 2, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReveiver.setWorkerFactory(factory);
		
		// now trigger
		messageReveiver.triggerFired();
		// since each worker's sleep is equal to the visibility timeout they should each have their messages visibility rest once.
		verify(mockSQSDao, times(1)).resetMessageVisibility(anyString(), anyInt(), any(List.class));
	}

}
