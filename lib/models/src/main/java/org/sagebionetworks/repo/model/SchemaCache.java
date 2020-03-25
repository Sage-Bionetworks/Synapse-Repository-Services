package org.sagebionetworks.repo.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;

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
		return getSchema(entity.getClass());
	}
	
	/**
	 * Get the schema for a JSONEntity class.
	 * @param clazz
	 * @return
	 */
	public static ObjectSchema getSchema(Class<? extends JSONEntity> clazz) {
		if(clazz == null) throw new IllegalArgumentException("Entity class cannot be null");
		ObjectSchema schema = cache.get(clazz);
		if(schema == null){
			try {
				String jsonString = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(clazz);
				if(jsonString == null) throw new IllegalArgumentException("The JSON Schema cannot be null for entity.getJSONSchema()");
				schema = new ObjectSchemaImpl(new JSONObjectAdapterImpl(jsonString));
			} catch (Exception e) {
				// convert this to a runtime.
				throw new RuntimeException(e);
			}
			cache.put(clazz, schema);
		}
		return schema;
	}

}
