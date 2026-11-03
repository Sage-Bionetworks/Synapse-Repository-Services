package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesRequest;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesResponse;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;

public interface SearchPipelineManager {

	/**
	 * Create a new search pipeline.
	 *
	 * @param user The user performing the operation
	 * @param request The search pipeline to create
	 * @return The created search pipeline
	 */
	NamedSearchPipeline create(UserInfo user, NamedSearchPipeline request);

	/**
	 * Get a search pipeline by its ID.
	 *
	 * @param user The user performing the operation
	 * @param id The ID of the search pipeline
	 * @return The search pipeline
	 */
	NamedSearchPipeline get(UserInfo user, String id);

	/**
	 * Update the description and settings of an existing search pipeline.
	 *
	 * @param user The user performing the operation
	 * @param request The search pipeline with updated fields
	 * @return The updated search pipeline
	 */
	NamedSearchPipeline update(UserInfo user, NamedSearchPipeline request);

	/**
	 * List search pipelines, optionally filtered by organization.
	 *
	 * @param user The user performing the operation
	 * @param request The list request with optional filters and pagination
	 * @return The page of search pipelines
	 */
	ListNamedSearchPipelinesResponse list(UserInfo user, ListNamedSearchPipelinesRequest request);
}
