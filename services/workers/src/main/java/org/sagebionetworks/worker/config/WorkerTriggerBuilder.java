package org.sagebionetworks.worker.config;

import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStack;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

class WorkerTriggerBuilder {

	private long startDelay;
	private long repeatInterval;
	private Object targetObject;
	
	WorkerTriggerBuilder() {}
		
	public WorkerTriggerBuilder withStartDelay(long startDelay) {
		this.startDelay = startDelay;
		return this;
	}
	
	public WorkerTriggerBuilder withRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
		return this;
	}
	
	public WorkerTriggerBuilder withStack(ConcurrentWorkerStack concurrentWorkerStack) {
		this.targetObject = concurrentWorkerStack;
		return this;
	}
	
	public WorkerTriggerBuilder withStack(SemaphoreGatedWorkerStack semaphoreGatedWorkerStack) {
		this.targetObject = semaphoreGatedWorkerStack;
		return this;
	}
	
	public WorkerTriggerBuilder withStack(MessageDrivenWorkerStack messageDrivenWorkerStack) {
		this.targetObject = messageDrivenWorkerStack;
		return this;
	}
		
	public SimpleTriggerFactoryBean build() {
		ValidateArgument.required(targetObject, "A stack");
		ValidateArgument.required(startDelay, "The startDelay");
		ValidateArgument.required(repeatInterval, "The repeatInterval");
		
		MethodInvokingJobDetailFactoryBean jobDetailFactory = new MethodInvokingJobDetailFactoryBean();		
		jobDetailFactory.setConcurrent(false);
		jobDetailFactory.setTargetMethod("run");
		jobDetailFactory.setTargetObject(targetObject);
		
		try {
			// Invoke the afterPropertiesSet here since this is not an exposed bean
			jobDetailFactory.afterPropertiesSet();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		SimpleTriggerFactoryBean triggerFactory = new SimpleTriggerFactoryBean();
		triggerFactory.setRepeatInterval(repeatInterval);
		triggerFactory.setStartDelay(startDelay);
		triggerFactory.setJobDetail(jobDetailFactory.getObject());
		
		return triggerFactory;
	}

}