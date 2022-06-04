package org.sagebionetworks.repo.manager.schema;

import java.util.Optional;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
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
	ValidationResults validate(JsonSchema schema, JsonSubject subject);

	/**
	 * Derived annotation are value-key-pairs that are derived from a combination of
	 * existing annotations combined with a {@link JsonSchema} that defines one or
	 * more 'default' or 'const' properties. 
	 * 
	 * @param schema
	 * @param subject
	 * @return
	 */
	Optional<Annotations> calculateDerivedAnnotations(JsonSchema schema, JSONObject subject);

}
