package org.sagebionetworks.util.json.translator;

import org.json.JSONObject;

/**
 * Functional interface to extract a value from a JSONObject.
 */
@FunctionalInterface
public interface ExtractFunction {

	/**
	 * Extract a value from the provided JSONObject using the provided key.
	 * @param key The key of the value.
	 * @param json The JSONObject to extract the value from.
	 * @return
	 */
	Object getFromJSON(String key, JSONObject json);
}
