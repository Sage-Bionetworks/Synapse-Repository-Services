package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesResponse;

public interface ColumnAnalyzerOverrideManager {

	/**
	 * Create a new column analyzer override.
	 *
	 * @param user The user performing the operation
	 * @param request The column analyzer override to create
	 * @return The created column analyzer override
	 */
	ColumnAnalyzerOverride create(UserInfo user, ColumnAnalyzerOverride request);

	/**
	 * Get a column analyzer override by its ID.
	 *
	 * @param user The user performing the operation
	 * @param id The ID of the column analyzer override
	 * @return The column analyzer override
	 */
	ColumnAnalyzerOverride get(UserInfo user, String id);

	/**
	 * Update an existing column analyzer override.
	 *
	 * @param user The user performing the operation
	 * @param request The column analyzer override with updated fields
	 * @return The updated column analyzer override
	 */
	ColumnAnalyzerOverride update(UserInfo user, ColumnAnalyzerOverride request);

	/**
	 * Delete a column analyzer override by its ID.
	 *
	 * @param user The user performing the operation
	 * @param id The ID of the column analyzer override to delete
	 */
	void delete(UserInfo user, String id);

	/**
	 * List column analyzer overrides, optionally filtered by organization.
	 *
	 * @param user The user performing the operation
	 * @param request The list request with optional filters and pagination
	 * @return The page of column analyzer overrides
	 */
	ListColumnAnalyzerOverridesResponse list(UserInfo user, ListColumnAnalyzerOverridesRequest request);
}
