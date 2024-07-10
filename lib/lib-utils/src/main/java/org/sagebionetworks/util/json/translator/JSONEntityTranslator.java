package org.sagebionetworks.util.json.translator;

import org.json.JSONObject;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class JSONEntityTranslator implements Translator<JSONEntity, JSONObject> {

	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return JSONEntity.class.isAssignableFrom(fieldType);
	}

	@Override
	public JSONEntity translateFromJSONToJava(Class<? extends JSONEntity> type, JSONObject jsonValue) {
		try {
			return EntityFactory.createEntityFromJSONObject(jsonValue, type);
		} catch (IllegalArgumentException | JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject translateFromJavaToJSON(JSONEntity fieldValue) {
		try {
			return EntityFactory.createJSONObjectForEntity(fieldValue);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<? extends JSONObject> getJSONClass() {
		return JSONObject.class;
	}

}
