package org.sagebionetworks.cloudwatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class WorkerLoggerImpl implements WorkerLogger {

	@Autowired
	Consumer consumer;
	
	private boolean shouldProfile;
	
	/**
	 * Spring will inject this value.
	 * @param shouldProfile
	 */
	public void setShouldProfile(boolean shouldProfile) {
		this.shouldProfile = shouldProfile;
	}
	
	/**
	 * Default no parameter ControllerProfiler constructor.
	 */
	public WorkerLoggerImpl(){
	}

	/**
	 * One parameter constructor for ControllerProfiler.
	 * @param consumer who receives the latency information
	 */
	public WorkerLoggerImpl(Consumer consumer){
		this.consumer = consumer;
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
	public static ProfileData makeProfileDataDTO(
			Class<?> workerClass, 
			ChangeMessage changeMessage, 
			Throwable cause, 
			boolean willRetry,
			Date timestamp) {
		return makeProfileDataDTO(workerClass.getName(), changeMessage, cause, willRetry, timestamp);
	}
	
	/**
	 * The more generic form of 
	 * @param name The name of the metric.
	 * @param changeMessage This is an optional parameter;
	 * @param cause The exception that caused this error.
	 * @param willRetry Will this work be re-tried?
	 * @param timestamp The time the error occurred.
	 * @return
	 */
	public static ProfileData makeProfileDataDTO(
			String name, 
			ChangeMessage changeMessage, 
			Throwable cause, 
			boolean willRetry,
			Date timestamp) {
		ProfileData nextPD = new ProfileData();
		nextPD.setNamespace(WORKER_NAMESPACE+" - "+ StackConfigurationSingleton.singleton().getStackInstance()); 
		nextPD.setName(name);
		nextPD.setValue(1D); // i.e. we are counting discrete events
		nextPD.setUnit("Count"); // for allowed values see http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/model/StandardUnit.html
		nextPD.setTimestamp(timestamp);
		Map<String,String> dimension = new HashMap<String, String>();
		dimension.put(WILL_RETRY_KEY, ""+willRetry);
		if(changeMessage != null){
			dimension.put(CHANGE_TYPE_KEY, changeMessage.getChangeType().name());
			dimension.put(OBJECT_TYPE_KEY, changeMessage.getObjectType().name());
		}
		String stackTraceAsString = MetricUtils.stackTracetoString(cause);
		dimension.put(STACK_TRACE_KEY, stackTraceAsString);
		nextPD.setDimension(dimension);
		
		return nextPD;
	}
	
	
	public static final String WORKER_NAMESPACE = "Asynchronous Workers";
	private static final String WILL_RETRY_KEY = "willRetry";
	private static final String CHANGE_TYPE_KEY = "changeType";
	private static final String OBJECT_TYPE_KEY = "objectType";
	private static final String STACK_TRACE_KEY = "stackTrace";
	
	/**
	 * 
	 * @param workerClass
	 * @param changeMessage
	 * @param cause can be null
	 * @param willRetry
	 */
	public void logWorkerFailure(Class<? extends Object> workerClass, ChangeMessage changeMessage, Throwable cause, boolean willRetry) {
		if (!shouldProfile) return;
		ProfileData profileData = makeProfileDataDTO(workerClass, changeMessage, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}
	
	@Override
	public void logWorkerFailure(String metricName, Throwable cause, boolean willRetry) {
		if (!shouldProfile) return;
		ProfileData profileData = makeProfileDataDTO(metricName, null, cause, willRetry, new Date());
		consumer.addProfileData(profileData);
	}
	
	/**
	 * Setter for consumer.  
	 * @param consumer
	 */
	public void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}
	
	/**
	 * Getter for consumer
	 * @return Consumer
	 */
	public Consumer getConsumer() {
		return this.consumer;
	}

	@Override
	public void logCustomMetric(ProfileData profileData) {
		if (!shouldProfile) return;
		consumer.addProfileData(profileData);
	}
}
