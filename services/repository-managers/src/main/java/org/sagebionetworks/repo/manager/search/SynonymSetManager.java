package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;

public interface SynonymSetManager {

	/**
	 * Create a new synonym set.
	 *
	 * @param user The user performing the operation
	 * @param request The synonym set to create
	 * @return The created synonym set
	 */
	SynonymSet create(UserInfo user, SynonymSet request);

	/**
	 * Get a synonym set by its ID.
	 *
	 * @param user The user performing the operation
	 * @param id The ID of the synonym set
	 * @return The synonym set
	 */
	SynonymSet get(UserInfo user, String id);

	/**
	 * Update an existing synonym set.
	 *
	 * @param user The user performing the operation
	 * @param request The synonym set with updated fields
	 * @return The updated synonym set
	 */
	SynonymSet update(UserInfo user, SynonymSet request);

	/**
	 * Delete a synonym set by its ID.
	 *
	 * @param user The user performing the operation
	 * @param id The ID of the synonym set to delete
	 */
	void delete(UserInfo user, String id);

	/**
	 * List synonym sets, optionally filtered by organization.
	 *
	 * @param user The user performing the operation
	 * @param request The list request with optional filters and pagination
	 * @return The page of synonym sets
	 */
	ListSynonymSetsResponse list(UserInfo user, ListSynonymSetsRequest request);
}
