package org.sagebionetworks.workflow;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.annotations.ExponentialRetry;

/**
 * @author deflaux
 *
 */
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
	 * Script arg for Synapse username
	 */
	public static final String USERNAME_ARG = "--username";

	/**
	 * Script arg for Synapse password
	 */
	public static final String PASSWORD_ARG = "--password";
	
	/**
	 * The one and only task list to use for activitites this workflow
	 */
	static final String ACTIVITIES_TASK_LIST = "activities";
	/**
	 * This version should roughly match our sprint version each time we modify this workflow
	 */
	static final String VERSION = "0.12.5"; 

	/**
	 * Retries for script processing
	 */
	static final int NUM_RETRIES = 3;
	/**
	 * First amount of time to wait before retrying
	 */
	static final int INITIAL_RETRY_INTERVAL_SECONDS = Constants.FIVE_MINUTES_OF_SECONDS;
	
	/**
	 * Given a synapse layer id, retrieve the layer entity
	 * @param layerId
	 * @return the layer entity
	 * @throws SynapseException
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	Data getLayer(String layerId) throws SynapseException;
	
	/**
	 * Given a url to a spreadsheet from Brig's crawlers, kick off a workflow for each row
	 * 
	 * @param url
	 * @return the number of jobs kicked off
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.ONE_HOUR_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	Integer processSpreadsheet(String url) throws IOException, HttpClientHelperException;

	/**
	 * Given the path to a script and a string of spreadsheet data from Brig's crawlers, write that string to a spreadsheet on disk and call Brig's script
	 * 
	 * @param script
	 * @param spreadsheetData
	 * @return an object holding stdout and stderr from the job
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws UnrecoverableException
	 * @throws JSONException
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FOUR_HOURS_OF_SECONDS)
	@ExponentialRetry(initialRetryIntervalSeconds = INITIAL_RETRY_INTERVAL_SECONDS, maximumAttempts = NUM_RETRIES)
	ActivityScriptResult runRScript(String script, String spreadsheetData) throws IOException, InterruptedException, UnrecoverableException, JSONException;

	/**
	 * @param layer 
	 * @param numJobsDispatched 
	 * @return the body of the notification message
	 * @throws UnrecoverableException 
	 * @throws JSONException 
	 * @throws SynapseException 
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	String formulateNotificationMessage(Data layer, Integer numJobsDispatched) throws SynapseException, JSONException, UnrecoverableException;

	/**
	 * @param recipient
	 * @param subject
	 * @param message
	 */
	@Activity(version = VERSION)
	@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS, 
			defaultTaskStartToCloseTimeoutSeconds = Constants.FIVE_MINUTES_OF_SECONDS)
	void notifyFollowers(String recipient, String subject, String message);

	/**
	 * Given a spreadsheet from Brig's crawlers, kick off a workflow for each row
	 * 
	 * @param file
	 * @return the number of jobs kicked off
	 * @throws IOException
	 */
	Integer processSpreadsheetContents(File file) throws IOException;

}
