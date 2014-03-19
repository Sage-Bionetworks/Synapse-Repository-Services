package org.sagebionetworks.cloudwatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class WorkerLoggerImpl implements WorkerLogger {
	//a singleton consumer from the Spring settings file 
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
	
	private static final int MAX_STACK_TRACE_ROWS = 3;
	
	private static final String stackTraceToString(Throwable cause) {
		StringBuilder sb = new StringBuilder();
		for (int i= 0; i<cause.getStackTrace().length && i<MAX_STACK_TRACE_ROWS; i++) {
			if (i>0) sb.append("\n");
			sb.append(cause.getStackTrace()[i].toString());
		}
		return sb.toString();
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
			Class workerClass, 
			ChangeMessage changeMessage, 
			Throwable cause, 
			boolean willRetry,
			Date timestamp) {
		ProfileData nextPD = new ProfileData();
		nextPD.setNamespace(workerClass.getName()); 
		nextPD.setName(workerClass.getName()+" - "+(willRetry? WILL_RETRY_KEY:""));
		nextPD.setValue(1D); // i.e. we are counting discrete events
		nextPD.setUnit("events");
		nextPD.setTimestamp(new Date());
		Map<String,String> dimension = new HashMap<String, String>();
		dimension.put(WILL_RETRY_KEY, ""+willRetry);
		dimension.put(CHANGE_TYPE_KEY, changeMessage.getChangeType().toString());
		dimension.put(OBJECT_ID_KEY, changeMessage.getObjectId());
		dimension.put(OBJECT_TYPE_KEY, changeMessage.getObjectType().toString());
		dimension.put(STACK_TRACE_KEY, stackTraceToString(cause));
		nextPD.setDimension(dimension);
		
		return nextPD;
	}
	
	private static final String WILL_RETRY_KEY = "willRetry";
	private static final String CHANGE_TYPE_KEY = "changeType";
	private static final String OBJECT_ID_KEY = "objectId";
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
}
