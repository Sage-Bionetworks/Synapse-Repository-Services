package org.sagebionetworks.repo.service.search;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;

public interface SearchIndexQueryService {

	/**
	 * Start an asynchronous search query job.
	 *
	 * @param userId The user performing the search
	 * @param query The search index query (contains searchIndexId, searchQuery, and an
	 *              optional responseParts list)
	 * @return The asynchronous job status containing the job ID
	 */
	AsynchronousJobStatus startSearchQuery(Long userId, SearchIndexQuery query);

	/**
	 * Get the results of a previously started asynchronous search query. The job's response
	 * body is a {@link SearchQueryResults} with opt-in parts selected by the request's
	 * {@code responseParts}.
	 *
	 * @param userId The user performing the search
	 * @param asyncToken The asynchronous job token
	 * @return The asynchronous job status
	 * @throws Throwable if the job failed or is not ready
	 */
	AsynchronousJobStatus getSearchQueryResults(Long userId, String asyncToken) throws Throwable;

	/**
	 * Perform a synchronous autocomplete search query. The response carries
	 * matching hits only and is capped at 8 entries.
	 *
	 * @param userId  The user performing the search
	 * @param request The slim autocomplete request (target index, prefix-flavored DSL clause,
	 *                optional returnFields)
	 * @return The autocomplete hits (up to 8)
	 */
	SearchQueryResults autocomplete(Long userId, SearchAutocompleteRequest request);
}
