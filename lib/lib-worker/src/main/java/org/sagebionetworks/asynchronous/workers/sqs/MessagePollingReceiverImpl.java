package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

/**
 * A basic implementation of a long polling message receiver.
 */
public class MessagePollingReceiverImpl implements MessageReceiver {

	static private Logger log = LogManager.getLogger(MessagePollingReceiverImpl.class);

	/**
	 * The maximum number of seconds that a message visibility timeout can be set to. Note: This does not mean a worker
	 * cannot run for more than this time. The message receiver will automatically refresh the timeout for any message
	 * that reaches its timeout half-life. By keeping the visibility timeout small we can quickly recover from failures.
	 */
	public static final int MAX_VISIBILITY_TIMEOUT_SECS = 60;
	public static final long INITIAL_BACKOFF_TIME_MSEC = 500;
	public static final double BACKOFF_MULTIPLIER = 1.5;
	public static final long MAX_BACKOFF_TIME_MSEC = 10000;

	@Autowired
	QueueServiceDao sqsDao;
	@Autowired
	Clock clock;

	/**
	 * The maximum number of threads used to process messages.
	 */
	private int maxNumberOfWorkerThreads;

	/**
	 * The maximum number of messages that each worker should handle.
	 */
	private int maxMessagePerWorker;
	/**
	 * The duration (in seconds) that the received messages are hidden from subsequent retrieve requests after being
	 * retrieved by a <code>ReceiveMessage</code> request.
	 */
	private int visibilityTimeoutSec;

	/**
	 * The MessageQueue that this instance is watching.
	 */
	private MessageQueue messageQueue;

	/**
	 * Providers workers to processes messages.
	 */
	private MessageWorkerFactory workerFactory;

	/**
	 * This is our thread pool.
	 */
	private ThreadPoolExecutor workerExecutorService;

	/**
	 * this tells us we are trying to exit all workers asap
	 */
	private volatile boolean cancelled = false;

	/**
	 * This is the thread that periodically updates the queue entries that are still progressing
	 */
	private ScheduledExecutorService queueUpdateTimer;

	private CountingSemaphoreDao workerSemaphore;

	private ConcurrentLinkedQueue<Message> progressingMessagesQueue = new ConcurrentLinkedQueue<Message>();
	private WorkerProgress workerProgress = new WorkerProgress() {
		@Override
		public void progressMadeForMessage(Message messag) {
			// Add this message to the
			progressingMessagesQueue.add(messag);
		}

		@Override
		public void retryMessage(Message message, int retryTimeoutInSeconds) {
			sqsDao.resetMessageVisibility(messageQueue.getQueueUrl(), retryTimeoutInSeconds, Collections.singletonList(message));
		}
	};

	/**
	 * Default used by Spring.
	 */
	public MessagePollingReceiverImpl() {
	}

	/**
	 * The maximum number of threads used to process messages.
	 * 
	 * @param maxNumberOfWorkerThreads
	 */
	@Required
	public void setMaxNumberOfWorkerThreads(int maxNumberOfWorkerThreads) {
		this.maxNumberOfWorkerThreads = maxNumberOfWorkerThreads;
	}

	/**
	 * The duration (in seconds) that the received messages are hidden from subsequent retrieve requests after being
	 * retrieved by a <code>ReceiveMessage</code> request.
	 * 
	 * @param visibilityTimeout
	 */
	@Required
	public void setVisibilityTimeoutSec(int visibilityTimeout) {
		// Will put this back after all workers can be ajusted.
		// if(visibilityTimeout > MAX_VISIBILITY_TIMEOUT_SECS) {
		// throw new
		// IllegalArgumentException("Visibility Timeout Seconds cannot exceed: "+MAX_VISIBILITY_TIMEOUT_SECS+" seconds. This does not limit the amount of time a worker can run since, the message receiver will automatically refresh the timeout for any message that reaches its timeout half-life.");
		// }
		this.visibilityTimeoutSec = visibilityTimeout;
	}

	/**
	 * The maximum number of messages that each worker should handle.
	 * 
	 * @param maxMessagePerWorker
	 */
	@Required
	public void setMaxMessagePerWorker(int maxMessagePerWorker) {
		this.maxMessagePerWorker = maxMessagePerWorker;
	}

	/**
	 * Providers workers to processes messages.
	 * 
	 * @param workerFactory
	 */
	@Required
	public void setWorkerFactory(MessageWorkerFactory workerFactory) {
		this.workerFactory = workerFactory;
	}

	@Required
	public void setMessageQueue(MessageQueue messageQueue) {
		this.messageQueue = messageQueue;
	}

	@Required
	public void setWorkerSemaphore(CountingSemaphoreDao workerSemaphore) {
		ValidateArgument
				.requirement(
						workerSemaphore.getLockTimeoutMS() >= 2 * (sqsDao.getLongPollWaitTimeInSeconds() * 1000L),
						"Worker semaphore timeout should be much longer than the sqs long poll timeout. At least 20 seconds + time it takes to handle messages, but ideally much longer (minutes)");

		this.workerSemaphore = workerSemaphore;
	}

	@PostConstruct
	void init() {
		if (!messageQueue.isEnabled()) {
			return;
		}

		workerExecutorService = new ThreadPoolExecutor(maxNumberOfWorkerThreads, maxNumberOfWorkerThreads, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		queueUpdateTimer = Executors.newScheduledThreadPool(1);
		queueUpdateTimer.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				updateQueueTimeouts();
			}
		}, visibilityTimeoutSec / 3, visibilityTimeoutSec / 3, TimeUnit.SECONDS);
	}

	@PreDestroy
	void exit() throws Exception {
		cancelled = true;
		workerExecutorService.shutdownNow();
		queueUpdateTimer.shutdownNow();
		workerExecutorService.awaitTermination(30, TimeUnit.SECONDS);
		queueUpdateTimer.awaitTermination(5, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			triggerFired();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int triggerFired() throws InterruptedException {
		// Validate all config.
		verifyConfig();
		// Do nothing if this queue is not enabled
		if (!messageQueue.isEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("Nothing to do since the queue is disabled: " + messageQueue.getQueueName());
			}
			return 0;
		}

		final Object queuePollAccess = new Object();
		List<Callable<Integer>> tasks = Lists.newArrayListWithCapacity(maxNumberOfWorkerThreads);
		for (int i = 0; i < maxNumberOfWorkerThreads; i++) {
			tasks.add(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					int totalCount = 0;
					while (!cancelled) {
						String lockToken = null;
						try {
							try {
								List<Message> toBeProcessed = null;
								// we only ever want one thread per machine to long poll sqs
								synchronized (queuePollAccess) {
									if (cancelled) {
										break;
									}
									lockToken = workerSemaphore.attemptToAcquireLock();
									if (lockToken == null) {
										// could not acquire a lock, sleep a while (while holding the queue lock, so
										// other
										// threads don't try to get the lock immediately) and try again
										clock.sleep(500);
										continue;
									}
									long backoffTime = INITIAL_BACKOFF_TIME_MSEC;
									while (toBeProcessed == null) {
										try {
											toBeProcessed = sqsDao.receiveMessagesLongPoll(messageQueue.getQueueUrl(), visibilityTimeoutSec,
													maxMessagePerWorker);
										} catch (AmazonClientException e) {
											// Something went wrong. Most likely, sqs unreachable or we are being
											// throttled.
											// Use exponential backoff
											clock.sleep(backoffTime + RandomUtils.nextInt((int) INITIAL_BACKOFF_TIME_MSEC / 4));
											if (backoffTime < MAX_BACKOFF_TIME_MSEC) {
												backoffTime *= BACKOFF_MULTIPLIER;
											}
										}
									}
								}
								if (toBeProcessed.size() > 0) {
									final String lockTokenToExtend = lockToken;
									totalCount += handleMessages(toBeProcessed, new WorkerProgress(){
										long lastExtension = clock.currentTimeMillis();
										@Override
										public void progressMadeForMessage(Message message) {
											workerProgress.progressMadeForMessage(message);
											long elapsed = clock.currentTimeMillis() - lastExtension;
											if (elapsed > workerSemaphore.getLockTimeoutMS() / 2L) {
												try {
													workerSemaphore.extendLockLease(lockTokenToExtend);
													lastExtension = clock.currentTimeMillis();
												} catch (NotFoundException e) {
													// the lock no longer exists. We should abort the work
													throw new RuntimeException("Abort worker because lock no longer exists: "
															+ e.getMessage());
												}
											}
										}

										@Override
										public void retryMessage(Message message, int retryTimeoutInSeconds) {
											workerProgress.retryMessage(message, retryTimeoutInSeconds);
										}
										
									});
								}
							} finally {
								if (lockToken != null) {
									workerSemaphore.releaseLock(lockToken);
								}
							}
						} catch (InterruptedException e) {
							// normally, we should only get here if we're trying to shutdown, in which case cancelled
							// should already be true
							if (!cancelled) {
								log.error("Unexpected interrupted exception in worker thread: " + e.getMessage(), e);
							}
							break;
						} catch (Throwable t) {
							log.error("Unexpected error in worker thread: " + t.getMessage(), t);
							// we got an unexpected error. Sleep for a bit, so we won't flood the logs if this is a
							// persistent error
							clock.sleep(1000);
						}
					}
					return totalCount;
				}
			});
		}
		List<Future<Integer>> results = workerExecutorService.invokeAll(tasks);
		int count = 0;
		for (Future<Integer> result : results) {
			try {
				count += result.get();
			} catch (ExecutionException e) {
				log.error("Error in worker thread: " + e.getCause().getMessage(), e.getCause());
			}
		}
		return count;
	}

	void updateQueueTimeouts() {
		// If the visibility timeout is exceeded the messages will once again become visible
		// to other works. We do not want this to happen to messages that are currently bing processed.
		// Therefore we reset the visibility timeout of all active messages when the half-life is reached.

		// Build up the set of messages that need to be updated
		Set<Message> messagesToUpdate = new HashSet<Message>();
		for (Message message = progressingMessagesQueue.poll(); message != null; message = progressingMessagesQueue.poll()) {
			messagesToUpdate.add(message);
		}
		if (!messagesToUpdate.isEmpty()) {
			sqsDao.resetMessageVisibility(messageQueue.getQueueUrl(), visibilityTimeoutSec, messagesToUpdate);
		}
	}

	/**
	 * Validate that we have all of the required configuration.
	 */
	private void verifyConfig() {
		if (sqsDao == null)
			throw new IllegalStateException("sqsDao cannot be null");
		if (maxNumberOfWorkerThreads == 0)
			throw new IllegalStateException("maxNumberOfWorkerThreads must be > 0 (is " + maxNumberOfWorkerThreads + ")");
		if (visibilityTimeoutSec == 0)
			throw new IllegalStateException("visibilityTimeout must be > 0 (is " + visibilityTimeoutSec + ")");
		if (messageQueue == null)
			throw new IllegalStateException("messageQueue cannot be null");
	}

	@Override
	public void emptyQueue() throws InterruptedException {
		long start = clock.currentTimeMillis();
		for (;;) {
			List<Message> toBeProcessed = sqsDao.receiveMessages(messageQueue.getQueueUrl(), visibilityTimeoutSec, maxMessagePerWorker);
			if (toBeProcessed.size() == 0) {
				break;
			}
			handleMessages(toBeProcessed, workerProgress);
			if (clock.currentTimeMillis() - start > visibilityTimeoutSec * 1000 * 10) {
				throw new RuntimeException("Timed-out waiting process all messages that were on the queue.");
			}
		}
	}

	private int handleMessages(List<Message> toBeProcessed, WorkerProgress progress) throws InterruptedException {
		try {
			final Callable<List<Message>> worker = workerFactory.createWorker(toBeProcessed, progress);
			List<Message> messagesToDelete = worker.call();
			// all returned messages are to be deleted.
			if (messagesToDelete.size() > 0) {
				sqsDao.deleteMessages(messageQueue.getQueueUrl(), messagesToDelete);
			}
			return messagesToDelete.size();
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			// We cannot remove this message from the queue.
			log.error("Failed to process a SQS message:", e);
			return 0;
		}
	}
}
