package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * CloudSearch search controller. It currently offers two methods:
 * <ol>
 * <li>'/searchRaw' proxies raw CloudSearch requests though as-is except for
 * adding an authorization filter
 * <li>'/search' operates like /searchRaw but in addition reformats the result
 * into a Synapse model object
 * </ol>
 * 
 * @author deflaux
 * 
 */
@ControllerInfo(displayName="Search Services", path="repo/v1")
@Controller
public class SearchController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * @param userId
	 * @param searchQuery
	 * @param request
	 * @return search results from CloudSearch
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { "/search" }, method = RequestMethod.POST)
	public @ResponseBody
	SearchResults proxySearch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestBody SearchQuery searchQuery,
			HttpServletRequest request) throws ClientProtocolException,
			IOException, HttpClientHelperException,
			DatastoreException, NotFoundException {
		return serviceProvider.getSearchService().proxySearch(userId, searchQuery);
	}

	/**
	 * @param userId
	 * @param searchQuery
	 * @param request
	 * @return search results from CloudSearch
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws JSONException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { "/searchRaw" }, method = RequestMethod.GET)
	public ModelAndView proxyRawSearch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = "q", required = false) String searchQuery,
			HttpServletRequest request) throws ClientProtocolException,
			IOException, HttpClientHelperException, JSONException,
			DatastoreException, NotFoundException {
		return serviceProvider.getSearchService().proxyRawSearch(userId, searchQuery, request);
	}
}
