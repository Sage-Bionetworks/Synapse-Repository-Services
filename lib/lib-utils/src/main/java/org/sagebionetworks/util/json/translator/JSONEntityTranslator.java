package org.sagebionetworks.util.json.translator;

import org.json.JSONObject;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.json.JavaJSONUtil;

public class JSONEntityTranslator implements Translator<JSONEntity, JSONObject> {

	@Override
	public boolean canTranslate(Class<?> fieldType) {
		return JSONEntity.class.isAssignableFrom(fieldType);
	}

	@Override
	public JSONEntity translateFromJSONToJava(Class<? extends JSONEntity> type, JSONObject jsonValue) {
		try {
			JSONEntity newJSONEntity = (JSONEntity) JavaJSONUtil.createNewInstance(type);
			newJSONEntity.initializeFromJSONObject(new JSONObjectAdapterImpl(jsonValue));
			return newJSONEntity;
		} catch (IllegalArgumentException | JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject translateFromJavaToJSON(JSONEntity fieldValue) {
		try {
			JSONObject jsonObject = new JSONObject();
			fieldValue.writeToJSONObject(new JSONObjectAdapterImpl(jsonObject));
			return jsonObject;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<? extends JSONObject> getJSONClass() {
		return JSONObject.class;
	}

}
