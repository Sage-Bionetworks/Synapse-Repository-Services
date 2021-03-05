package org.sagebionetworks.cloudwatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

public class WorkerLoggerImpl implements WorkerLogger {

	private Consumer consumer;
	private String workersNamespace;

	@Autowired
	public WorkerLoggerImpl(Consumer consumer, StackConfiguration config) {
		this.consumer = consumer;
		this.workersNamespace = WORKER_NAMESPACE + " - " + config.getStackInstance();
		this.shouldProfile = config.getCloudWatchOnOff();

	}

	private boolean shouldProfile;

	/**
	 * Spring will inject this value.
	 * 
	 * @param shouldProfile
	 */
	public void setShouldProfile(boolean shouldProfile) {
		this.shouldProfile = shouldProfile;
	}

	/**
	 * Default no parameter ControllerProfiler constructor.
	 */
	public WorkerLoggerImpl() {
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
		ProfileData profileData = buildProfileData(workerClass, changeMessage, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}

	@Override
	public void logWorkerFailure(String metricName, Throwable cause, boolean willRetry) {
		if (!shouldProfile) {
			return;
		}
		ProfileData profileData = buildProfileData(metricName, null, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}

	@Override
	public void logWorkerMetric(Class<?> workerClass, String metricName, boolean willRetry) {
		if (!shouldProfile) {
			return;
		}

		ValidateArgument.required(workerClass, "The workerClass");
		ValidateArgument.required(metricName, "The metricName");
		
		Map<String, String> dimensions = ImmutableMap.of(
				DIMENSION_WORKER_CLASS, workerClass.getSimpleName(),
				DIMENSION_WILL_RETRY, String.valueOf(willRetry)
		);

		ProfileData profileData = buildProfileData(new Date(), metricName, dimensions);
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
	private ProfileData buildProfileData(Class<?> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry, Date timestamp) {
		return buildProfileData(workerClass.getName(), changeMessage, cause, willRetry, timestamp);
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
	private ProfileData buildProfileData(String name, ChangeMessage changeMessage, Throwable cause, boolean willRetry, Date timestamp) {
		Map<String, String> dimension = new HashMap<String, String>();
		
		dimension.put(DIMENSION_WILL_RETRY, String.valueOf(willRetry));

		if (changeMessage != null) {
			dimension.put(DIMENSION_CHANGE_TYPE, changeMessage.getChangeType().name());
			dimension.put(DIMENSION_OBJECT_TYPE, changeMessage.getObjectType().name());
		}

		String stackTraceAsString = MetricUtils.stackTracetoString(cause);

		dimension.put(DIMENSION_STACK_TRACE, stackTraceAsString);

		return buildProfileData(timestamp, name, dimension);
	}

	private ProfileData buildProfileData(Date timestamp, String metricName, Map<String, String> dimensions) {
		ProfileData data = new ProfileData();

		data.setNamespace(workersNamespace);
		data.setName(metricName);
		data.setUnit(StandardUnit.Count.name());
		data.setValue(1D);
		data.setTimestamp(timestamp);

		if (dimensions != null && !dimensions.isEmpty()) {
			data.setDimension(dimensions);
		}

		return data;
	}
}
