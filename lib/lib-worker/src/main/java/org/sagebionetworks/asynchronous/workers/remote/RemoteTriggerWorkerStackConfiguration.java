package org.sagebionetworks.asynchronous.workers.remote;

import org.sagebionetworks.workers.util.Gate;

public class RemoteTriggerWorkerStackConfiguration {
	
	private String queueName;
	private String lockKey;
	private SQSMessageProvider messageProvider;
	private Gate gate;

	/**
	 * @param queueName The name of the queue where the message will be sent, this name should be stack agnostic
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	/**
	 * @return The name of the queue where the message will be sent, this name should be stack agnostic
	 */
	public String getQueueName() {
		return queueName;
	}
	
	/**
	 * @param lockKey The name of the lock key for this worker, the worker will be limited to one concurrent execution on a stack
	 */
	public void setLockKey(String lockKey) {
		this.lockKey = lockKey;
	}
	
	/**
	 * @return The name of the lock key for this worker, the worker will be limited to one concurrent execution on a stack
	 */
	public String getLockKey() {
		return lockKey;
	}
	
	/**
	 * @param messageProvider An implementation of an {@link SQSMessageProvider} that supplies the message to be sent
	 */
	public void setMessageProvider(SQSMessageProvider messageProvider) {
		this.messageProvider = messageProvider;
	}
	
	/**
	 * @return An implementation of an {@link SQSMessageProvider} that supplies the message to be sent
	 */
	public SQSMessageProvider getMessageProvider() {
		return messageProvider;
	}
	
	public Gate getGate() {
		return gate;
	}
	
	public void setGate(Gate gate) {
		this.gate = gate;
	}
}
