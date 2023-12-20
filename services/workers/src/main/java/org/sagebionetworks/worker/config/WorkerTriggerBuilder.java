package org.sagebionetworks.worker.config;

import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.repo.manager.config.SimpleTriggerBuilder;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStack;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

class WorkerTriggerBuilder {
	
	private SimpleTriggerBuilder builder;
	
	public WorkerTriggerBuilder() {
		builder = new SimpleTriggerBuilder();
	}
		
	public WorkerTriggerBuilder withStartDelay(long startDelay) {
		builder.withStartDelay(startDelay);
		return this;
	}
	
	public WorkerTriggerBuilder withRepeatInterval(long repeatInterval) {
		builder.withRepeatInterval(repeatInterval);
		return this;
	}
	
	public WorkerTriggerBuilder withStack(ConcurrentWorkerStack concurrentWorkerStack) {
		builder.withTargetObject(concurrentWorkerStack);
		return this;
	}
	
	public WorkerTriggerBuilder withStack(SemaphoreGatedWorkerStack semaphoreGatedWorkerStack) {
		builder.withTargetObject(semaphoreGatedWorkerStack);
		return this;
	}
	
	public WorkerTriggerBuilder withStack(MessageDrivenWorkerStack messageDrivenWorkerStack) {
		builder.withTargetObject(messageDrivenWorkerStack);
		return this;
	}
	
	public WorkerTriggerBuilder withDataSourceMonitor(DataSourcePoolMonitor dataSourceMonitor) {
		builder.withTargetObject(dataSourceMonitor);
		builder.withTargetMethod("collectMetrics");
		return this;
	}
	
	public SimpleTriggerFactoryBean build() {
		return builder.build();
	}

}