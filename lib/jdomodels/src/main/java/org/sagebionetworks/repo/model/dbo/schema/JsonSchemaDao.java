package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;

import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
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
	 * Get the latest versionId for a schemaId.
	 * @param schemaId
	 * @return
	 */
	String getLatestVersionId(String schemaId);

	/**
	 * Get the latest JsonSchemaVersionInfo for a schema.
	 * 
	 * @param organizationName
	 * @param schemaNames
	 * @return
	 */
	JsonSchemaVersionInfo getVersionLatestInfo(String organizationName, String schemaName);
	
	/**
	 * Get the schema ID for the given organization and name.
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	String getSchemaId(String organizationName, String schemaName);

	/**
	 * Truncate all data.
	 */
	void truncateAll();

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
	void deleteSchema(String schemaId);

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

	/**
	 * List the schemas for the given organization.
	 * @param organizationName
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<JsonSchemaInfo> listSchemas(String organizationName, long limit, long offset);

	/**
	 * List the versions for the given organization and schema names.
	 * @param organizationName
	 * @param schemaName
	 * @param limitForQuery
	 * @param offset
	 * @return
	 */
	List<JsonSchemaVersionInfo> listSchemaVersions(String organizationName, String schemaName, long limit,
			long offset);

	/**
	 * Bind a JsonSchema to the provided objectId-objectType pair.
	 * @param request
	 */
	JsonSchemaObjectBinding bindSchemaToObject(BindSchemaRequest request);
	
	/**
	 * Get the JSON schema bound to the given objectId and type.
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	JsonSchemaObjectBinding getSchemaBindingForObject(Long objectId, BoundObjectType objectType);

	/**
	 * Clear the bound schema from an Object.
	 * @param objectId
	 * @param objectType
	 */
	void clearBoundSchema(Long objectId, BoundObjectType objectType);


}
