package org.sagebionetworks.util.json.translator;

import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;

public enum JSONType implements ExtractFunction {

	STRING(String.class, (k, o) -> o.getString(k)),
	LONG(Long.class, (k, o) -> o.getLong(k)),
	DOUBLE(Double.class, (k, o) -> o.getDouble(k)),
	BOOLEAN(Boolean.class, (k,o)-> o.getBoolean(k)),
	JSON_OBJECT(JSONObject.class, (k, o) -> o.getJSONObject(k));

	private JSONType(Class<?> type, ExtractFunction function) {
		this.jsonType = type;
		this.function = function;
	}

	private ExtractFunction function;
	private Class<?> jsonType;
	
	/**
	 * Lookup the JSONType enum value that matches the provided types.
	 * @param type
	 * @return
	 */
	public static JSONType lookupType(Class<?> type) {
		ValidateArgument.required(type, "type");
		for(JSONType json: JSONType.values()) {
			if(json.jsonType.equals(type)) {
				return json;
			}
		}
		throw new IllegalArgumentException("Unknown type for type: "+type.getName());
	}

	@Override
	public Object getFromJSON(String key, JSONObject json) {
		return this.function.getFromJSON(key, json);
	}

	public ExtractFunction getFunction() {
		return function;
	}

	public Class<?> getJsonType() {
		return jsonType;
	}
}
