package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.JsonSchema;

public interface ValidationJsonSchemaIndexDao {
	
	/**
	 * Creates or updates the versionId to the given schema
	 * @param versionId
	 * @param schema
	 */
	void createOrUpdate(String versionId, JsonSchema schema);

	/**
	 * Deletes the validation schema associated 
	 * with the versionId from the index
	 * @param versionId
	 */
	void delete(String versionId);

	/**
	 * Gets the validation schema for the given versionId,
	 * throws NotFoundException if does not exist
	 * @param versionId
	 * @return
	 */
	JsonSchema getValidationSchema(String versionId);
	
	/**
	 * Remove all validation schemas
	 */
	void truncateAll();
}
