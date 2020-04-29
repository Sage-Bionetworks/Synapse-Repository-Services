package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;

public interface JsonSchemaDao {

	/**
	 * Get the version information for the given version ID
	 * 
	 * @param versionId
	 * @return
	 */
	JsonSchemaVersionInfo getVersionInfo(String versionId);

	/**
	 * Get the versionId for a specific schema version.
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 * @return
	 */
	String getVersionId(String organizationName, String schemaName, String semanticVersion);

	/**
	 * Get the JsonSchemaVersionInfo for a specific version.
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 * @return
	 */
	JsonSchemaVersionInfo getVersionInfo(String organizationName, String schemaName, String semanticVersion);

	/**
	 * Get the versionId of the latest version for a schema.
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	String getLatestVersionId(String organizationName, String schemaName);

	/**
	 * Get the latest JsonSchemaVersionInfo for a schema.
	 * 
	 * @param organizationName
	 * @param schemaNames
	 * @return
	 */
	JsonSchemaVersionInfo getVersionLatestInfo(String organizationName, String schemaName);

	/**
	 * Truncate all data.
	 */
	void trunacteAll();

	/**
	 * Get the schema for the given version ID.
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	JsonSchema getSchema(String versionId);

	/**
	 * Attempt to delete the given schema.
	 * 
	 * @param schemaId
	 */
	int deleteSchema(String schemaId);

	/**
	 * Delete a specific version of a schema.
	 * 
	 */
	void deleteSchemaVersion(String versionId);

	/**
	 * Create a new schema version for the given request.
	 * 
	 * @param request
	 * @return
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(NewSchemaVersionRequest request);


}
