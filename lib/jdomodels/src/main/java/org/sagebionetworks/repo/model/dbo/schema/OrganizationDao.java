package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.Organization;

public interface OrganizationDao {

	/**
	 * Create a new Organization.
	 * @param org
	 * @return
	 */
	Organization createOrganization(Organization org);
	
	/**
	 * Get an Organization by name.
	 * @param name
	 * @return
	 */
	Organization getOrganization(String name);
	
	/**
	 * Delete an organization by name.
	 * @param name
	 */
	void deleteOrganization(String name);
	
	/**
	 * Truncate all organization data.
	 */
	void truncateAll();
	
}
