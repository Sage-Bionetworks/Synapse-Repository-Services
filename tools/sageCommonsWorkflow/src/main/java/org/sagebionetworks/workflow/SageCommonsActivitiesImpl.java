package org.sagebionetworks.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * 
 * @author deflaux
 * 
 */
public class SageCommonsActivitiesImpl implements SageCommonsActivities {

	@Override
	public Layer getLayer(String layerId) throws SynapseException {
		Synapse synapse = SageCommonsConfigHelper.getSynapseClient();
		return synapse.getEntity(layerId, Layer.class);
	}

	@Override
	public Integer processSpreadsheet(String url) throws IOException,
			HttpClientHelperException {

		// Download the spreadsheet
		File tempFile = File.createTempFile("sageCommons", "csv");
		int numJobs = 0;
		try {
			tempFile = HttpClientHelper.getContent(SageCommonsConfigHelper
					.getHttpClient(), url, tempFile);
			numJobs = processSpreadsheetContents(tempFile);
		} finally {
			tempFile.delete();
		}
		return numJobs;
	}

	Integer processSpreadsheetContents(File file) throws IOException {
		int numJobs = 0;
		// Get a factory for these child workflows
		SageCommonsRScriptWorkflowClientFactory clientFactory = new SageCommonsRScriptWorkflowClientFactoryImpl();

		// Read it line by line, kicking off child workflows
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String header = reader.readLine();
		String line;
		while ((line = reader.readLine()) != null) {
			SageCommonsRScriptWorkflowClient childWorkflow = clientFactory
					.getClient();
			childWorkflow.runRScript(SAGE_COMMONS_SCRIPT, header + "\n" + line);
			numJobs++;
		}
		return numJobs;
	}

	@Override
	public ActivityScriptResult runRScript(String script, String spreadsheetData)
			throws IOException, InterruptedException, UnrecoverableException,
			JSONException {

		ScriptResult scriptResult = null;
		File tempFile = File.createTempFile("sageCommonsJob", "csv");

		try {
			FileWriter writer = new FileWriter(tempFile);
			writer.write(spreadsheetData);
			writer.close();

			List<String> scriptArgs = new ArrayList<String>();
			scriptArgs.add(SPREADSHEET_SCRIPT_ARG);
			scriptArgs.add(tempFile.getAbsolutePath());

			scriptResult = ScriptProcessor.runScript(SageCommonsConfigHelper
					.getConfig(), script, scriptArgs);

		} finally {
			tempFile.delete();
		}
		
		ActivityScriptResult activityResult = new ActivityScriptResult();
		activityResult.setStdout(scriptResult.getStdout());
		activityResult.setStderr(scriptResult.getStderr());

		return activityResult;
	}

	@Override
	public String formulateNotificationMessage(Layer layer,
			Integer numJobsDispatched) throws SynapseException, JSONException,
			UnrecoverableException {

		String message = "Dispatched all " + numJobsDispatched
				+ " jobs found in " + layer.getName();
		return message;
	}

	@Override
	public void notifyFollowers(String recipient, String subject, String message) {
		// Reenable this once the method is implemented
		// Notification.doEmailNotifyFollower(recipient, subject, message);
	}

}
