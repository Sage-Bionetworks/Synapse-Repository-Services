package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;

/**
 * Conversions for the free-form <code>schemaData</code> of requests and submissions, which the generated POJOs expose as
 * an {@link Object}.
 */
public final class SchemaDataJson {

	private SchemaDataJson() {
	}

	/**
	 * The schema data is a {@link JSONObjectAdapter} when it was read off the wire and a {@link JSONObject} when it was
	 * read from the database or built in code.
	 * 
	 * @return The schema data as a {@link JSONObject}, a copy when it was an adapter, null if schemaData is null
	 * @throws IllegalArgumentException If the schema data is not a JSON object
	 */
	public static JSONObject toJSONObject(Object schemaData) {
		if (schemaData == null) {
			return null;
		}
		if (schemaData instanceof JSONObject jsonObject) {
			return jsonObject;
		}
		if (schemaData instanceof JSONObjectAdapter adapter) {
			return parse(adapter.toJSONString());
		}
		throw new IllegalArgumentException("schemaData must be a JSON object.");
	}

	/**
	 * @return The JSON text of the given schema data, null if schemaData is null
	 * @throws IllegalArgumentException If the schema data is not a JSON object
	 */
	public static String toJSONString(Object schemaData) {
		JSONObject jsonObject = toJSONObject(schemaData);
		return jsonObject == null ? null : jsonObject.toString();
	}

	/**
	 * @return The schema data parsed from the given JSON text, null if the json is null
	 * @throws IllegalArgumentException If the json is not a JSON object
	 */
	public static JSONObject fromJSONString(String json) {
		return json == null ? null : parse(json);
	}

	private static JSONObject parse(String json) {
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			throw new IllegalArgumentException("schemaData must be a JSON object.", e);
		}
	}
}
