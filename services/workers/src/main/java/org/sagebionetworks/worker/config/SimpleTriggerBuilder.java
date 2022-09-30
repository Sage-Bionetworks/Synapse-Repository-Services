package org.sagebionetworks.worker.config;

import org.quartz.SimpleTrigger;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * Helper to build a {@link SimpleTrigger}.
 *
 */
public class SimpleTriggerBuilder {

	private Boolean concurrent = false;;
	private String targetMethod = "run";
	private Object targetObject;
	private Long startDelay = 1000L;
	private Long repeatInterval;
	
	
	/**
	 * @param concurrent the concurrent to set
	 */
	public SimpleTriggerBuilder withConcurrent(Boolean concurrent) {
		this.concurrent = concurrent;
		return this;
	}



	/**
	 * @param targetMethod the targetMethod to set
	 */
	public SimpleTriggerBuilder withTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
		return this;
	}



	/**
	 * @param targetObject the targetObject to set
	 */
	public SimpleTriggerBuilder withTargetObject(Object targetObject) {
		this.targetObject = targetObject;
		return this;
	}



	/**
	 * @param startDelay the startDelay to set
	 */
	public SimpleTriggerBuilder withStartDelayMS(Long startDelay) {
		this.startDelay = startDelay;
		return this;
	}


	/**
	 * @param repeatInterval the repeatInterval to set
	 */
	public SimpleTriggerBuilder withRepeatIntervalMS(Long repeatInterval) {
		this.repeatInterval = repeatInterval;
		return this;
	}


	public SimpleTrigger build() {
		MethodInvokingJobDetailFactoryBean invokingBean = new MethodInvokingJobDetailFactoryBean();
		invokingBean.setConcurrent(concurrent);
		invokingBean.setTargetMethod(targetMethod);
		invokingBean.setTargetObject(targetObject);
		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(invokingBean.getObject());
		trigger.setStartDelay(startDelay);
		trigger.setRepeatInterval(repeatInterval);
		return trigger.getObject();
	}
}
