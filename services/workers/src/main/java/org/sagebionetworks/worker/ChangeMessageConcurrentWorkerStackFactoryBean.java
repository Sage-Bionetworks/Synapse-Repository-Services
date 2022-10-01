package org.sagebionetworks.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentSingleton;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.springframework.beans.factory.FactoryBean;

import com.amazonaws.services.sqs.AmazonSQS;

public class ChangeMessageConcurrentWorkerStackFactoryBean implements FactoryBean<ConcurrentWorkerStack> {

	private String queueName;
	private ChangeMessageDrivenRunner worker;
	private AmazonSQS amazonSQS;
	private ConcurrentWorkerStack.Builder builder = ConcurrentWorkerStack.builder();
	
	public void setSingleton(ConcurrentSingleton singleton) {
		amazonSQS = singleton.getAmazonSQSClient();
		builder.withSingleton(singleton);
	}

	public void setCanRunInReadOnly(Boolean canRunInReadOnly) {
		builder.withCanRunInReadOnly(canRunInReadOnly);
	}

	public void setSemaphoreLockKey(String semaphoreLockKey) {
		builder.withSemaphoreLockKey(semaphoreLockKey);
	}

	public void setSemaphoreMaxLockCount(Integer semaphoreMaxLockCount) {
		builder.withSemaphoreMaxLockCount(semaphoreMaxLockCount);
	}

	public void setSemaphoreLockAndMessageVisibilityTimeoutSec(
			Integer semaphoreLockAndMessageVisibilityTimeoutSec) {
		builder.withSemaphoreLockAndMessageVisibilityTimeoutSec(semaphoreLockAndMessageVisibilityTimeoutSec);
	}

	public void setMaxThreadsPerMachine(Integer maxThreadsPerMachine) {
		builder.withMaxThreadsPerMachine(maxThreadsPerMachine);
	}

	public void setWorker(ChangeMessageDrivenRunner worker) {
		this.worker = worker;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
		builder.withQueueName(queueName);
	}
	
	@Override
	public ConcurrentWorkerStack getObject() throws Exception {
		builder.withWorker(new ChangeMessageBatchProcessor(amazonSQS, queueName, worker));
		return builder.build();
	}

	@Override
	public Class<?> getObjectType() {
		return ConcurrentWorkerStack.class;
	}

}
