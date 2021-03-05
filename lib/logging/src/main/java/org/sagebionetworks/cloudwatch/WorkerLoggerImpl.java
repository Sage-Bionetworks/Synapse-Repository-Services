package org.sagebionetworks.cloudwatch;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class WorkerLoggerImpl implements WorkerLogger {

	private Consumer consumer;
	private String workersNamespace;
	private boolean shouldProfile;

	@Autowired
	public WorkerLoggerImpl(Consumer consumer, StackConfiguration config) {
		this.consumer = consumer;
		this.workersNamespace = WORKER_NAMESPACE + " - " + config.getStackInstance();
		this.shouldProfile = config.getCloudWatchOnOff();
	}

	/**
	 * 
	 * @param workerClass
	 * @param changeMessage
	 * @param cause         can be null
	 * @param willRetry
	 */
	@Override
	public void logWorkerFailure(Class<? extends Object> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry) {
		if (!shouldProfile) {
			return;
		}
		ProfileData profileData = buildCountProfileData(workerClass, changeMessage, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}

	@Override
	public void logWorkerFailure(String metricName, Throwable cause, boolean willRetry) {
		if (!shouldProfile) {
			return;
		}
		ProfileData profileData = buildCountProfileData(metricName, null, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}

	@Override
	public void logWorkerCountMetric(Class<?> workerClass, String metricName) {
		ValidateArgument.required(workerClass, "The workerClass");
		ValidateArgument.required(metricName, "The metricName");
		
		Map<String, String> dimensions = Collections.singletonMap(DIMENSION_WORKER_CLASS, workerClass.getSimpleName());

		ProfileData profileData = buildCountProfileData(new Date(), metricName, dimensions);
		
		consumer.addProfileData(profileData);
	}
	
	@Override
	public void logWorkerTimeMetric(Class<?> workerClass, long timeMillis, Map<String, String> customDimensions) {
		ValidateArgument.required(workerClass, "The workerClass");
		
		Map<String, String> dimensions = new HashMap<>();
		
		if (customDimensions != null && !customDimensions.isEmpty()) {
			dimensions.putAll(customDimensions);
		}
		
		dimensions.put(DIMENSION_WORKER_CLASS, workerClass.getSimpleName());
		
		ProfileData profileData = buildProfileData(new Date(), METRIC_NAME_WORKER_TIME, StandardUnit.Milliseconds, Double.valueOf(timeMillis), dimensions);
		
		consumer.addProfileData(profileData);
		
	}

	@Override
	public void logCustomMetric(ProfileData profileData) {
		if (!shouldProfile) {
			return;
		}
		consumer.addProfileData(profileData);
	}
	
	/**
	 * Makes transfer object and returns it.
	 * 
	 * @param workerClass
	 * @param changeMessage
	 * @param cause
	 * @param willRetry
	 * @return
	 */
	private ProfileData buildCountProfileData(Class<?> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry, Date timestamp) {
		return buildCountProfileData(workerClass.getName(), changeMessage, cause, willRetry, timestamp);
	}

	/**
	 * The more generic form of
	 * 
	 * @param name          The name of the metric.
	 * @param changeMessage This is an optional parameter;
	 * @param cause         The exception that caused this error.
	 * @param willRetry     Will this work be re-tried?
	 * @param timestamp     The time the error occurred.
	 * @return
	 */
	private ProfileData buildCountProfileData(String name, ChangeMessage changeMessage, Throwable cause, boolean willRetry, Date timestamp) {
		Map<String, String> dimension = new HashMap<String, String>();
		
		dimension.put(DIMENSION_WILL_RETRY, String.valueOf(willRetry));

		if (changeMessage != null) {
			dimension.put(DIMENSION_CHANGE_TYPE, changeMessage.getChangeType().name());
			dimension.put(DIMENSION_OBJECT_TYPE, changeMessage.getObjectType().name());
		}

		String stackTraceAsString = MetricUtils.stackTracetoString(cause);

		dimension.put(DIMENSION_STACK_TRACE, stackTraceAsString);

		return buildCountProfileData(timestamp, name, dimension);
	}

	private ProfileData buildCountProfileData(Date timestamp, String metricName, Map<String, String> dimensions) {
		return buildProfileData(timestamp, metricName, StandardUnit.Count, 1D, dimensions);
	}
	
	private ProfileData buildProfileData(Date timestamp, String metricName, StandardUnit unit, Double value, Map<String, String> dimensions) {
		ProfileData data = new ProfileData();

		data.setNamespace(workersNamespace);
		data.setName(metricName);
		data.setUnit(unit.name());
		data.setValue(value);
		data.setTimestamp(timestamp);

		if (dimensions != null && !dimensions.isEmpty()) {
			data.setDimension(dimensions);
		}

		return data;
	}
}
