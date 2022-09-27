package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * A new {@link ConcurrentWorkerStack} should be created for each type of
 * worker. The stack will first attempt to acquire a global semaphore lock using
 * a combination of the provided semaphoreLockKey and semaphoreMaxLockCount. An
 * acquired lock will be held for the duration of the stacks's
 * {@link Runnable#run()} method.
 * <p>
 * Once a semaphore lock is acquired, this instance will run an infinite loop
 * that will perform three main tasks:
 * <ul>
 * <li>Start new worker jobs as needed up to the provided
 * maxThreadsPerMachine.</li>
 * <li>Periodically refresh the semaphore lock and the SQS message visibility of
 * each running job.</li>
 * <li>Remove finished jobs to provide capacity for new jobs.</li>
 * </ul>
 * Any uncaught exceptions from the workers, will be logged but will not result
 * in the termination of the stack. However, if an {@link InterruptedException}
 * is encountered either from a {@link Future#get()} or
 * {@link Thread#sleep(long)}, the stack will enter a "shutdown" state. Once the
 * "shutdown" state is started, it will continue to run and monitor all Existing
 * jobs, but it will no longer add new jobs. Once all of the existing jobs are
 * completed, shutdown will be completed and the loop will terminate.
 */
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
	private final List<WorkerJob> runningJobs;
	private final String queueUrl;

	private long nextRefreshTimeMS;
	private State state;;
	private final ConcurrentProgressCallback lockCallback;

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
		this.lockRefreshFrequencyMS = (semaphoreLockAndMessageVisibilityTimeoutSec * 1000) / 3;
		this.queueUrl = singleton.getSqsQueueUrl(queueName);
		this.runningJobs = new ArrayList<>(semaphoreMaxLockCount);
		this.lockCallback = new ConcurrentProgressCallback(semaphoreLockAndMessageVisibilityTimeoutSec);
	}

	/**
	 * The main entry point for the stack. This method should be called from a
	 * timer.
	 */
	@Override
	public void run() {
		state = State.CONTINUING;
		if (!canProcessMoreMessages()) {
			return;
		}
		singleton.runWithSemaphoreLock(semaphoreLockKey, semaphoreLockAndMessageVisibilityTimeoutSec,
				semaphoreMaxLockCount, lockCallback, () -> {
					try {
						mainLoop();
					} catch (Exception e) {
						log.error("Failed:", e);
					}
				});
	}

	/**
	 * Start the shutdown process for this stack. The stack will no longer add get
	 * new messages and start new worker threads. However, the stack will continue
	 * to wait for the existing jobs to complete.
	 */
	void startShutdown() {
		state = State.CONTINUING;
	}

	/**
	 * Can this stack process more messages?
	 * 
	 */
	boolean canProcessMoreMessages() {
		if (State.SHUTTING_DOWN.equals(state)) {
			return false;
		}
		if (canRunInReadOnly) {
			return true;
		}
		return singleton.isStackAvailableForWrite();
	}

	/**
	 * Reset the nextRefreshTimeMS to be now() + (timeout/3)
	 */
	void resetNextRefreshTimeMS() {
		nextRefreshTimeMS = singleton.getCurrentTimeMS() + lockRefreshFrequencyMS;
	}

	void mainLoop() {
		while (shouldContinueRunning()) {
			refreshLocksIfNeeded();
			checkRunningJobs();
			attemptToAddMoreWorkers();
			try {
				singleton.sleep(1000);
			} catch (InterruptedException e) {
				log.info("Interrupted. Will shutdown after all jobs finish running");
				startShutdown();
			}
		}
	}

	/**
	 * The loop should continue to run if this returns true.
	 * 
	 * @return True if runningJobs.size > 1 || State.RUNNING.equals(state)
	 */
	boolean shouldContinueRunning() {
		if (runningJobs.size() > 1) {
			return true;
		}
		return State.CONTINUING.equals(state);
	}
	
	/**
	 * Check on all of the running jobs. Any job that is finished will be removed.
	 * If a {@link Future#get()} throws an {@link InterruptedException}, the stack
	 * will enter the "shutdown" state. All other exceptions will be logged.
	 */
	void checkRunningJobs() {
		Iterator<WorkerJob> it = runningJobs.iterator();
		while (it.hasNext()) {
			WorkerJob job = it.next();
			if (job.getFuture().isDone()) {
				it.remove();
				try {
					job.getFuture().get();
				} catch (InterruptedException e) {
					log.info("Interrupted. Will shutdown after all jobs finish running");
					startShutdown();
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					log.error("Worker failed:", cause);
				}
			}
		}
	}

	/**
	 * Attempt to add new workers while remaining under the maxThreadsPerMachine.
	 * Note: Since AWS SQS has a limit of of 10 messages per
	 * {@link AmazonSQSClient#receiveMessage(com.amazonaws.services.sqs.model.ReceiveMessageRequest)},
	 * no more than 10 worker threads will be started per call.
	 */
	void attemptToAddMoreWorkers() {
		if (!canProcessMoreMessages()) {
			return;
		}
		int maxNumberOfMessagesToRecieve = Math.min(MAX_MESSAGES_PER_REQUEST,
				maxThreadsPerMachine - runningJobs.size());
		if (maxNumberOfMessagesToRecieve < 1) {
			return;
		}

		runningJobs.addAll(singleton.pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessagesToRecieve,
				maxNumberOfMessagesToRecieve, worker));

	}

	/**
	 * If now() >= nextRefreshTimeMS, then trigger the refresh of all locks/messages.
	 */
	void refreshLocksIfNeeded() {
		if (singleton.getCurrentTimeMS() >= nextRefreshTimeMS) {
			lockCallback.progressMade();
			runningJobs.forEach(job -> {
				job.getListener().progressMade();
			});
			resetNextRefreshTimeMS();
		}
	}

	/**
	 * Possible states for this stack.
	 *
	 */
	static enum State {
		CONTINUING, SHUTTING_DOWN
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
