package org.sagebionetworks.worker.config;

import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

public class WorkerTriggerBuilder {

	private SimpleTriggerFactoryBean triggerFactory;
	private MethodInvokingJobDetailFactoryBean jobDetailFactory;
	private long startDelay;
	private long repeatInterval;
	private Object targetObject;
	
	public WorkerTriggerBuilder() {
		this.triggerFactory = new SimpleTriggerFactoryBean();
		this.jobDetailFactory = new MethodInvokingJobDetailFactoryBean();
	}
	
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
		
	public SimpleTriggerFactoryBean build() {
		ValidateArgument.required(targetObject, "A stack");
		ValidateArgument.required(startDelay, "The startDelay");
		ValidateArgument.required(repeatInterval, "The repeatInterval");
		
		jobDetailFactory.setConcurrent(false);
		jobDetailFactory.setTargetMethod("run");
		jobDetailFactory.setTargetObject(targetObject);
		
		try {
			// Invoke the afterPropertiesSet here since this is not an exposed bean
			jobDetailFactory.afterPropertiesSet();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		triggerFactory.setRepeatInterval(repeatInterval);
		triggerFactory.setStartDelay(startDelay);
		triggerFactory.setJobDetail(jobDetailFactory.getObject());
		
		return triggerFactory;
	}

}
