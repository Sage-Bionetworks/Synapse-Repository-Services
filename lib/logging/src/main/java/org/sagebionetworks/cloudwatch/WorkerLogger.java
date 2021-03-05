package org.sagebionetworks.cloudwatch;

import java.util.Map;

import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * Interface for logging to CloudWatch failures of asynchronous workers
 * 
 * @author brucehoff
 *
 */
public interface WorkerLogger {

	String WORKER_NAMESPACE = "Asynchronous Workers";
	String DIMENSION_WILL_RETRY = "willRetry";
	String DIMENSION_CHANGE_TYPE = "changeType";
	String DIMENSION_OBJECT_TYPE = "objectType";
	String DIMENSION_STACK_TRACE = "stackTrace";
	String DIMENSION_WORKER_CLASS = "workerClass";
	String METRIC_NAME_WORKER_TIME = "workerTime";

	/**
	 * Log a change message driven worker event. The given class name (Class.getName) will be used as
	 * the metric name. The namespace follows the pattern <{@link #WORKER_NAMESPACE} - stackInstance>,
	 * the stack trace and the willRetry will be used as dimensions. If a changeMessage is supplied its
	 * changeType and objectType will be added as dimensions.
	 * 
	 * @param workerClass
	 * @param changeMessage
	 * @param cause
	 * @param willRetry
	 */
	void logWorkerFailure(Class<? extends Object> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry);

	/**
	 * Log a generic worker failure. The namespace follows the pattern <{@link #WORKER_NAMESPACE} -
	 * stackInstance>, the stack trace and the willRetry will be used as dimensions.
	 * 
	 * @param metricName This will be main name of the metric.
	 * @param cause      The exception that caused the failure.
	 * @param willRetry  Will the worker re-try this job?
	 */
	void logWorkerFailure(String metricName, Throwable cause, boolean willRetry);

	/**
	 * Sends a cloudwatch count metric (with a value of 1) for a worker defined by the given class. The
	 * namespace follows the pattern <{@link #WORKER_NAMESPACE} - stackInstance>, the worker (simple)
	 * class name is used as the {@link #DIMENSION_WORKER_CLASS} dimension.
	 * 
	 * @param workerClass The class whose {@link Class#getSimpleName()} will be used as the workerClass
	 *                    dimension
	 * @param metricName  The name of the metric
	 */
	void logWorkerCountMetric(Class<?> workerClass, String metricName);

	/**
	 * Sends a cloudwatch metric to keep track of the time spent working. The namespace follows the
	 * pattern <{@link #WORKER_NAMESPACE} - stackInstance>, the worker (simple) class name is used as
	 * the {@link #DIMENSION_WORKER_CLASS} dimension and the {@link #METRIC_NAME_WORKER_TIME} is used as
	 * the metric name.
	 * 
	 * The map of dimensions is added to the metric if not null and not empty, note that the
	 * {@link #DIMENSION_WORKER_CLASS} will override any of the dimensions in the map with the same key.
	 * 
	 * @param workerClass
	 * @param dimensions
	 */
	void logWorkerTimeMetric(Class<?> workerClass, long timeMillis, Map<String, String> dimensions);

	/**
	 * Log a custom metric
	 * 
	 * @param pd
	 */
	void logCustomMetric(ProfileData pd);
}