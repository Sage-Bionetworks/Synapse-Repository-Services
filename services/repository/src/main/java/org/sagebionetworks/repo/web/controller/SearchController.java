package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Search for Entities on Synapse
 * 
 */
@ControllerInfo(displayName="Search Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SearchController {
	
	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Searches for <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a>s that are accessible by the current user.
	 * If not authenticated, only public result will be shown.
	 * See <a href="${org.sagebionetworks.repo.model.search.query.SearchFieldName}">SearchFieldName</a> for the list of searchable fields for use in booleanQuery, rangeQuery, and returnFields
	 * @param userId
	 * @param searchQuery
	 * @return search results from CloudSearch
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { "/search" }, method = RequestMethod.POST)
	public @ResponseBody
	SearchResults proxySearch(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody SearchQuery searchQuery) {
		return serviceProvider.getSearchService().proxySearch(userId, searchQuery);
	}
}
