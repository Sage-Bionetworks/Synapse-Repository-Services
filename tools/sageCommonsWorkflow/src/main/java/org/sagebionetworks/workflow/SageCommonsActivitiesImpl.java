package org.sagebionetworks.workflow;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * 
 * @author deflaux
 * 
 */
public class SageCommonsActivitiesImpl implements SageCommonsActivities {

	private static final Logger log = Logger
			.getLogger(SageCommonsActivitiesImpl.class.getName());

	private static final int MAX_NUM_DEBUG_MODE_CHILD_WORKFLOWS = 5;

	SageCommonsChildWorkflowDispatcher dispatcher;

	/**
	 * @param dispatcher
	 */
	public SageCommonsActivitiesImpl(
			SageCommonsChildWorkflowDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public Data getLayer(String layerId) throws SynapseException {
		Synapse synapse = SageCommonsConfigHelper.getSynapseClient();
		return synapse.getEntity(layerId, Data.class);
	}

	@Override
	public Integer processSpreadsheet(String url) throws IOException,
			HttpClientHelperException {
		int numJobs = 0;

		URL parsedUrl = new URL(url);
		String filename = parsedUrl.getPath().replace("/", "_");

		// Download the spreadsheet
		File tempFile = File.createTempFile("sageCommons", filename);
		try {
			log.debug("Downloading: " + url);
			tempFile = HttpClientHelper.getContent(SageCommonsConfigHelper
					.getHttpClient(), url, tempFile);
			numJobs = processSpreadsheetContents(tempFile);
		} finally {
			tempFile.delete();
		}
		return numJobs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Integer processSpreadsheetContents(File file) throws IOException {
		int numJobs = 0;
		BufferedReader reader = null;

		if (file.getName().endsWith("zip")) {
			ZipFile zipFile = new ZipFile(file);
			Enumeration zipFileEntries = zipFile.entries();
			while (zipFileEntries.hasMoreElements()) {
				// grab a zip file entry
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
				BufferedInputStream inputStream = new BufferedInputStream(
						zipFile.getInputStream(entry));
				try {
					log.debug("Processing file: " + entry.getName());
					reader = new BufferedReader(new InputStreamReader(
							inputStream));
					numJobs += processSpreadsheetContents(reader);
				} finally {
					inputStream.close();
				}
			}
		} else {
			log.debug("Processing file: " + file.getName());
			reader = new BufferedReader(new FileReader(file));
			numJobs += processSpreadsheetContents(reader);
		}
		return numJobs;
	}

	int processSpreadsheetContents(BufferedReader reader) throws IOException {
		int numJobs = 0;
		// Read it line by line
		String header = reader.readLine();
		String line;
		while ((line = reader.readLine()) != null) {
			log.debug("Adding job: \n" + header + "\n" + line);
			dispatcher.dispatchRScriptChildWorkflow(SageCommonsConfigHelper
					.getWorkflowScript(), header + "\n" + line);
			numJobs++;

			if (SageCommonsConfigHelper.debugMode()
					&& MAX_NUM_DEBUG_MODE_CHILD_WORKFLOWS <= numJobs) {
				return numJobs;
			}
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
			scriptArgs.add(USERNAME_ARG);
			scriptArgs.add(SageCommonsConfigHelper.getSynapseUsername());
			scriptArgs.add(PASSWORD_ARG);
			scriptArgs.add(SageCommonsConfigHelper.getSynapsePassword());

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
	public String formulateNotificationMessage(Data layer,
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
