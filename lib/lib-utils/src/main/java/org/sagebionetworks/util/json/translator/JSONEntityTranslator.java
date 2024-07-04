package org.sagebionetworks.util.json.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

import org.json.JSONObject;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class JSONEntityTranslator implements Translator<JSONEntity, JSONObject> {

	@Override
	public boolean canTranslate(Class fieldType) {
		return JSONEntity.class.isAssignableFrom(fieldType);
	}

	@Override
	public JSONEntity translateFromJSONToFieldValue(Class type, JSONObject jsonValue) {
		try {
			Optional<Constructor> constructor = Arrays.stream(type.getDeclaredConstructors())
					.filter(c -> c.getParameterCount() == 0).findFirst();
			if (!constructor.isPresent()) {
				throw new IllegalArgumentException("No zero argument constructor found for:" + type.getName());
			}
			JSONEntity newJSONEntity = (JSONEntity) constructor.get().newInstance(null);
			newJSONEntity.initializeFromJSONObject(new JSONObjectAdapterImpl(jsonValue));
			return newJSONEntity;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JSONObject translateFieldValueToJSON(Class type, JSONEntity fieldValue) {
		try {
			JSONObject jsonObject = new JSONObject();
			fieldValue.writeToJSONObject(new JSONObjectAdapterImpl(jsonObject));
			return jsonObject;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<? extends JSONEntity> getFieldClass() {
		return JSONEntity.class;
	}

	@Override
	public Class<? extends JSONObject> getJSONClass() {
		return JSONObject.class;
	}

}
