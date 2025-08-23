package org.sagebionetworks.repo.model.grid.node;

import org.json.JSONArray;

public class ConstantUtils {

	/**
	 * Given a constant value, convert it to JSON by inserting into a JSONArray.
	 * 
	 * @param value
	 * @return
	 */
	public static String constantValueToJson(Object value) {
		if (value == null) {
			return "[]";
		}
		return new JSONArray().put(value).toString();
	}

}
