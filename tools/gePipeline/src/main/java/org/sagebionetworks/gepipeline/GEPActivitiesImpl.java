package org.sagebionetworks.gepipeline;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.sagebionetworks.workflow.Notification;

/**
 * 
 */
public class GEPActivitiesImpl implements GEPActivities {

	/**
	 * @param s
	 * @return string url encoded but with + as %20
	 */
	public static String formatAsScriptParam(String s) {
		// so let's try URLEncoding the param. This means we must URLDecode on
		// the R side
		try {
			// R's URLdecode expects %20 for space, not +
			return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String processData(String script, String activityInput) {
		ScriptResult result = ScriptProcessor.doProcess(script, Arrays
				.asList(new String[] { GEPWorkflow.INPUT_DATA_PARAMETER_KEY,
						formatAsScriptParam(activityInput), }));
		
		// TODO stdout and stderr are no longer in the workflow history, this needs to be fixed
		// Truncate stdout and stderr to a more reasonable size so that we fit
		// withing the workflow state constraints
		/*
		stdout = (MAX_SCRIPT_OUTPUT > scriptResult.getStdout().length()) ? scriptResult
				.getStdout()
				: scriptResult.getStdout().substring(0, MAX_SCRIPT_OUTPUT);
		stderr = (MAX_SCRIPT_OUTPUT > scriptResult.getStderr().length()) ? scriptResult
				.getStderr()
				: scriptResult.getStderr().substring(0, MAX_SCRIPT_OUTPUT);
		*/
		return result.getStringResult(ScriptResult.OUTPUT_JSON_KEY);
	}

	@Override
	public void notifyFollower(String recipient, String subject, String message) {
		Notification.doSnsNotifyFollowers(GEPWorkflowConfigHelper
				.getSNSClient(), recipient, subject, message);
	}

}
