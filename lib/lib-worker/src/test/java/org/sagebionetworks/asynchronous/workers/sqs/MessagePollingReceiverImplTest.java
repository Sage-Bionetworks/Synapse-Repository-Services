package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.util.DefaultClock;
import org.sagebionetworks.util.SingleThreadRunner;
import org.sagebionetworks.util.TestClock;
import org.sagebionetworks.util.ThreadTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

public class MessagePollingReceiverImplTest {

	MessagePollingReceiverImpl messageReceiver;
	Integer maxNumberOfWorkerThreads = 5;
	Integer maxMessagePerWorker = 3;
	Integer visibilityTimeoutSecs = 5;
	String queueUrl = "queueUrl";
	MessageQueue mockQueue;
	MessageWorkerFactory stubFactory;
	QueueServiceDao mockSQSDao;
	CountingSemaphoreDao mockCountingSemaphoreDao;
	List<Message> messageList;
	Queue<Message> messageQueue;
	SingleThreadRunner<Integer> triggerFired;

	@Before
	public void before() {
		ThreadTestUtils.doBefore();
		mockSQSDao = Mockito.mock(QueueServiceDao.class);
		mockQueue = Mockito.mock(MessageQueue.class);
		when(mockQueue.getQueueUrl()).thenReturn(queueUrl);
		when(mockQueue.isEnabled()).thenReturn(true);
		mockCountingSemaphoreDao = Mockito.mock(CountingSemaphoreDao.class);
		when(mockCountingSemaphoreDao.attemptToAcquireLock()).thenReturn("token");
		// Inject all of the dependencies
		messageReceiver = new MessagePollingReceiverImpl();
		messageReceiver.setMaxMessagePerWorker(maxMessagePerWorker);
		messageReceiver.setMaxNumberOfWorkerThreads(maxNumberOfWorkerThreads);
		messageReceiver.setVisibilityTimeoutSec(60);
		ReflectionTestUtils.setField(messageReceiver, "sqsDao", mockSQSDao);

		ReflectionTestUtils.setField(messageReceiver, "messageQueue", mockQueue);
		ReflectionTestUtils.setField(messageReceiver, "workerExecutorService", new ThreadPoolExecutor(maxNumberOfWorkerThreads,
				maxNumberOfWorkerThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()));
		ReflectionTestUtils.setField(messageReceiver, "queueUpdateTimer", Executors.newScheduledThreadPool(1));
		ReflectionTestUtils.setField(messageReceiver, "workerSemaphore", mockCountingSemaphoreDao);
		ReflectionTestUtils.setField(messageReceiver, "clock", new DefaultClock());

		// Setup a list of messages.
		int maxMessages = maxNumberOfWorkerThreads * maxMessagePerWorker;
		messageList = new LinkedList<Message>();
		// We want fewer messages than the max
		for (int i = 0; i < maxMessages - 2; i++) {
			messageList.add(new Message().withMessageId("id" + i).withReceiptHandle("handle1" + i));
		}
		// Setup the messages
		messageQueue = new LinkedList<Message>(messageList);
		when(mockSQSDao.receiveMessagesLongPoll(any(String.class), anyInt(), anyInt())).thenAnswer(new Answer<List<Message>>() {

			@Override
			public synchronized List<Message> answer(InvocationOnMock invocation) throws Throwable {
				Integer maxRequests = (Integer) invocation.getArguments()[2];
				List<Message> results = new LinkedList<Message>();
				if (messageQueue.isEmpty()) {
					// block further messages
					Thread.sleep(20000);
					return null;
				}
				for (int i = 0; i < maxRequests && !messageQueue.isEmpty(); i++) {
					results.add(messageQueue.poll());
				}
				return results;
			}

		});
		triggerFired = new SingleThreadRunner<Integer>(new Callable<Integer>() {
			@Override
			public Integer call() {
				try {
					return messageReceiver.triggerFired();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@After
	public void after() throws Exception {
		messageReceiver.exit();
		triggerFired.stop();
		ThreadTestUtils.doAfter();
	}

	@Test(expected = IllegalStateException.class)
	public void testNullmaxNumberOfWorkerThreads() throws InterruptedException {
		messageReceiver.setMaxNumberOfWorkerThreads(0);
		messageReceiver.triggerFired();
	}

	@Test(expected = IllegalStateException.class)
	public void testNullVisibilityTimeout() throws InterruptedException {
		messageReceiver.setVisibilityTimeoutSec(0);
		messageReceiver.triggerFired();
	}

	@Test
	public void testTriggerFiredMultipleMessagesSuccess() throws Exception {
		// Setup a worker stack
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will not timeout or throw exceptions
		for (int i = 0; i < maxNumberOfWorkerThreads; i++) {
			workerStack.push(new StubWorker(0, 0, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReceiver.setWorkerFactory(factory);

		// now run trigger
		triggerFired.start();

		// and wait for everything to happen
		verify(mockSQSDao, timeout(5000).times(5)).deleteMessages(anyString(), anyListOf(Message.class));
		verify(mockSQSDao, timeout(5000).times(maxNumberOfWorkerThreads + 1)).receiveMessagesLongPoll(anyString(), anyInt(), anyInt());
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(0, 3));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(3, 6));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(6, 9));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(9, 12));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(12, 13));
		verify(mockSQSDao, times(maxNumberOfWorkerThreads + 1)).receiveMessagesLongPoll(queueUrl, 60, 3);

		messageReceiver.exit();
		assertEquals(13, triggerFired.get().intValue());

		verifyNoMoreInteractions(mockSQSDao);
	}

	@Test
	public void testTriggerFiredOneFailureMulitipleSuccess() throws Exception {
		MessageWorkerFactory factory = new MessageWorkerFactory() {
			@Override
			public Callable<List<Message>> createWorker(List<Message> messages, WorkerProgress workerProgress) {
				StubWorker worker;
				if (messages.equals(messageList.subList(0, 3))) {
					worker = new StubWorker(500, 0, new Exception("Simulated a failure"));
				} else {
					worker = new StubWorker(0, 0, null);
				}
				return worker.withMessage(messages).withProgress(workerProgress);
			}
		};
		messageReceiver.setWorkerFactory(factory);

		// now trigger
		triggerFired.start();

		// and wait for everything to happen
		verify(mockSQSDao, timeout(5000).times(4)).deleteMessages(anyString(), anyListOf(Message.class));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(3, 6));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(6, 9));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(9, 12));
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(12, 13));
		verify(mockSQSDao, times(maxNumberOfWorkerThreads + 1)).receiveMessagesLongPoll(queueUrl, 60, 3);

		messageReceiver.exit();
		assertEquals(10, triggerFired.get().intValue());

		verifyNoMoreInteractions(mockSQSDao);
	}

	@Test
	public void testQueueDisabled() throws Exception {
		when(mockQueue.isEnabled()).thenReturn(false);
		// now trigger
		messageReceiver.triggerFired();
		verifyZeroInteractions(mockSQSDao);
	}

	@Test
	public void testResetVisibility() throws Exception {
		visibilityTimeoutSecs = 2;
		messageReceiver.setVisibilityTimeoutSec(visibilityTimeoutSecs);

		ScheduledExecutorService queueUpdater = Executors.newScheduledThreadPool(1);
		queueUpdater.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				messageReceiver.updateQueueTimeouts();
			}
		}, (visibilityTimeoutSecs * 1000) / 3, (visibilityTimeoutSecs * 1000) / 3, TimeUnit.MILLISECONDS);
		ReflectionTestUtils.setField(messageReceiver, "queueUpdateTimer", queueUpdater);

		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		// Setup workers that will take longer than the half-life of the timeout
		// which should trigger a reset of the visibility timeout.
		long visibilityTimeoutMS = visibilityTimeoutSecs * 1000;
		long sleepTime = visibilityTimeoutMS * 3 / 4;
		for (int i = 0; i < maxNumberOfWorkerThreads; i++) {
			workerStack.push(new StubWorker(sleepTime, 2, null));
		}
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReceiver.setWorkerFactory(factory);

		// now trigger
		triggerFired.start();
		// and wait for everything to happen
		verify(mockSQSDao, timeout(5000).times(5)).deleteMessages(anyString(), anyListOf(Message.class));
		verify(mockSQSDao, timeout(5000).times(maxNumberOfWorkerThreads + 1)).receiveMessagesLongPoll(anyString(), anyInt(), anyInt());

		// since each worker's sleep is equal to the visibility timeout they should each have their messages visibility
		// rest once.
		verify(mockSQSDao, times(1)).resetMessageVisibility(anyString(), anyInt(), anyListOf(Message.class));

		messageReceiver.exit();
		assertEquals(13, triggerFired.get().intValue());

		verifyNoMoreInteractions(mockSQSDao);
	}

	@Test
	public void tooManyWorkers() throws Exception {
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		for (int i = 0; i < maxNumberOfWorkerThreads; i++) {
			workerStack.push(new StubWorker(20000, 0, null));
		}
		messageReceiver.setWorkerFactory(factory);

		mockCountingSemaphoreDao = Mockito.mock(CountingSemaphoreDao.class);
		OngoingStubbing<String> when = when(mockCountingSemaphoreDao.attemptToAcquireLock());
		// one missed attempts for each worker
		for (int i = 0; i < maxNumberOfWorkerThreads; i++) {
			when = when.thenReturn(null);
		}
		// then all workers but one succeed
		when = when.thenReturn(null);
		for (int i = 0; i < maxNumberOfWorkerThreads - 1; i++) {
			// then each worker succeeds
			when = when.thenReturn("token");
		}
		// keep failing for a bit and then succeed
		when = when.thenReturn(null, null, null, "token");
		ReflectionTestUtils.setField(messageReceiver, "workerSemaphore", mockCountingSemaphoreDao);

		mockSQSDao = Mockito.mock(QueueServiceDao.class);
		when(mockSQSDao.receiveMessagesLongPoll(any(String.class), anyInt(), anyInt())).thenAnswer(
				new Answer<List<Message>>() {
					@Override
					public List<Message> answer(InvocationOnMock invocation) throws Throwable {
						return Lists.newArrayList(new Message());
					}

				});
		ReflectionTestUtils.setField(messageReceiver, "sqsDao", mockSQSDao);

		TestClock testClock = new TestClock();
		ReflectionTestUtils.setField(messageReceiver, "clock", testClock);
		long start = testClock.currentTimeMillis();

		// now trigger
		triggerFired.start();

		// wait for all attempts to fail and finally succeed
		verify(mockSQSDao, timeout(5000).times(maxNumberOfWorkerThreads)).receiveMessagesLongPoll(anyString(), anyInt(), anyInt());

		messageReceiver.exit();

		verify(mockCountingSemaphoreDao, times(maxNumberOfWorkerThreads * 2 + 4)).attemptToAcquireLock();
		long totalSleepTime = testClock.currentTimeMillis() - start;
		assertEquals(500 * (maxNumberOfWorkerThreads + 4), totalSleepTime);
	}

	@Test
	public void testExponentialBackoff() throws Exception {
		Stack<StubWorker> workerStack = new Stack<StubWorker>();
		workerStack.push(new StubWorker(0, 0, null));
		StubWorkerFactory factory = new StubWorkerFactory(workerStack);
		messageReceiver.setWorkerFactory(factory);

		mockSQSDao = Mockito.mock(QueueServiceDao.class);
		OngoingStubbing<List<Message>> when = when(mockSQSDao.receiveMessagesLongPoll(any(String.class), anyInt(), anyInt()));
		for (int i = 0; i < 10; i++) {
			when = when.thenThrow(new AmazonClientException("dummy"));
		}
		when.thenReturn(messageList.subList(0, 2)).thenAnswer(new Answer<List<Message>>() {
			@Override
			public List<Message> answer(InvocationOnMock invocation) throws Throwable {
				// block further messages
				Thread.sleep(20000);
				return null;
			}

		});
		ReflectionTestUtils.setField(messageReceiver, "sqsDao", mockSQSDao);

		TestClock testClock = new TestClock();
		ReflectionTestUtils.setField(messageReceiver, "clock", testClock);
		long start = testClock.currentTimeMillis();

		// now run trigger
		triggerFired.start();

		// and wait for everything to happen
		verify(mockSQSDao, timeout(5000)).deleteMessages(anyString(), anyListOf(Message.class));
		verify(mockSQSDao, timeout(5000).times(12)).receiveMessagesLongPoll(anyString(), anyInt(), anyInt());
		verify(mockSQSDao).deleteMessages(queueUrl, messageList.subList(0, 2));
		verify(mockSQSDao, times(12)).receiveMessagesLongPoll(queueUrl, 60, 3);

		messageReceiver.exit();
		assertEquals(2, triggerFired.get().intValue());

		verifyNoMoreInteractions(mockSQSDao);
		// sequence is 500, 750, 1125, 1687, 2531, 3796, 5695, 8542, 12814 + a quarter random each time
		// total time should be between 50000 and 63000
		long totalSleepTime = testClock.currentTimeMillis() - start;
		assertTrue(totalSleepTime + " > 50000 && " + totalSleepTime + " < 63000", totalSleepTime > 50000 && totalSleepTime < 63000);
	}
}
