package org.sagebionetworks.repo.model.table.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		try {
			return new JSONObject(value).toString();
		} catch (JSONException objEx) {
			try {
				// Maybe it's an array?
				return new JSONArray(value).toString();
			} catch (JSONException arrayEx) {				
				throw new IllegalArgumentException("Invalid JSON object or array: " + objEx.getMessage() + ", " + arrayEx.getMessage());
			}
		}
	}

	@Override
	public String parseValueForDatabaseRead(String value) throws IllegalArgumentException {
		return value;
	}

}
