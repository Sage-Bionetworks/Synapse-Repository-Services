package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;

import org.sagebionetworks.repo.model.schema.Organization;

public interface OrganizationDao {

	/**
	 * Create a new Organization.
	 * 
	 * @param name
	 * @param createdBy
	 * @return
	 */
	Organization createOrganization(String name, Long createdBy);

	/**
	 * Get an Organization by name.
	 * 
	 * @param name
	 * @return
	 */
	Organization getOrganizationByName(String name);

	/**
	 * Delete an organization by id.
	 * 
	 * @param id
	 */
	void deleteOrganization(String id);

	/**
	 * Truncate all organization data.
	 */
	void truncateAll();

	/**
	 * List a single page of Organizations.
	 * @param limitForQuery
	 * @param offset
	 * @return
	 */
	List<Organization> listOrganizations(long limit, long offset);

}
