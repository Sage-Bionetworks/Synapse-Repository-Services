package org.sagebionetworks.cloudwatch;

import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * Interface for logging to CloudWatch failures of asynchronous workers
 * 
 * @author brucehoff
 *
 */
public interface WorkerLogger {
	public void setShouldProfile(boolean shouldProfile);
	
	public void logWorkerFailure(Class<? extends Object> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry);
	
	/**
	 * Log a custom metric
	 * @param pd
	 */
	public void logCustomMetric(ProfileData pd);
	
	public void setConsumer(Consumer consumer);
}