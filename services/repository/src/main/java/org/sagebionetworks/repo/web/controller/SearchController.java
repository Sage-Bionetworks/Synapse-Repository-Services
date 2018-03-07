package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

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
 * CloudSearch search controller. It currently offers two methods:
 * <ol>
 * <li>'/search' appends a authorization filter to the user's search and reformats the result
 * into a Synapse model object
 * </ol>
 * 
 */
@ControllerInfo(displayName="Search Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SearchController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;

	/**
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
