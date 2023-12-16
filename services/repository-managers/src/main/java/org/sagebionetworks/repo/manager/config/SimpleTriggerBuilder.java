package org.sagebionetworks.repo.manager.config;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

public class SimpleTriggerBuilder {

	private long startDelay;
	private long repeatInterval;
	private Object targetObject;
	private String targetMethod;
	
	public SimpleTriggerBuilder() {}
		
	public SimpleTriggerBuilder withStartDelay(long startDelay) {
		this.startDelay = startDelay;
		return this;
	}
	
	public SimpleTriggerBuilder withRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
		return this;
	}
	
	public SimpleTriggerBuilder withTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
		return this;
	}
	
	public SimpleTriggerBuilder withTargetObject(Object targetObject) {
		this.targetObject = targetObject;
		return this;
	}
		
	public SimpleTriggerFactoryBean build() {
		ValidateArgument.required(targetObject, "A target object");
		ValidateArgument.required(startDelay, "The startDelay");
		ValidateArgument.required(repeatInterval, "The repeatInterval");
		
		MethodInvokingJobDetailFactoryBean jobDetailFactory = new MethodInvokingJobDetailFactoryBean();		
		jobDetailFactory.setConcurrent(false);
		jobDetailFactory.setTargetMethod(targetMethod == null ? "run" : targetMethod);
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
