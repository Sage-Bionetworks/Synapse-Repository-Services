package org.sagebionetworks.asynchronous.workers.concurrent;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack.Builder;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * An extension to {@link ConcurrentWorkerStack} that uses any type of
 * {@link ChangeMessageDrivenRunner} as the worker.
 *
 */
public class ConcurrentChangeMessageWorkerStackBuilder {

	private Builder wrappedBuilder;
	private AmazonSQSClient amazonSQSClient;
	private String queueName;
	private ChangeMessageDrivenRunner changeMessageDrivenRunner;

	/**
	 * See: {@link Builder#withSingleton(ConcurrentSingleton)}
	 * 
	 * @param singleton
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withSingleton(ConcurrentSingleton singleton) {
		ValidateArgument.required(singleton, "singleton");
		this.amazonSQSClient = singleton.getSqsClient();
		this.wrappedBuilder.withSingleton(singleton);
		return this;
	}

	/**
	 * See: {@link Builder#withCanRunInReadOnly(Boolean)}
	 * 
	 * @param canRunInReadOnly
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withCanRunInReadOnly(Boolean canRunInReadOnly) {
		this.wrappedBuilder.withCanRunInReadOnly(canRunInReadOnly);
		return this;
	}

	/**
	 * See: {@link Builder#withSemaphoreLockKey(String)}
	 * 
	 * @param semaphoreLockKey
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withSemaphoreLockKey(String semaphoreLockKey) {
		this.wrappedBuilder.withSemaphoreLockKey(semaphoreLockKey);
		return this;
	}

	/**
	 * See: {@link Builder#withSemaphoreMaxLockCount(Integer)}
	 * 
	 * @param semaphoreMaxLockCount
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withSemaphoreMaxLockCount(Integer semaphoreMaxLockCount) {
		this.wrappedBuilder.withSemaphoreMaxLockCount(semaphoreMaxLockCount);
		return this;
	}

	/**
	 * See: {@link Builder#withSemaphoreLockAndMessageVisibilityTimeoutSec(Integer)}
	 * 
	 * @param semaphoreLockAndMessageVisibilityTimeoutSec
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withSemaphoreLockAndMessageVisibilityTimeoutSec(
			Integer semaphoreLockAndMessageVisibilityTimeoutSec) {
		this.wrappedBuilder
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(semaphoreLockAndMessageVisibilityTimeoutSec);
		return this;
	}

	/**
	 * See: {@link Builder#withMaxThreadsPerMachine(Integer)}
	 * 
	 * @param maxThreadsPerMachine
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withMaxThreadsPerMachine(Integer maxThreadsPerMachine) {
		this.wrappedBuilder.withMaxThreadsPerMachine(maxThreadsPerMachine);
		return this;
	}

	/**
	 * See: {@link Builder#withQueueName(String)}
	 * 
	 * @param queueName
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withQueueName(String queueName) {
		this.queueName = queueName;
		this.wrappedBuilder.withQueueName(queueName);
		return this;
	}

	/**
	 * The worker that will handle change messages for this stack.
	 * 
	 * @param worker
	 * @return
	 */
	public ConcurrentChangeMessageWorkerStackBuilder withWorker(ChangeMessageDrivenRunner worker) {
		this.changeMessageDrivenRunner = worker;
		return this;
	}

	/**
	 * Build the stack.
	 * 
	 * @return
	 */
	public ConcurrentWorkerStack build() {
		this.wrappedBuilder
				.withWorker(new ChangeMessageBatchProcessor(amazonSQSClient, queueName, changeMessageDrivenRunner));
		return this.wrappedBuilder.build();
	}

}
