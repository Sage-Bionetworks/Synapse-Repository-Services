package org.sagebionetworks.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.utils.ExternalProcessHelper.ExternalProcessResult;

public class ScriptResult {

	public static final String SYNAPSE_WORKFLOW_RESULT_START_DELIMITER = "SynapseWorkflowResult_START";
	public static final String SYNAPSE_WORKFLOW_RESULT_END_DELIMITER = "SynapseWorkflowResult_END";

	private static final Pattern OUTPUT_DELIMITER_PATTERN = Pattern.compile(
			".*" + SYNAPSE_WORKFLOW_RESULT_START_DELIMITER + "(.*)"
					+ SYNAPSE_WORKFLOW_RESULT_START_DELIMITER + ".*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

	public static final String OUTPUT_JSON_KEY = "output";

	JSONObject structuredOutput;

	ExternalProcessResult result;

	public ScriptResult(ExternalProcessResult result) throws JSONException {
		this.result = result;

		Matcher resultMatcher = OUTPUT_DELIMITER_PATTERN.matcher(result
				.getStdout());
		if (resultMatcher.matches()) {
			structuredOutput = new JSONObject(resultMatcher.group(1));
		}
	}

	public String getStringResult(String key) {
		String ans = null;
		if (structuredOutput.has(key)) {
			try {
				ans = structuredOutput.getString(key);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		return ans;
	}

	public List<String> getStringListResult(String key) {
		List<String> ans = new ArrayList<String>();
		if (structuredOutput.has(key)) {
			try {
				JSONArray a = structuredOutput.getJSONArray(key);
				for (int i = 0; i < a.length(); i++)
					ans.add(a.getString(i));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		return ans;
	}

	public Map<String, String> getStringMapResult(String key) {
		try {
			JSONObject map = structuredOutput.getJSONObject(key);
			List<String> gseids = Arrays.asList(JSONObject.getNames(map));
			Map<String, String> gseToDateMap = new HashMap<String, String>();
			for (String gseid : gseids)
				gseToDateMap.put(gseid, map.getString(gseid));
			return gseToDateMap;
		} catch (JSONException e) {
			throw new RuntimeException((structuredOutput == null ? null
					: structuredOutput.toString()), e);
		}
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
