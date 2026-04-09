package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.schema.Organization;

public interface OrganizationDao {

	/**
	 * Create a new Organization with an auto-generated ID.
	 *
	 * @param name
	 * @param createdBy
	 * @return
	 */
	Organization createOrganization(String name, Long createdBy);

	/**
	 * Create a new Organization with a specific ID.
	 *
	 * @param name
	 * @param createdBy
	 * @param id The specific ID to assign to the organization.
	 * @return
	 */
	Organization createOrganization(String name, Long createdBy, Long id);

	/**
	 * Get an Organization by name.
	 * 
	 * @param name
	 * @return
	 */
	Organization getOrganizationByName(String name);


	/**
	 * Get an Organization by ID.
	 *
	 * @param id
	 * @return
	 */
	Optional<Organization> getOrganizationById(String id);

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
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Organization> listOrganizations(long limit, long offset);

}
