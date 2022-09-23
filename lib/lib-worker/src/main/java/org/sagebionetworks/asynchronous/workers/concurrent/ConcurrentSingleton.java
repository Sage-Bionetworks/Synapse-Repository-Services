package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.amazonaws.services.sqs.model.Message;

/**
 * This singleton provides access to all dependencies that are shared by all
 * worker stack instances. Note: This singleton must not encapsulate the state
 * of individual workers or worker stacks.
 *
 */
public interface ConcurrentSingleton {

	/**
	 * Is the stack currently in read-only mode?
	 * 
	 * @return
	 */
	boolean isInReadOnlyMode();

	Optional<String> attemptToAcquireSempahoreLock(String semaphoreLockKey, Integer semaphoreLockAndMessageVisibilityTimeoutSec,
			Integer semaphoreMaxLockCount);

	void releaseSemaphoreLockSilently(String semaphoreLockKey, String lockToken);

	void refreshSemaphoreLockTimeout(String key, String token, long timeoutSec);

	long getCurrentTimeMS();

	void sleep(long sleepTimeMS);

	List<Message> getSqsMessages(String queueUrl, int maxNumberOfMessages, int messageVisibilityTimeoutSec);

	String getSqsQueueUrl(String queueName);

	void resetSqsMessageVisibilityTimeoutSilently(String queueUrl, String messageReceiptHandle,
			int messageVisibilityTimeoutSec);

	void deleteSqsMessageSilently(String queuUrl, String messageReceiptHandle);

	Future<Void> submitJobToThreadPool(Callable<Void> job);
}
