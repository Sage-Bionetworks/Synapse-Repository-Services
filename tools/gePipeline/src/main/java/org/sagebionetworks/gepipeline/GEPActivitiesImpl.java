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
	* Even though we are sending stdout and stderr from the R script to a log
	* file, include a portion of that output in the workflow history for
	* convenience. We only want to dig through the logs if we need to.
	*/
	private static final int MAX_SCRIPT_OUTPUT = 10240;

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
	public ProcessDataResult processData(String script, String activityInput) {

		ScriptResult scriptResult = ScriptProcessor.doProcess(script, Arrays
				.asList(new String[] { GEPWorkflow.INPUT_DATA_PARAMETER_KEY,
						formatAsScriptParam(activityInput), }));

		// Truncate stdout and stderr to a more reasonable size so that we fit
		// withing the workflow state constraints
		String stdout = (MAX_SCRIPT_OUTPUT > scriptResult.getStdout().length()) ? scriptResult
				.getStdout()
				: scriptResult.getStdout().substring(0, MAX_SCRIPT_OUTPUT);
		String stderr = (MAX_SCRIPT_OUTPUT > scriptResult.getStderr().length()) ? scriptResult
				.getStderr()
				: scriptResult.getStderr().substring(0, MAX_SCRIPT_OUTPUT);

		ProcessDataResult activityResult = new ProcessDataResult();
		activityResult.setResult(scriptResult
				.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
		activityResult.setStdout(stdout);
		activityResult.setStderr(stderr);

		return activityResult;
	}

	@Override
	public void notifyFollower(String recipient, String subject, String message) {
		Notification.doSnsNotifyFollowers(GEPWorkflowConfigHelper
				.getSNSClient(), recipient, subject, message);
	}

}
