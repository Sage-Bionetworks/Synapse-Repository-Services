package org.sagebionetworks.repo.model.grid.patch;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The value of a con node is JSON-like value. The value can be any JSON value,
 * including null, true, false, numbers, strings, arrays, objects, binary blobs,
 * undefined value, and logical clock timestamp
 */
public enum ConType {

	NULL, 
	BOOLEAN(Boolean.class), 
	LONG(Long.class, Integer.class, Short.class), 
	DOUBLE(Double.class, Float.class), 
	STRING(String.class), 
	JSON_ARRAY(JSONArray.class), 
	JSON_OBJECT(JSONObject.class),	
	TIMESTAMP(LogicalTimestamp.class),
	UNDEFINED;
	
	private Class<?>[] supportedClasses;
	
	private ConType(Class<?> ...classes) {
		this.supportedClasses = classes;
	}
	
	public Class<?>[] getSupportedClasses() {
		return supportedClasses;
	}
	
	private static final Map<Class<?>, ConType> CLASS_MAP = new HashMap<>();
	
	static {
		for (ConType type : ConType.values()) {
			
			if (type.supportedClasses == null || type.supportedClasses.length == 0) {
				continue;
			}
			
			for (Class<?> clazz : type.supportedClasses) {
				CLASS_MAP.put(clazz, type);
			}
		}
	}
	
	public static ConType fromValue(Object value) {
		if (value == null) {
			return NULL;
		}
		return CLASS_MAP.getOrDefault(value.getClass(), UNDEFINED);
	}
	
}
