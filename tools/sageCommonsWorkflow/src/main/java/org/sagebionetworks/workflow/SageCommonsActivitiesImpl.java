package org.sagebionetworks.workflow;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
	public List<String> processSpreadsheet(String url) throws IOException,
			HttpClientHelperException {

		// Download the spreadsheet
		File tempFile = File.createTempFile("sageCommons", "csv");
		List<String> jobs = null;
		try {
			tempFile = HttpClientHelper.getContent(SageCommonsConfigHelper
					.getHttpClient(), url, tempFile);
			jobs = processSpreadsheetContents(tempFile);
		} finally {
			tempFile.delete();
		}
		return jobs;
	}

	List<String> processSpreadsheetContents(File file) throws IOException {
		List<String> jobs = new ArrayList<String>();

		// Read it line by line, kicking off child workflows
		ZipFile zipFile = new ZipFile(file);
		Enumeration zipFileEntries = zipFile.entries();
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			BufferedInputStream inputStream = new BufferedInputStream(zipFile
		            .getInputStream(entry));
			try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String header = reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				jobs.add(header + "\n" + line);
			}
			}
			finally {
				inputStream.close();
			}
		}
		return jobs;
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
