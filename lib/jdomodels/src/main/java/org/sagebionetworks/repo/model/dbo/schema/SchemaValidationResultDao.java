package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;

public interface SchemaValidationResultDao {

	/**
	 * Clear the validation results for the given object.
	 * 
	 * @param objectId
	 * @param objectType
	 */
	void clearResults(String objectid, ObjectType objectType);

	/**
	 * Create or update the given validation results in the database.
	 * 
	 * @param results
	 */
	void createOrUpdateResults(ValidationResults results);

	ValidationResults getValidationResults(String objectid, ObjectType objectType);

	/**
	 * Clear all validation results.
	 */
	void clearAll();

}
