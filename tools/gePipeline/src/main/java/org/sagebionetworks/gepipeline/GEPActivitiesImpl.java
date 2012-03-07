package org.sagebionetworks.gepipeline;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.json.JSONException;
import org.sagebionetworks.workflow.Notification;
import org.sagebionetworks.workflow.ActivityScriptResult;
import org.sagebionetworks.workflow.ScriptProcessor;
import org.sagebionetworks.workflow.ScriptResult;
import org.sagebionetworks.workflow.UnrecoverableException;

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
	public ActivityScriptResult processData(String script, String activityInput)
			throws IOException, InterruptedException, UnrecoverableException,
			JSONException {

		ScriptResult scriptResult = ScriptProcessor.runSynapseScript(
				GEPWorkflowConfigHelper.getConfig(), script, Arrays
						.asList(new String[] {
								GEPWorkflow.INPUT_DATA_PARAMETER_KEY,
								formatAsScriptParam(activityInput), }));

		// Dev Note: we cannot return an instance of ScriptResult from an
		// activity because it is not serializable by SWF in its current form

		ActivityScriptResult activityResult = new ActivityScriptResult();
		activityResult.setResult(scriptResult
				.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
		activityResult.setStdout(scriptResult.getStdout());
		activityResult.setStderr(scriptResult.getStderr());

		return activityResult;
	}

	@Override
	public void notifyFollower(String recipient, String subject, String message) {
		Notification.doSnsNotifyFollowers(GEPWorkflowConfigHelper
				.getSNSClient(), recipient, subject, message);
	}

}
