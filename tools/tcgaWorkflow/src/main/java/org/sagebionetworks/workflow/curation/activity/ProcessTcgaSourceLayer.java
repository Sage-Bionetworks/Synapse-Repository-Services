package org.sagebionetworks.workflow.curation.activity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ProcessTcgaSourceLayer {

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

		ScriptResult scriptResult = new ScriptResult();

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
		Process process = Runtime.getRuntime().exec(
				new String[] { script, "--rawLayerId", rawLayerId.toString(),
						"--localFilepath", localFilepath });

		// TODO threads for slurping in stdout and stderr

		String line;

		// Collect stdout from the script
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		StringBuilder stdoutAccumulator = new StringBuilder();
		while ((line = inputStream.readLine()) != null) {
			stdoutAccumulator.append(line);
			// TODO log this
			System.out.println(line);
		}
		inputStream.close();
		scriptResult.setStdout(stdoutAccumulator.toString());

		// Collect stderr from the script
		BufferedReader errorStream = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		StringBuilder stderrAccumulator = new StringBuilder();
		while ((line = errorStream.readLine()) != null) {
			stderrAccumulator.append(line);
			// TODO log this
			System.out.println(line);
		}
		errorStream.close();
		scriptResult.setStderr(stderrAccumulator.toString());

		int returnCode = process.waitFor();
		if (0 != returnCode) {
			throw new Exception("activity failed(" + returnCode + ": "
					+ stderrAccumulator);
		}

		// TODO parse JSON output from R script sent to stdout to get the
		// processed layer id
		scriptResult.setProcessedLayerId(42);
		return scriptResult;
	}
}
