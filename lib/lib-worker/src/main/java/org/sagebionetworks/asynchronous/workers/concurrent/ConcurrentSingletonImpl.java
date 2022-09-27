package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

@Repository
public class ConcurrentSingletonImpl implements ConcurrentSingleton {

	private final CountingSemaphore countingSemaphore;
	private final ExecutorService executorService;
	private final AmazonSQSClient amazonSQSClient;
	private final StackStatusDao stackStatusDao;

	public ConcurrentSingletonImpl(CountingSemaphore countingSemaphore, AmazonSQSClient amazonSQSClient, StackStatusDao stackStatusDao) {
		super();
		this.countingSemaphore = countingSemaphore;
		this.amazonSQSClient = amazonSQSClient;
		this.stackStatusDao = stackStatusDao;
		this.executorService = Executors.newCachedThreadPool();
	}

	@Override
	public boolean isStackAvailableForWrite() {
		return !stackStatusDao.isStackReadWrite();
	}

	@Override
	public long getCurrentTimeMS() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long sleepTimeMS) throws InterruptedException {
		Thread.sleep(sleepTimeMS);
	}

	@Override
	public String getSqsQueueUrl(String queueName) {
		return amazonSQSClient.getQueueUrl(queueName).getQueueUrl();
	}


	@Override
	public void runWithSemaphoreLock(String lockKey, int lockTimeoutSec, int maxLockCount, ProgressCallback callback,
			Runnable runner) {
		String token = countingSemaphore.attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount);
		if (token == null) {
			return;
		}
		callback.addProgressListener(() -> {
			countingSemaphore.refreshLockTimeout(lockKey, token, lockTimeoutSec);
		});
		try {
			runner.run();
		} finally {
			countingSemaphore.releaseLock(lockKey, token);
		}
	}


	@Override
	public List<WorkerJob> pollForMessagesAndStartJobs(String queueUrl, int maxNumberOfMessages,
			int messageVisibilityTimeoutSec, MessageDrivenRunner worker) {
		// Poll for the requested number of messages.
		List<Message> messages = amazonSQSClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueUrl)
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(maxNumberOfMessages)
				.withVisibilityTimeout(messageVisibilityTimeoutSec)).getMessages();
		// For each message start a new job.
		return messages.stream().map((message) -> {
			return startWorkerJob(queueUrl, messageVisibilityTimeoutSec, worker, message);
		}).collect(Collectors.toList());
	}

	WorkerJob startWorkerJob(String queueUrl, int messageVisibilityTimeoutSec, MessageDrivenRunner worker,
			Message message) {
		ConcurrentProgressCallback callback = new ConcurrentProgressCallback(messageVisibilityTimeoutSec);
		callback.addProgressListener(() -> {
			amazonSQSClient.changeMessageVisibility(new ChangeMessageVisibilityRequest().withQueueUrl(queueUrl)
					.withQueueUrl(message.getReceiptHandle()).withVisibilityTimeout(messageVisibilityTimeoutSec));
		});
		Future<Void> future = executorService.submit(() -> {
			boolean deleteMessage = true;
			try {
				worker.run(callback, message);
			} catch (RecoverableMessageException e) {
				deleteMessage = false;
				amazonSQSClient.changeMessageVisibility(new ChangeMessageVisibilityRequest().withQueueUrl(queueUrl)
						.withQueueUrl(message.getReceiptHandle()).withVisibilityTimeout(5));
			} finally {
				if (deleteMessage) {
					amazonSQSClient.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl)
							.withReceiptHandle(message.getReceiptHandle()));
				}
			}
			return null;
		});
		return new WorkerJob(future, callback);
	}

}
