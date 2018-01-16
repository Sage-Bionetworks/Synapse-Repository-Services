package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.springframework.web.bind.annotation.ResponseBody;

public interface SearchService {

	/**
	 * Perform the search defined in the SearchQuery on behalf of the user
	 * @param userId
	 * @param searchQuery
	 * @return the results of the search
	 */
	public @ResponseBody
	SearchResults proxySearch(Long userId, SearchQuery searchQuery);

}