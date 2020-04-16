package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.SchemaInfo;

public interface JsonSchemaDao {

	/**
	 * Create a JSON schema if it does not already exist.
	 * 
	 * @param schemaRoot
	 * @return The returned SchemaInfo.id will either be the ID of the newly created
	 *         schema or the ID of the existing schema.
	 */
	SchemaInfo createSchemaIfDoesNotExist(SchemaInfo schemaRoot);

	/**
	 * Lookup a SchemaInfo from an organization name and schema name.
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	SchemaInfo getSchemaInfoForUpdate(String organizationName, String schemaName);

	/**
	 * Create a new JSON blob if the one does not already exist for the given
	 * sha256hex.
	 * 
	 * @param json
	 * @param sha256hex
	 * @return If a new JSON blob is created then the new ID will be returned, else
	 *         the existing ID will be returned.
	 */
	String createJsonBlobIfDoesNotExist(String json, String sha256hex);

	/**
	 * Get the JSON data ID for the provide sha256hex
	 * 
	 * @param sha256hex
	 * @return
	 */
	String getJsonBlobId(String sha256hex);
	
	/**
	 * Create a new version for a JSON schema.
	 * @param request
	 * @return
	 */
	String createNewVersion(NewVersionRequest request);
	
	/**
	 * Get the version information for the given version ID
	 * @param versionId
	 * @return
	 */
	JsonSchemaVersionInfo getVersionInfo(String versionId);

	/**
	 * Truncate all data.
	 */
	void trunacteAll();

}
