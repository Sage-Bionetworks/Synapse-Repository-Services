package org.sagebionetworks.workflow;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.annotations.ExponentialRetry;

/**
 * @author deflaux
 * 
 */
@Activities(version=SageCommonsActivities.VERSION) 
@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
		defaultTaskStartToCloseTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS,
		defaultTaskList = SageCommonsActivities.ACTIVITIES_TASK_LIST)
public interface SageCommonsActivities {

	/**
	 * The parameter key to pass to the R script for the spreadsheet it should process
	 */
	public static final String SPREADSHEET_SCRIPT_ARG = "--spreadsheet";
		
	/**
	 * The one and only task list to use for activitites this workflow
	 */
	static final String ACTIVITIES_TASK_LIST = "activities";
	/**
	 * This version should roughly match our sprint version each time we modify this workflow
	 */
	static final String VERSION = "0.12.2"; 

	/**
	 * Retries for script processing
	 */
	static final int NUM_RETRIES = 3;
	/**
	 * First amount of time to wait before retrying
	 */
	static final int INITIAL_RETRY_INTERVAL_SECONDS = 300;
	
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	Layer getLayer(String layerId) throws SynapseException;
	
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	List<String> processSpreadsheet(String url) throws IOException, HttpClientHelperException;

	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	ActivityScriptResult runRScript(String script, String spreadsheetData) throws IOException, InterruptedException, UnrecoverableException, JSONException;

	/**
	 * @param layerId
	 * @return the body of the notification message
	 * @throws UnrecoverableException 
	 * @throws JSONException 
	 * @throws SynapseException 
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	String formulateNotificationMessage(Layer layer, Integer numJobsDispatched) throws SynapseException, JSONException, UnrecoverableException;

	/**
	 * @param recipient
	 * @param subject
	 * @param message
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	void notifyFollowers(String recipient, String subject, String message);

}
