package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.OrganizationRequest;

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
	Organization createOrganziation(UserInfo user, OrganizationRequest request);
	
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
}
