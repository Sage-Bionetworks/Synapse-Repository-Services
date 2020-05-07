package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
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
	 * 
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 * @return
	 */
	JsonSchema getSchema(String organizationName, String schemaName, String semanticVersion);

	/**
	 * Attempt to delete all version of a schema.
	 * @param user
	 * @param organizationName
	 * @param schemaName
	 */
	public void deleteSchemaAllVersion(UserInfo user, String organizationName, String schemaName);
	
	/**
	 * Delete a specific version of a schema.
	 * @param user
	 * @param organizationName
	 * @param schemaName
	 * @param semanticVersion
	 */
	void deleteSchemaVersion(UserInfo user, String organizationName, String schemaName, String semanticVersion);

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


}
