package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * A simple cache for schema objects.
 * @author John
 *
 */
public class SchemaCache {
	
	private static Map<Class<? extends JSONEntity>, ObjectSchema> cache = Collections.synchronizedMap(new HashMap<Class<? extends JSONEntity>, ObjectSchema>());
	
	/**
	 * Get the ObjectSchema for a JSONEntity.
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static ObjectSchema getSchema(JSONEntity entity) {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		Class<? extends JSONEntity> clazz = entity.getClass();
		ObjectSchema schema = cache.get(clazz);
		if(schema == null){
			String jsonString = entity.getJSONSchema();
			if(jsonString == null) throw new IllegalArgumentException("The JSON Schema cannot be null for entity.getJSONSchema()");
			try {
				schema = new ObjectSchema(JSONObjectAdapterImpl.createAdapterFromJSONString(jsonString));
			} catch (JSONObjectAdapterException e) {
				// convert this to a runtime.
				throw new RuntimeException(e);
			}
			cache.put(clazz, schema);
		}
		return schema;
	}

}
