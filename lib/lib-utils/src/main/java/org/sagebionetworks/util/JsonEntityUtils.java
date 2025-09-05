package org.sagebionetworks.util;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class JsonEntityUtils {

	/**
	 * Serialize the to a JSON string.
	 * @param entity
	 * @return
	 */
	public static String toJsonString(JSONEntity entity) {
		if (entity == null) {
			return null;
		}
		try {
			return EntityFactory.createJSONStringForEntity(entity);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deserialize from a JSON string.
	 * @param <T>
	 * @param jsonString
	 * @param clazz
	 * @return
	 */
	public static <T extends JSONEntity> T fromJsonString(String jsonString, Class<? extends T> clazz) {
		if (jsonString == null) {
			return null;
		}
		try {
			return EntityFactory.createEntityFromJSONString(jsonString, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

}
