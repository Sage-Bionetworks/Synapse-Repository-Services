package org.sagebionetworks.workflow.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.workflow.UnrecoverableException;

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

	private static final String INPUT_DATASET_PARAMETER_KEY = "--datasetId";
	private static final String INPUT_LAYER_PARAMETER_KEY = "--layerId";
	private static final String LOCAL_FILEPATH_INPUT_PARAMETER_KEY = "--localFilepath";

	private static final String OUTPUT_LAYER_JSON_KEY = "layerId";
	private static final Pattern OUTPUT_DELIMITER_PATTERN = Pattern.compile(
			"TcgaWorkflowResult_START(.*)TcgaWorkflowResult_END",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

	/**
	 * @author deflaux
	 * 
	 */
	public static class ScriptResult {

		int outputLayerId;
		String stdout;
		String stderr;

		/**
		 * @return the layer id for the layer newly created by this processing
		 *         step
		 */
		public int getProcessedLayerId() {
			return outputLayerId;
		}

		/**
		 * @param processedLayerId
		 */
		public void setProcessedLayerId(int processedLayerId) {
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
	 * @param localFilepath
	 * @return the ScriptResult
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws UnrecoverableException
	 * @throws JSONException
	 */
	public static ScriptResult doProcessLayer(String script, Integer datasetId,
			Integer rawLayerId, String localFilepath) throws IOException,
			InterruptedException, UnrecoverableException, JSONException {

		String scriptInput[] = new String[] { script,
				INPUT_DATASET_PARAMETER_KEY, datasetId.toString(),
				INPUT_LAYER_PARAMETER_KEY, rawLayerId.toString(),
				LOCAL_FILEPATH_INPUT_PARAMETER_KEY, localFilepath };

		ScriptResult scriptResult = new ScriptResult();

		// TODO
		// When these R scripts are run via this workflow, a value will be
		// passed in for --localFilepath, the script will get the layer metadata
		// from the repository service which includes the md5 checksum and it
		// will confirm those match before proceeding.
		//
		// When these R scripts are run by hand via scientists, the lack of
		// localFilepath will cause the file to be downloaded from S3url in the
		// layer metadata and stored in the local R file cache. The scientists
		// will work this way when developing new scripts or modifying existing
		// scripts.

		log.debug("Running: " + script);
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
			throw new UnrecoverableException("activity failed(" + returnCode
					+ ": " + stderrAccumulator);
		}

		Matcher resultMatcher = OUTPUT_DELIMITER_PATTERN.matcher(scriptResult
				.getStdout());
		if (resultMatcher.matches()) {
			JSONObject result = new JSONObject(resultMatcher.group(1));
			if (result.has(OUTPUT_LAYER_JSON_KEY)) {
				scriptResult.setProcessedLayerId(result
						.getInt(OUTPUT_LAYER_JSON_KEY));
			}
		}
		return scriptResult;
	}
}
