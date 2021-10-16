package org.sagebionetworks.repo.manager.schema;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class SchemaTestUtils {

	/**
	 * Load a schema from the classpath.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static JsonSchema loadSchemaFromClasspath(String name) throws Exception {
		String jsonString = loadStringFromClasspath(name);
		return new JsonSchema(new JSONObjectAdapterImpl(new JSONObject(jsonString)));
	}

	/**
	 * Load the file contents from the class path.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static String loadStringFromClasspath(String name) throws Exception {
		try (InputStream in = SchemaTestUtils.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			return IOUtils.toString(in, "UTF-8");
		}
	}
	
	public static JsonSchema cloneJsonSchema(JsonSchema schema) {
		try {
			String json = EntityFactory.createJSONStringForEntity(schema);
			return EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
}
