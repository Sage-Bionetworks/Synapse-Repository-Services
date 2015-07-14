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
	
	/**
	 * Log a change message driven worker event.
	 * @param workerClass
	 * @param changeMessage
	 * @param cause
	 * @param willRetry
	 */
	public void logWorkerFailure(Class<? extends Object> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry);
	
	/**
	 * Log a generic worker failure.
	 * @param metricName This will be main name of the metric.
	 * @param cause The exception that caused the failure.
	 * @param willRetry Will the worker re-try this job? 
	 */
	public void logWorkerFailure(String metricName, Throwable cause, boolean willRetry);
	
	/**
	 * Log a custom metric
	 * @param pd
	 */
	public void logCustomMetric(ProfileData pd);
	
	public void setConsumer(Consumer consumer);
}