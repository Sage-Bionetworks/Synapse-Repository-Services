package org.sagebionetworks.workflow.curation.activity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ProcessTcgaSourceLayer {

	private static final Logger log = Logger
			.getLogger(ProcessTcgaSourceLayer.class.getName());

	private static final String INPUT_LAYER_PARAMETER_KEY = "--layerId";
	private static final String LOCAL_FILEPATH_INPUT_PARAMETER_KEY = "--localFilepath";

	private static final String OUTPUT_LAYER_JSON_KEY = "layerId";
	private static final Pattern OUTPUT_DELIMITER_PATTERN = Pattern.compile(
			"TcgaWorkflowResult_START(.*)TcgaWorkflowResult_END",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

	public static class ScriptResult {

		int processedLayerId;
		String stdout;
		String stderr;

		public int getProcessedLayerId() {
			return processedLayerId;
		}

		public void setProcessedLayerId(int processedLayerId) {
			this.processedLayerId = processedLayerId;
		}

		public String getStdout() {
			return stdout;
		}

		public void setStdout(String stdout) {
			this.stdout = stdout;
		}

		public String getStderr() {
			return stderr;
		}

		public void setStderr(String stderr) {
			this.stderr = stderr;
		}
	}

	public static ScriptResult doProcessTcgaSourceLayer(String script,
			Integer rawLayerId, String localFilepath) throws Exception {

		String scriptInput[] = new String[] { script,
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
			throw new Exception("activity failed(" + returnCode + ": "
					+ stderrAccumulator);
		}

		Matcher resultMatcher = OUTPUT_DELIMITER_PATTERN.matcher(scriptResult
				.getStdout());
		if (resultMatcher.matches()) {
			JSONObject result = new JSONObject(resultMatcher.group(1));
			if (result.has(OUTPUT_LAYER_JSON_KEY)) {
				scriptResult.setProcessedLayerId(result.getInt(OUTPUT_LAYER_JSON_KEY));
			}
		}
		return scriptResult;
	}
}
