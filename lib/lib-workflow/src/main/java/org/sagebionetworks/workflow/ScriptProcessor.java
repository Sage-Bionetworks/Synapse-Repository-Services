package org.sagebionetworks.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.utils.ExternalProcessHelper;
import org.sagebionetworks.utils.ExternalProcessHelper.ExternalProcessResult;

/**
 * Generic Script runner
 * 
 * @author deflaux
 * 
 */
public class ScriptProcessor {
	private static final Logger log = Logger.getLogger(ScriptProcessor.class
			.getName());

	private static final String R_SCRIPT_REGEXP = ".*\\.[rR]$";
	private static final String R_ARGS_DELIMITER = "--args";

	private static final String SYNAPSE_USERNAME_KEY = "--username";
	private static final String SYNAPSE_SECRETKEY_KEY = "--secretKey";
	private static final String AUTH_ENDPOINT_KEY = "--authEndpoint";
	private static final String REPO_ENDPOINT_KEY = "--repoEndpoint";

	private static final String VERBOSE_OPTION = "--verbose";

	/**
	 * Run a particular script that interacts with Synapse
	 * 
	 * @param config
	 * @param script
	 * @param scriptParams
	 *            -- attr, value pairs of params, e.g.
	 *            INPUT_DATASET_PARAMETER_KEY, datasetId.toString()
	 * @return the ScriptResult
	 * @throws JSONException 
	 * @throws UnrecoverableException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static ScriptResult runSynapseScript(
			WorkflowTemplatedConfiguration config, String script,
			List<String> scriptParams) throws IOException, InterruptedException, UnrecoverableException, JSONException {

		if (null == scriptParams) {
			scriptParams = new ArrayList<String>();
		}

		scriptParams.add(SYNAPSE_USERNAME_KEY);
		scriptParams.add(config.getSynapseUsername());
		scriptParams.add(SYNAPSE_SECRETKEY_KEY);
		scriptParams.add(config.getSynapseSecretKey());
		scriptParams.add(AUTH_ENDPOINT_KEY);
		scriptParams.add(config.getAuthenticationServicePublicEndpoint());
		scriptParams.add(REPO_ENDPOINT_KEY);
		scriptParams.add(config.getRepositoryServiceEndpoint());
		return runScript(config, script, scriptParams);
	}

	/**
	 * Run a particular script (e.g., python or R)
	 * 
	 * @param config
	 * @param script
	 * @param scriptParams
	 *            -- attr, value pairs of params, e.g.
	 *            INPUT_DATASET_PARAMETER_KEY, datasetId.toString()
	 * @return the ScriptResult
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws UnrecoverableException 
	 * @throws JSONException 
	 */
	public static ScriptResult runScript(WorkflowTemplatedConfiguration config,
			String script, List<String> scriptParams) throws IOException, InterruptedException, UnrecoverableException, JSONException {

		List<String> scriptInput = new ArrayList<String>();

		if (script.matches(R_SCRIPT_REGEXP)) {
			scriptInput.add(config.getRScriptPath());
			scriptInput.add(VERBOSE_OPTION);
			scriptInput.add(script);
			scriptInput.add(R_ARGS_DELIMITER);
		} else {
			scriptInput.add(script);
		}

		if (null != scriptParams) {
			scriptInput.addAll(scriptParams);
		}

		log.debug("About to run: " + StringUtils.join(scriptInput, " "));

		String cmdLine[] = new String[scriptInput.size()];
		ExternalProcessResult result = ExternalProcessHelper
				.runExternalProcess(scriptInput.toArray(cmdLine));

		if (0 != result.getReturnCode()) {
			throw new UnrecoverableException("Activity failed("
					+ result.getReturnCode() + ") for "
					+ StringUtils.join(scriptInput, " ") + "\nstderr: "
					+ result.getStderr() + "\nstdout: " + result.getStdout());
		}
		log.debug("Finished running: " + StringUtils.join(scriptInput, " "));

		ScriptResult scriptResult = new ScriptResult(result);
		return scriptResult;
	}
}
