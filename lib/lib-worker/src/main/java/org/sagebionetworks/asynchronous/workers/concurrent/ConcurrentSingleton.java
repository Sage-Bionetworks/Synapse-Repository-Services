package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;

/**
 * This singleton provides access to all dependencies that are shared by all
 * worker stack instances. Note: This singleton does not directly encapsulate
 * the state of individual workers or worker stacks. However, it does
 * encapsulate the {@link Executors#newCachedThreadPool()} used to run each
 * worker on a separate thread.
 *
 */
public interface ConcurrentSingleton {

	/**
	 * Is the stack available for write (read-write mode)?
	 * 
	 * @return
	 */
	boolean isStackAvailableForWrite();

	/**
	 * Attempt to run the provided runner while holding a global semaphore lock. If
	 * a lock cannot be acquired, the provided runner will not be run. If a lock is
	 * acquired, it will be held for the duration of the {@link Runnable#run()}
	 * call. The lock will be unconditionally released after the run.
	 * 
	 * @param lockKey
	 * @param lockTimeoutSec
	 * @param maxLockCount
	 * @param callback
	 * @param runner
	 */
	void runWithSemaphoreLock(String lockKey, int lockTimeoutSec, int maxLockCount, ProgressCallback callback,
			Runnable runner);

	/**
	 * Get the {@link System#currentTimeMillis()}.
	 * 
	 * @return
	 */
	long getCurrentTimeMS();

	/**
	 * Will call {@link Thread#sleep(long)}.
	 * 
	 * @param sleepTimeMS
	 * @throws InterruptedException
	 */
	void sleep(long sleepTimeMS) throws InterruptedException;

	/**
	 * Given a queue name, lookup the associated SQS queue URL. The caller is
	 * Expected to cache the provided URL.
	 * 
	 * @param queueName
	 * @return
	 */
	String getSqsQueueUrl(String queueName);

	/**
	 * Poll for messages from an SQS queue. Each message received from the queue
	 * will be passed to the provided worker on a new thread. It is up to the caller
	 * to monitor the resulting {@link Future}, and to refresh the message
	 * visibility timeout by calling {@link ProgressListener#progressMade()}.
	 * 
	 * @param queueUrl                    The URL of the queue to poll.
	 * @param maxNumberOfMessages         The maximum number of messages that should
	 *                                    be received from the queue. A new thread
	 *                                    will be started for each message received.
	 *                                    Note: The current AWS limit for the number
	 *                                    of messages is 10.
	 * @param messageVisibilityTimeoutSec The number of seconds that received
	 *                                    message will remain in-flight before being
	 *                                    returned to the queue. The caller is
	 *                                    expected to call
	 *                                    {@link ProgressListener#progressMade()} to
	 *                                    periodically refresh the message timeout.
	 * @param worker                      For each message polled this worker will
	 *                                    receive a call to:
	 *                                    {@link MessageDrivenRunner#run(ProgressCallback, com.amazonaws.services.sqs.model.Message)}
	 * @return
	 */
	List<WorkerJob> pollForMessagesAndStartJobs(String queueUrl, int maxNumberOfMessages,
			int messageVisibilityTimeoutSec, MessageDrivenRunner worker);

}
