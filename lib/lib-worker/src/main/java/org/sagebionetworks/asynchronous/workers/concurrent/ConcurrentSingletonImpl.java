package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.amazonaws.services.sqs.model.Message;

public class ConcurrentSingletonImpl implements ConcurrentSingleton {
	
	private final ExecutorService executorService;
	
	public ConcurrentSingletonImpl() {
		super();
		this.executorService = Executors.newCachedThreadPool();
	}

	@Override
	public boolean isInReadOnlyMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Optional<String> attemptToAcquireSempahoreLock(String semaphoreLockKey,
			Integer semaphoreLockAndMessageVisibilityTimeoutSec, Integer semaphoreMaxLockCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releaseSemaphoreLockSilently(String semaphoreLockKey, String lockToken) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshSemaphoreLockTimeout(String key, String token, long timeoutSec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getCurrentTimeMS() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void sleep(long sleepTimeMS) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Message> getSqsMessages(String queueUrl, int maxNumberOfMessages, int messageVisibilityTimeoutSec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSqsQueueUrl(String queueName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetSqsMessageVisibilityTimeoutSilently(String queueUrl, String messageReceiptHandle,
			int messageVisibilityTimeoutSec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteSqsMessageSilently(String queuUrl, String messageReceiptHandle) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Future<Void> submitJobToThreadPool(Callable<Void> job) {
		// TODO Auto-generated method stub
		return null;
	}


}
