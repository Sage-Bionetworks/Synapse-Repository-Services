package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.apache.commons.lang3.NotImplementedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Services for search queries and autocomplete.
 */
@ControllerInfo(displayName = "Search Query Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SearchQueryController {


	/**
	 * Start an asynchronous search query job.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_QUERY_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startQuery(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchIndexQuery request) {
		throw new NotImplementedException("Stub method not yet implemented");
	}

	/**
	 * Get the results of a previously started asynchronous search query.
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}">AsynchronousJobStatus</a> object.
	 * </p>
	 *
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_QUERY_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody SearchQueryResults getQueryResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		throw new NotImplementedException("Stub method not yet implemented");
	}

	/**
	 * Perform a synchronous autocomplete search query.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_AUTOCOMPLETE, method = RequestMethod.POST)
	public @ResponseBody SearchQueryResults autocomplete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchIndexQuery request) {
		throw new NotImplementedException("Stub method not yet implemented");
	}
}
