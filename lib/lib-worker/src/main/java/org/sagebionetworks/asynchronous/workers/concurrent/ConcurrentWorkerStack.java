package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;


public class ConcurrentWorkerStack implements Runnable {

	public static final int MAX_MESSAGES_PER_REQUEST = 10;

	private static final Logger log = LogManager.getLogger(ConcurrentWorkerStack.class);

	private final ConcurrentSingleton singleton;
	private final Boolean canRunInReadOnly;
	private final String semaphoreLockKey;
	private final Integer semaphoreMaxLockCount;
	private final Integer semaphoreLockAndMessageVisibilityTimeoutSec;
	private final Integer maxThreadsPerMachine;
	private final MessageDrivenRunner worker;
	
	private final int lockRefreshFrequencyMS;
	private final List<Job> runningJobs;
	private final String queueUrl;
	
	private long nextRefreshTimeMS;
	private boolean shuttingDown;
	private ProgressListener lockProgressListener;
	
	
	public ConcurrentWorkerStack(ConcurrentSingleton singleton, Boolean canRunInReadOnly, String semaphoreLockKey,
			Integer semaphoreMaxLockCount, Integer semaphoreLockAndMessageVisibilityTimeoutSec,
			Integer maxThreadsPerMachine, MessageDrivenRunner worker, String queueName) {
		super();
		this.singleton = singleton;
		this.canRunInReadOnly = Boolean.TRUE.equals(canRunInReadOnly);
		this.semaphoreLockKey = semaphoreLockKey;
		this.semaphoreMaxLockCount = semaphoreMaxLockCount;
		this.semaphoreLockAndMessageVisibilityTimeoutSec = semaphoreLockAndMessageVisibilityTimeoutSec;
		this.maxThreadsPerMachine = maxThreadsPerMachine;
		this.worker = worker;
		this.lockRefreshFrequencyMS = (semaphoreLockAndMessageVisibilityTimeoutSec *1000)/3;
		this.queueUrl = singleton.getSqsQueueUrl(queueName);
		this.runningJobs = new ArrayList<>(semaphoreMaxLockCount);
		this.shuttingDown = false;
	}

	@Override
	public void run() {
		if (!canProcessMoreMessages()) {
			return;
		}
		Optional<String> lockTokenOptional = singleton.attemptToAcquireSempahoreLock(semaphoreLockKey,
				semaphoreLockAndMessageVisibilityTimeoutSec, semaphoreMaxLockCount);
		if(lockTokenOptional.isEmpty()) {
			return;
		}
		lockProgressListener = ()->{
			try {
				singleton.refreshSemaphoreLockTimeout(semaphoreLockKey, lockTokenOptional.get(), semaphoreLockAndMessageVisibilityTimeoutSec);
			} catch (Exception e) {
				log.error("Failed to refresh semaphore lock. Will shutdown after all of the current jobs finish.", e);
				startShutdown();
				// Use a no-operation listener 
				lockProgressListener = ()->{};
			}
		};
		updateNextRefreshTimeMS();
		try {
			loop();
		} catch (Exception e) {
			log.error("Failed:", e);
		} finally {
			singleton.releaseSemaphoreLockSilently(semaphoreLockKey, lockTokenOptional.get());
		}
	}
	
	/**
	 * Start the shutdown process for this stack.  The stack will no longer add get new messages and start new worker
	 * threads.  However, the stack will continue to wait for the existing jobs to complete.
	 */
	void startShutdown() {
		shuttingDown = true;
	}
	
	/**
	 * Can this stack process more messages?
	 * 
	 */
	boolean canProcessMoreMessages() {
		if(shuttingDown) {
			return false;
		}
		if(canRunInReadOnly) {
			return true;
		}
		return !singleton.isInReadOnlyMode();
	}

	void updateNextRefreshTimeMS() {
		nextRefreshTimeMS = singleton.getCurrentTimeMS() + lockRefreshFrequencyMS;
	}
	
	void loop() {
		while(shouldContinue()) {
			frame();
			singleton.sleep(1000);
		}
	}
	
	void frame() {
		refreshLocksIfNeeded();
		checkRunningJobs();
		attemptToAddMoreWorkers();
	}

	/**
	 * Check on all of the running jobs.
	 * Any job that is 
	 */
	void checkRunningJobs() {
		Iterator<Job> it = runningJobs.iterator();
		while(it.hasNext()) {
			Job job = it.next();
			if(job.future.isDone()) {
				it.remove();
				try {
					job.future.get();
				} catch (InterruptedException e) {
					log.info("Interrupted. Will shutdown after all jobs finish running");
					startShutdown();
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if(cause instanceof RecoverableMessageException) {
						int messageVisibilityTimeoutSec = 5;
						singleton.resetSqsMessageVisibilityTimeoutSilently(queueUrl, job.message.getReceiptHandle(), messageVisibilityTimeoutSec);
					}else {
						singleton.deleteSqsMessageSilently(queueUrl, job.message.getReceiptHandle());
					}
				}
			}
		}
	}
	
	void attemptToAddMoreWorkers() {
		if (!canProcessMoreMessages()) {
			return;
		}
		int maxNumberOfMessagesToRecieve = Math.min(MAX_MESSAGES_PER_REQUEST, maxThreadsPerMachine - runningJobs.size());
		if(maxNumberOfMessagesToRecieve < 1) {
			return;
		}
		List<Message> messages = singleton.getSqsMessages(queueUrl, maxNumberOfMessagesToRecieve,
				semaphoreLockAndMessageVisibilityTimeoutSec);
		messages.forEach(m -> {
			ConcurrentProgressCallback callback = new ConcurrentProgressCallback(
					semaphoreLockAndMessageVisibilityTimeoutSec);
			callback.addProgressListener(()->{
				singleton.resetSqsMessageVisibilityTimeoutSilently(queueUrl, m.getReceiptHandle(), semaphoreLockAndMessageVisibilityTimeoutSec);
			});			
			Future<Void> future = singleton.submitJobToThreadPool(() -> {
				worker.run(callback, m);
				return null;
			});
			runningJobs.add(new Job(m, callback, future));
		});

	}

	boolean shouldContinue() {
		if(runningJobs.size() > 1) {
			return true;
		}
		return !shuttingDown;
	}
	
	void refreshLocksIfNeeded() {
		if(singleton.getCurrentTimeMS() >= nextRefreshTimeMS) {
			lockProgressListener.progressMade();
			runningJobs.forEach(job->{
				job.callback.progressMade();
			});
			updateNextRefreshTimeMS();
		}
	}

	private static class Job {
		final Message message;
		final ConcurrentProgressCallback callback;
		final Future<Void> future;
		
		public Job(Message message, ConcurrentProgressCallback callback, Future<Void> future) {
			super();
			this.message = message;
			this.callback = callback;
			this.future = future;
		}
	}
	
	/**
	 * Use the builder to construct this worker stack.
	 * 
	 * @return
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {


		public ConcurrentWorkerStack build() {
//			return new ConcurrentWorkerStack(countingSempahore, gate, semaphoreLockKey, semaphoreMaxLockCount,
//					semaphoreLockAndMessageVisibilityTimeoutSec, runner, executorService);
			return null;
		}
	}

}
