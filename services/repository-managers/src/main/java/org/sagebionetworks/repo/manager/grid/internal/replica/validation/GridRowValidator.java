package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.List;

import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.springframework.stereotype.Service;

/**
 * Shared, schema-driven row validator used in all places a JSON Schema is used to validate
 * one or more grid rows.
 */
@Service
public class GridRowValidator {

	private final JsonSchemaManager jsonSchemaManager;
	private final JsonSchemaValidationManager jsonSchemaValidationManager;

	public GridRowValidator(JsonSchemaManager jsonSchemaManager,
			JsonSchemaValidationManager jsonSchemaValidationManager) {
		this.jsonSchemaManager = jsonSchemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
	}

	/**
	 * Resolve the de-referenced validation schema for the given schema $id.
	 *
	 * @param schemaId the bound JSON Schema $id
	 * @return the validation schema
	 */
	public JsonSchema getValidationSchema(String schemaId) {
		return jsonSchemaManager.getValidationSchema(schemaId);
	}

	/**
	 * Validate a batch of subjects against the provided schema. Each returned
	 * {@link ValidationResults} has its transient fields cleared via
	 * {@link #cleanupValidationResults(ValidationResults)} so the payload is
	 * minimal and identical whether produced by the worker or the push build.
	 *
	 * @param schema   the validation schema
	 * @param subjects the subjects to validate, in order
	 * @return one {@link ValidationResults} per subject, in the same order
	 */
	public List<ValidationResults> validateBatch(JsonSchema schema, List<JsonSubject> subjects) {
		List<ValidationResults> results = jsonSchemaValidationManager.validateBatch(schema, subjects);
		results.forEach(GridRowValidator::cleanupValidationResults);
		return results;
	}

	/**
	 * Remove 'extra' data from a row's validation results to reduce its size.
	 *
	 * @param validationResults the results to clean up (mutated in place)
	 */
	public static void cleanupValidationResults(ValidationResults validationResults) {
		validationResults.setValidatedOn(null);
		validationResults.setSchema$id(null);
		validationResults.setValidationException(null);
	}
}
