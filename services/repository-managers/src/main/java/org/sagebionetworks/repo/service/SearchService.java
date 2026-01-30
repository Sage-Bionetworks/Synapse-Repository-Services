package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionResults;
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

	/**
	 * Get suggestions for search terms on behalf of the user
	 *
	 * @param userId
	 * @param suggestionQuery
	 * @return the results of the suggestion
	 */
	@ResponseBody
	SuggestionResults getSuggestions(Long userId, SuggestionQuery suggestionQuery);

}