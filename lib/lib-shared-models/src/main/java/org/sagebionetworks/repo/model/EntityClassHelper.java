package org.sagebionetworks.repo.model;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class EntityClassHelper {
	private static AutoGenFactory autoGenFactory = new AutoGenFactory();
	
	private static final String CONCRETE_TYPE_FIELD_NAME = "concreteType";
	
	public static String entityType(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
		return jsonObjectAdapter.getString(CONCRETE_TYPE_FIELD_NAME);
	}
	
	public static JSONEntity deserialize(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
		String entityType = entityType(jsonObjectAdapter);
		JSONEntity newInstance = (JSONEntity)autoGenFactory.newInstance(entityType);
		newInstance.initializeFromJSONObject(jsonObjectAdapter);
		return newInstance;
	}
	
}
