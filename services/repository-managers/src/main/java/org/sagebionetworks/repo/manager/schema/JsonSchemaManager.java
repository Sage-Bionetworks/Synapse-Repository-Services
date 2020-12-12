package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * 
 * Business logic for the user defined schemas.
 *
 */
public interface JsonSchemaManager {
	
	public static final String ABSOLUTE_$ID_TEMPALTE = "https://repo-prod.prod.sagebase.org/repo/v1/schema/type/registered/%s";
	
	/**
	 * Create the full absolute $id from the relative $id
	 * @param $id
	 * @return
	 */
	public static String createAbsolute$id(String relative$id) {
		return String.format(ABSOLUTE_$ID_TEMPALTE, relative$id);
	}
	
	/**
	 * Create a new Organization from the given request
	 * @param user
	 * @param request
	 * @return
	 */
	Organization createOrganziation(UserInfo user, CreateOrganizationRequest request);
	
	/**
	 * Get the current ACL for the identified organization.
	 * 
	 * @param user
	 * @param organziationId
	 * @return
	 */
	public AccessControlList getOrganizationAcl(UserInfo user, String organziationId);
	
	/**
	 * Update the ACL for the identified organization.
	 * 
	 * @param user
	 * @param organziationId
	 * @return
	 */
	public AccessControlList updateOrganizationAcl(UserInfo user, String organziationId, AccessControlList acl);

	/**
	 * Delete the identified Organization.
	 * @param user
	 * @param id
	 */
	void deleteOrganization(UserInfo user, String id);

	/**
	 * Lookup an Organization by name.
	 * @param user
	 * @param name
	 * @return
	 */
	Organization getOrganizationByName(UserInfo user, String name);
	
	/**
	 * Create a new JsonSchema.
	 * @param user
	 * @param request
	 * @return
	 */
	CreateSchemaResponse createJsonSchema(UserInfo user, CreateSchemaRequest request) throws RecoverableMessageException;

	/**
	 * Get the JSON schema for a given $id
	 * @param $id
	 * @param isTopLevel
	 * @return
	 */
	JsonSchema getSchema(String $id, boolean isTopLevel);

	void truncateAll();

	/**
	 * Get the latest version of the given schema.
	 * @param user
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	JsonSchemaVersionInfo getLatestVersion(String organizationName, String schemaName);
	
	/**
	 * List a single page of organizations.
	 * @param request
	 * @return
	 */
	ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request);
	
	/**
	 * List a single page of schemas for an organization.
	 * @param request
	 * @return
	 */
	ListJsonSchemaInfoResponse listSchemas(ListJsonSchemaInfoRequest request);
	
	/**
	 * List a single page of schema versions for an organization and schema.
	 * @param request
	 * @return
	 */
	ListJsonSchemaVersionInfoResponse listSchemaVersions(ListJsonSchemaVersionInfoRequest request);

	/**
	 * A validation schema is a self-contained representation of a schema.
	 * Specifically, each external '$ref' in the schema is loaded into the local
	 * '$defs' map. Each '$ref' is then changed to reference the local '$defs' map.
	 * @param id
	 * @return
	 */
	JsonSchema getValidationSchema(String id);

	/**
	 * Bind a JSON schema to an object.
	 * @param createdBy
	 * @param $id
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	JsonSchemaObjectBinding bindSchemaToObject(Long createdBy, String $id, Long objectId, BoundObjectType objectType);

	/**
	 * Get the JsonSchemaObjectBinding for the given objectId and objectType 
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	JsonSchemaObjectBinding getJsonSchemaObjectBinding(Long objectId, BoundObjectType objectType);

	/**
	 * Clear the bound schema from an object.
	 * @param objectId
	 * @param objectType
	 */
	void clearBoundSchema(Long objectId, BoundObjectType objectType);

	/**
	 * Delete a schema for the given $id.
	 * @param user
	 * @param $id
	 */
	void deleteSchemaById(UserInfo user, String $id);


}
