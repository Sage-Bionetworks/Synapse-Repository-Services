package org.sagebionetworks.repo.manager.schema;

import java.io.IOException;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface SchemaTranslator {
	
	/**
	 * Load an ObjectSchemaImpl from the auto-generated project via the classpath.
	 * 
	 * @param id The ID of the schema to load. For example to load the schema for a
	 *           FileEntity use: "org.sagebionetworks.repo.model.FileEntity"
	 * @return
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	ObjectSchemaImpl loadSchemaFromClasspath(String id);

	/**
	 * Translate from an ObjectSchema to a JsonSchema;
	 * @param objectSchema
	 * @return
	 */
	JsonSchema translate(ObjectSchema objectSchema);
}
