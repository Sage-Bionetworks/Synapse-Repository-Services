package org.sagebionetworks.repo.util;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.DatastoreException;

/**
 * This utility class facilitates the conversion from model objects to Json Schema
 * 
 * @author deflaux
 *
 */
public class SchemaHelper {

	// Use a static instance of this per
	// http://wiki.fasterxml.com/JacksonBestPracticesPerformance
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Get the schema for a class<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @param theModelClass
	 * @return the Schema
	 * @throws DatastoreException
	 */
	@SuppressWarnings("unchecked")
	public static JsonSchema getSchema(Class theModelClass) throws DatastoreException {
		try {
			return OBJECT_MAPPER.generateJsonSchema(theModelClass);
		} catch (JsonMappingException e) {
			throw new DatastoreException(
					"Unable to create a schema for " + theModelClass.getName(), e);
		}
	}

}
