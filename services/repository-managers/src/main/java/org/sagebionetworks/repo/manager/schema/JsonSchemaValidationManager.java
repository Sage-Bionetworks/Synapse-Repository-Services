package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public interface JsonSchemaValidationManager {

	/**
	 * Validate the given subject against the given schema.
	 * 
	 * @param schema
	 * @param subject
	 * @return
	 */
	public ValidationResults validate(JsonSchema schema, JsonSubject subject);

}
