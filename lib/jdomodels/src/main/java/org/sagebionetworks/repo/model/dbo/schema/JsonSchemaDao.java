package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;

public interface JsonSchemaDao {

	/**
	 * Lookup the schemaId given an organizationId and schema name, and lock on the
	 * row using FOR UPDATE.
	 * 
	 * @param organizationId
	 * @param schemaName
	 * @return schemaId
	 */
	String getSchemaInfoForUpdate(String organizationId, String schemaName);

	/**
	 * Get the JSON data ID for the provide sha256hex
	 * 
	 * @param sha256hex
	 * @return
	 */
	String getJsonBlobId(String sha256hex);

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
	 * Use: {@link #createNewSchemaVersion(NewSchemaVersionRequest)}
	 * 
	 * @param organizationId
	 * @param schemaName
	 * @param createdBy
	 * @return
	 */
	String createSchemaIfDoesNotExist(String organizationId, String schemaName, Long createdBy);

	/**
	 * Use: {@link #createNewSchemaVersion(NewSchemaVersionRequest)}
	 * 
	 * @param schema
	 * @return
	 */
	String createJsonBlobIfDoesNotExist(JsonSchema schema);

	/**
	 * Use: {@link #createNewSchemaVersion(NewSchemaVersionRequest)}
	 * 
	 * @param schemaId
	 * @param semanticVersion
	 * @param createdBy
	 * @param blobId
	 * @return
	 */
	JsonSchemaVersionInfo createNewVersion(String schemaId, String semanticVersion, Long createdBy, String blobId);

	/**
	 * Create a new schema version for the given request.
	 * 
	 * @param request
	 * @return
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(NewSchemaVersionRequest request);

	/**
	 * Find the latest version ID for the given schema ID without using the cache.
	 * 
	 * @param schemaId
	 * @return
	 */
	Long findLatestVersionId(String schemaId);

	/**
	 * Get the schemaId for the given versionId and lock schema row using FOR
	 * UPDATE.
	 * 
	 * @param versionId
	 * @return
	 */
	String getSchemaIdForUpdate(String versionId);

	/**
	 * Get the etag of the latest version for a given schema ID.
	 * @param schemaId
	 * @return
	 */
	String getLatestVersionEtag(String schemaId);

}
