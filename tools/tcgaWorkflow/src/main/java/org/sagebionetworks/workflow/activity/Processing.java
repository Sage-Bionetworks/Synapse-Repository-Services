package org.sagebionetworks.workflow.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.curation.ConfigHelper;

/**
 * Workflow activities relevant to data processing.
 * 
 * The basic approach here is that R and Python scripts are typically the work
 * horses for data processing.
 * 
 * All processing takes at least one layer as input. The scripts themselves
 * should be smart enough to pull over any additional layers if needed.
 * 
 * The scripts should also be smart enough to know whether they *want* to work
 * on the input layer.
 * 
 * All successful processing outputs a new Synapse layer. Any other side effects
 * or metadata should be sent directly to Synapse.
 * 
 * All skipped processing outputs a -1 for the new layer.
 * 
 * All failed processing should return a non-zero exit code from the script to
 * indicate failure.
 * 
 * @author deflaux
 * 
 */
public class Processing {

	private static final Logger log = Logger.getLogger(Processing.class
			.getName());

	private static final String R_SCRIPT_REGEXP = ".*\\.[rR]$";
	private static final String R_ARGS_DELIMITER = "--args";

	private static final String SYNAPSE_USERNAME_KEY = "--username";
	private static final String SYNAPSE_PASSWORD_KEY = "--password";
	private static final String AUTH_ENDPOINT_KEY = "--authEndpoint";
	private static final String REPO_ENDPOINT_KEY = "--repoEndpoint";
	private static final String INPUT_DATASET_PARAMETER_KEY = "--datasetId";
	private static final String INPUT_LAYER_PARAMETER_KEY = "--layerId";

	private static final String OUTPUT_LAYER_JSON_KEY = "layerId";
	private static final Pattern OUTPUT_DELIMITER_PATTERN = Pattern.compile(
			".*SynapseWorkflowResult_START(.*)SynapseWorkflowResult_END.*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

	/**
	 * @author deflaux
	 * 
	 */
	public static class ScriptResult {

		String outputLayerId;
		String stdout;
		String stderr;

		/**
		 * @return the layer id for the layer newly created by this processing
		 *         step
		 */
		public String getProcessedLayerId() {
			return outputLayerId;
		}

		/**
		 * @param processedLayerId
		 */
		public void setProcessedLayerId(String processedLayerId) {
			this.outputLayerId = processedLayerId;
		}

		/**
		 * @return all output sent to stdout by this script
		 */
		public String getStdout() {
			return stdout;
		}

		/**
		 * @param stdout
		 */
		public void setStdout(String stdout) {
			this.stdout = stdout;
		}

		/**
		 * @return all output sent to stderr by this script
		 */
		public String getStderr() {
			return stderr;
		}

		/**
		 * @param stderr
		 */
		public void setStderr(String stderr) {
			this.stderr = stderr;
		}
	}

	/**
	 * Run a particular script on a particular layer.
	 * 
	 * @param script
	 * @param datasetId
	 * @param rawLayerId
	 * @return the ScriptResult
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws UnrecoverableException
	 * @throws JSONException
	 */
	public static ScriptResult doProcessLayer(String script, String datasetId,
			String rawLayerId) throws IOException, InterruptedException,
			UnrecoverableException, JSONException {

		String argsDelimiter = (script.matches(R_SCRIPT_REGEXP)) ? R_ARGS_DELIMITER
				: "";

		
		// http://sagebionetworks.jira.com/source/browse/PLFM/trunk/tools/tcgaWorkflow/src/main/java/org/sagebionetworks/workflow/curation/TcgaWorkflow.java?r2=3150&r1=3123
		String scriptInput[] = new String[] { script, argsDelimiter,
				SYNAPSE_USERNAME_KEY, ConfigHelper.getSynapseUsername(),
				SYNAPSE_PASSWORD_KEY, ConfigHelper.getSynapsePassword(),
				AUTH_ENDPOINT_KEY, ConfigHelper.getAuthenticationServiceEndpoint(),
				REPO_ENDPOINT_KEY, ConfigHelper.getRepositoryServiceEndpoint(),
				INPUT_DATASET_PARAMETER_KEY, datasetId.toString(),
				INPUT_LAYER_PARAMETER_KEY, rawLayerId.toString() };

		ScriptResult scriptResult = new ScriptResult();

		// TODO
		// When these R scripts are run via this workflow, the script will get
		// the layer metadata
		// from the repository service which includes the md5 checksum and it
		// will download the data and confirm those match before proceeding.
		//
		// When these R scripts are run by hand via scientists, it works the
		// exact same way, yay!

		log.debug("About to run: " + StringUtils.join(scriptInput, " "));
		Process process = Runtime.getRuntime().exec(scriptInput);
		// TODO threads for slurping in stdout and stderr

		String line;

		// Collect stdout from the script
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		StringBuilder stdoutAccumulator = new StringBuilder();
		while ((line = inputStream.readLine()) != null) {
			stdoutAccumulator.append(line);
			log.debug(line);
		}
		inputStream.close();
		scriptResult.setStdout(stdoutAccumulator.toString());

		// Collect stderr from the script
		BufferedReader errorStream = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		StringBuilder stderrAccumulator = new StringBuilder();
		while ((line = errorStream.readLine()) != null) {
			stderrAccumulator.append(line);
			log.debug(line);
		}
		errorStream.close();
		scriptResult.setStderr(stderrAccumulator.toString());

		int returnCode = process.waitFor();
		if (0 != returnCode) {
			throw new UnrecoverableException("Activity failed(" + returnCode
					+ ") for " + StringUtils.join(scriptInput, " ")
					+ " stderr: " + stderrAccumulator);
		}
		log.debug("Finished running: " + StringUtils.join(scriptInput, " "));

		Matcher resultMatcher = OUTPUT_DELIMITER_PATTERN.matcher(scriptResult
				.getStdout());
		if (resultMatcher.matches()) {
			JSONObject result = new JSONObject(resultMatcher.group(1));
			if (result.has(OUTPUT_LAYER_JSON_KEY)) {
				scriptResult.setProcessedLayerId(result
						.getString(OUTPUT_LAYER_JSON_KEY));
			}
		}
		return scriptResult;
	}
}
