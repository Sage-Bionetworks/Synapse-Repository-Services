package org.sagebionetworks.workflow.activity;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.utils.ExternalProcessHelper;
import org.sagebionetworks.utils.ExternalProcessHelper.ExternalProcessResult;
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
		ExternalProcessResult result;
		
		public ScriptResult(ExternalProcessResult result) throws JSONException {
			this.result = result;
			
			Matcher resultMatcher = OUTPUT_DELIMITER_PATTERN.matcher(result.getStdout());
			if (resultMatcher.matches()) {
				JSONObject structuredOutput = new JSONObject(resultMatcher.group(1));
				if (structuredOutput.has(OUTPUT_LAYER_JSON_KEY)) {
					setProcessedLayerId(structuredOutput.getString(OUTPUT_LAYER_JSON_KEY));
				}
			}
		}
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
			return result.getStdout();
		}

		/**
		 * @return all output sent to stderr by this script
		 */
		public String getStderr() {
			return result.getStderr();
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

		String scriptInput[];
		
		if (script.matches(R_SCRIPT_REGEXP)) {
			String argsDelimiter = R_ARGS_DELIMITER;
			String rScriptPath = ConfigHelper.getRScriptPath();

			scriptInput = new String[] {rScriptPath, script, argsDelimiter,
					SYNAPSE_USERNAME_KEY, ConfigHelper.getSynapseUsername(),
					SYNAPSE_PASSWORD_KEY, ConfigHelper.getSynapsePassword(),
					AUTH_ENDPOINT_KEY, ConfigHelper.getAuthenticationServicePrivateEndpoint(),
					REPO_ENDPOINT_KEY, ConfigHelper.getRepositoryServiceEndpoint(),
					INPUT_DATASET_PARAMETER_KEY, datasetId.toString(),
					INPUT_LAYER_PARAMETER_KEY, rawLayerId.toString() };
		}
		else {
			scriptInput = new String[] {script, 
					SYNAPSE_USERNAME_KEY, ConfigHelper.getSynapseUsername(),
					SYNAPSE_PASSWORD_KEY, ConfigHelper.getSynapsePassword(),
					AUTH_ENDPOINT_KEY, ConfigHelper.getAuthenticationServicePrivateEndpoint(),
					REPO_ENDPOINT_KEY, ConfigHelper.getRepositoryServiceEndpoint(),
					INPUT_DATASET_PARAMETER_KEY, datasetId.toString(),
					INPUT_LAYER_PARAMETER_KEY, rawLayerId.toString() };
		}

		// TODO
		// When these R scripts are run via this workflow, the script will get
		// the layer metadata
		// from the repository service which includes the md5 checksum and it
		// will download the data and confirm those match before proceeding.
		//
		// When these R scripts are run by hand via scientists, it works the
		// exact same way, yay!

		log.debug("About to run: " + StringUtils.join(scriptInput, " "));
		ExternalProcessResult result = ExternalProcessHelper.runExternalProcess(scriptInput);

		if (0 != result.getReturnCode()) {
			throw new UnrecoverableException("Activity failed(" + result.getReturnCode()
					+ ") for " + StringUtils.join(scriptInput, " ")
					+ " stderr: " + result.getStderr());
		}
		log.debug("Finished running: " + StringUtils.join(scriptInput, " "));

		ScriptResult scriptResult = new ScriptResult(result);
		
		return scriptResult;
	}
}
