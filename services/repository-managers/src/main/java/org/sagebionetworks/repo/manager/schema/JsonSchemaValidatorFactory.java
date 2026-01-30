package org.sagebionetworks.repo.manager.schema;

import java.io.UncheckedIOException;

import org.apache.commons.lang.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.SchemaException;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.loader.internal.DefaultSchemaClient;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Factory service responsible for creating validator Schema instances from JsonSchema objects.
 */
@Service
public class JsonSchemaValidatorFactory {

	public static final String DRAFT_07 = "http://json-schema.org/draft-07/schema";

	/**
	 * Build a validator {@link Schema} from the provided {@link JsonSchema}.
	 * 
	 * @param jsonSchema
	 * @param useDefaults When set to true, default values will be added to the
	 *                    provided subject and default values are made available to
	 *                    visitors.
	 * @return The validator Schema object
	 * @throws IllegalArgumentException If the provided jsonSchema is not valid or supported.
	 */
	public Schema buildValidator(JsonSchema jsonSchema, boolean useDefaults) {
		ValidateArgument.required(jsonSchema, "jsonSchema");
		
		if (StringUtils.isBlank(jsonSchema.get$schema())) {
			/**
			 * The validation library silently ignores all JSON schema features added after
			 * draft-04, when a $schema is not provided. This causes unexpected behavior for
			 * users that depend on newer features but forget to include a $schema.
			 * Therefore, we default to draft-07 for this case.
			 */
			jsonSchema.set$schema(DRAFT_07);
		}
		
		String validationSchemaJson;
		
		try {
			validationSchemaJson = EntityFactory.createJSONStringForEntity(jsonSchema);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException("Failed to load schema: " + e.getMessage(), e);
		}
		
		try {
			SchemaLoader loader = SchemaLoader.builder()
				.schemaJson(new JSONObject(validationSchemaJson))
				.schemaClient(new DefaultSchemaClient())
				.useDefaults(useDefaults)
				.build();
			
			return loader.load().build();
		} catch (SchemaException | UncheckedIOException e) {
			// The error message when the version is not supported is not very user friendly
			if ("#: could not determine version".equals(e.getMessage())) {
				throw new IllegalArgumentException("Unsupported JSON schema version: " + jsonSchema.get$schema(), e);
			}			
			throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage(), e);
		}
	}

}
