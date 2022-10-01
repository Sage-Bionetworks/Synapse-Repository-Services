package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * A new {@link ConcurrentWorkerStack} should be created for each type of
 * worker. The stack will first attempt to acquire a global semaphore lock using
 * a combination of the provided semaphoreLockKey and semaphoreMaxLockCount. An
 * acquired lock will be held for the duration of the stacks's
 * {@link ConcurrentWorkerStack#run()} method.
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
 * "shutdown" state is started, it will continue to run and monitor all existing
 * jobs, but it will no longer add new jobs. Once all of the existing jobs are
 * completed, shutdown will be completed and the loop will terminate.
 * <p>
 * Note: While this stack manages all worker threads from the thread that calls
 * {@link ConcurrentWorkerStack#run()}.
 */
public class ConcurrentWorkerStack implements Runnable {

	public static final int MAX_MESSAGES_PER_REQUEST = 10;

	private static final Logger log = LogManager.getLogger(ConcurrentWorkerStack.class);

	// direct parameters
	private final ConcurrentSingleton singleton;
	private final boolean canRunInReadOnly;
	private final String semaphoreLockKey;
	private final int semaphoreMaxLockCount;
	private final int semaphoreLockAndMessageVisibilityTimeoutSec;
	private final int maxThreadsPerMachine;
	private final MessageDrivenRunner worker;

	// derived parameters
	private final int lockRefreshFrequencyMS;
	private final String queueUrl;

	// local state
	private long nextRefreshTimeMS;
	private StackState state;;
	private ConcurrentProgressCallback lockCallback;
	private List<WorkerJob> runningJobs;
	
	ConcurrentWorkerStack(){
		singleton = null;
		canRunInReadOnly = false;
		semaphoreLockKey = null;
		semaphoreMaxLockCount = -1;
		semaphoreLockAndMessageVisibilityTimeoutSec = -1;
		maxThreadsPerMachine = -1;
		worker = null;
		lockRefreshFrequencyMS = -1;
		queueUrl = null;
	};

	private ConcurrentWorkerStack(ConcurrentSingleton singleton, Boolean canRunInReadOnly, String semaphoreLockKey,
			Integer semaphoreMaxLockCount, Integer semaphoreLockAndMessageVisibilityTimeoutSec,
			Integer maxThreadsPerMachine, MessageDrivenRunner worker, String queueName) {
		super();
		ValidateArgument.required(singleton, "singleton");
		ValidateArgument.required(semaphoreLockKey, "semaphoreLockKey");
		ValidateArgument.required(semaphoreMaxLockCount, "semaphoreMaxLockCount");
		ValidateArgument.requirement(semaphoreMaxLockCount >= 1, "semaphoreMaxLockCount must be greater than or equals to 1.");
		ValidateArgument.required(semaphoreLockAndMessageVisibilityTimeoutSec,
				"semaphoreLockAndMessageVisibilityTimeoutSec");
		ValidateArgument.requirement(semaphoreLockAndMessageVisibilityTimeoutSec >= 30,
				"semaphoreLockAndMessageVisibilityTimeoutSec must be greater than or equal to 30 seconds.");
		ValidateArgument.required(maxThreadsPerMachine, "maxThreadsPerMachine");
		ValidateArgument.requirement(maxThreadsPerMachine >= 1,
				"maxThreadsPerMachine must be greater than or equal to 1.");
		ValidateArgument.required(worker, "worker");
		ValidateArgument.required(queueName, "queueName");

		this.singleton = singleton;
		this.canRunInReadOnly = Boolean.TRUE.equals(canRunInReadOnly);
		this.semaphoreLockKey = semaphoreLockKey;
		this.semaphoreMaxLockCount = semaphoreMaxLockCount;
		this.semaphoreLockAndMessageVisibilityTimeoutSec = semaphoreLockAndMessageVisibilityTimeoutSec;
		this.maxThreadsPerMachine = maxThreadsPerMachine;
		this.worker = worker;
		this.lockRefreshFrequencyMS = (semaphoreLockAndMessageVisibilityTimeoutSec * 1000) / 3;
		this.queueUrl = singleton.getSqsQueueUrl(queueName);
	}

	/**
	 * The main entry point for the stack. This method should be called from a timer
	 * thread. Unless an {@link InterruptedException} is encountered, this method
	 * will run indefinitely.
	 */
	@Override
	public void run() {
		resetAllState();
		if (!canProcessMoreMessages()) {
			return;
		}
		singleton.runWithSemaphoreLock(semaphoreLockKey, semaphoreLockAndMessageVisibilityTimeoutSec,
				semaphoreMaxLockCount, lockCallback, () -> {
					try {
						infiniteLoop();
					} catch (Exception e) {
						log.error("Failed:", e);
					}
				});
	}

	void infiniteLoop() {
		while (shouldContinueRunning()) {
			refreshLocksIfNeeded();
			checkRunningJobs();
			attemptToAddMoreWorkers();
			try {
				singleton.sleep(1000);
			} catch (InterruptedException e) {
				startShutdown();
			}
		}
	}

	/**
	 * Reset all state.
	 */
	void resetAllState() {
		state = StackState.CONTINUE;
		runningJobs = new ArrayList<>(semaphoreMaxLockCount);
		lockCallback = new ConcurrentProgressCallback(semaphoreLockAndMessageVisibilityTimeoutSec);
		resetNextRefreshTimeMS();
	}

	/**
	 * Start the shutdown process for this stack. The stack will no longer add get
	 * new messages and start new worker threads. However, the stack will continue
	 * to wait for the existing jobs to complete.
	 */
	void startShutdown() {
		log.info("Interrupted. Will shutdown after all existing jobs finish running.");
		state = StackState.SHUT_DOWN;
	}

	/**
	 * Can this stack process more messages?
	 * 
	 */
	boolean canProcessMoreMessages() {
		if (StackState.SHUT_DOWN.equals(state)) {
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

	/**
	 * The loop should continue to run if this returns true.
	 * 
	 * @return True if runningJobs.size > 1 || State.RUNNING.equals(state)
	 */
	boolean shouldContinueRunning() {
		if (!runningJobs.isEmpty()) {
			return true;
		}
		return StackState.CONTINUE.equals(state);
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
					log.info("Interrupted. Will shutdown after all jobs finish running.");
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
				semaphoreLockAndMessageVisibilityTimeoutSec, worker));
	}
	
	

	/**
	 * If now() >= nextRefreshTimeMS, then trigger the refresh of all
	 * locks/messages.
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
	
	ConcurrentProgressCallback getLockCallback() {
		return this.lockCallback;
	}
	
	StackState getState() {
		return state;
	}
	
	List<WorkerJob> getRunningJobs(){
		return runningJobs;
	}
	
	long getNextRefreshTimeMS() {
		return this.nextRefreshTimeMS;
	}
	
	long getLockRefreshFrequencyMS() {
		return lockRefreshFrequencyMS;
	}

	/**
	 * Possible states for this stack.
	 *
	 */
	static enum StackState {
		CONTINUE, SHUT_DOWN
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

		private ConcurrentSingleton singleton;
		private Boolean canRunInReadOnly;
		private String semaphoreLockKey;
		private Integer semaphoreMaxLockCount;
		private Integer semaphoreLockAndMessageVisibilityTimeoutSec;
		private Integer maxThreadsPerMachine;
		private String queueName;
		private MessageDrivenRunner worker;

		/**
		 * Wrapper of all of the stack's dependencies. Note: {@link ConcurrentSingleton}
		 * does not encapsulate any worker state.
		 * 
		 * @param singleton
		 * @return
		 */
		public Builder withSingleton(ConcurrentSingleton singleton) {
			this.singleton = singleton;
			return this;
		}

		/**
		 * Can this worker run while the stack is in read-only mode? Defaults to false.
		 * 
		 * @param canRunInReadOnly
		 * @return
		 */
		public Builder withCanRunInReadOnly(Boolean canRunInReadOnly) {
			this.canRunInReadOnly = canRunInReadOnly;
			return this;
		}

		/**
		 * Both the semaphoreLockKey and semaphoreMaxLockCount are used to limit the
		 * number of machines across the cluster that can run this type of worker at any
		 * given time. For example, if semaphoreLockKey='someWorker' and
		 * semaphoreMaxLockCount=3, then the semaphore system will ensure that no more
		 * than three instances of 'someWorker' will be running in the worker cluster at
		 * one time. Note: Each instance can run multiple threads if
		 * maxThreadsPerMachine is greater than one.
		 * 
		 * @param semaphoreLockKey
		 * @return
		 */
		public Builder withSemaphoreLockKey(String semaphoreLockKey) {
			this.semaphoreLockKey = semaphoreLockKey;
			return this;
		}

		/**
		 * See: {@link Builder#withSemaphoreLockKey(String)}
		 */
		public Builder withSemaphoreMaxLockCount(Integer semaphoreMaxLockCount) {
			this.semaphoreMaxLockCount = semaphoreMaxLockCount;
			return this;
		}

		/**
		 * Sets the semaphore lock timeout and SQS messages visibility timeout in
		 * seconds. The stack will automatically refresh all locks/visibility at
		 * frequency of one third of this timeout. This timeout must be greater than or
		 * equal to 30 seconds. Required.
		 * 
		 * @param semaphoreLockAndMessageVisibilityTimeoutSec
		 * @return
		 */
		public Builder withSemaphoreLockAndMessageVisibilityTimeoutSec(
				Integer semaphoreLockAndMessageVisibilityTimeoutSec) {
			this.semaphoreLockAndMessageVisibilityTimeoutSec = semaphoreLockAndMessageVisibilityTimeoutSec;
			return this;
		}

		/**
		 * Sets the maximum number of threads per machine that will be used to run this
		 * worker. Must be greater than or equal to one. Required.
		 * 
		 * @param maxThreadsPerMachine
		 * @return
		 */
		public Builder withMaxThreadsPerMachine(Integer maxThreadsPerMachine) {
			this.maxThreadsPerMachine = maxThreadsPerMachine;
			return this;
		}

		/**
		 * The {@link MessageDrivenRunner} is the worker that will be invoke to process
		 * each SQS messages polled by this worker stack. The worker should be a
		 * thread-safe singleton. Required.
		 * 
		 * @param worker
		 * @return
		 */
		public Builder withWorker(MessageDrivenRunner worker) {
			this.worker = worker;
			return this;
		}
		
		/**
		 * The full name of the SQS queue that will provide messages for this worker stack.
		 * @param queueName
		 * @return
		 */
		public Builder withQueueName(String queueName) {
			this.queueName = queueName;
			return this;
		}

		public ConcurrentWorkerStack build() {
			return new ConcurrentWorkerStack(singleton, canRunInReadOnly, semaphoreLockKey, semaphoreMaxLockCount,
					semaphoreLockAndMessageVisibilityTimeoutSec, maxThreadsPerMachine, worker, queueName);
		}
	}

}
