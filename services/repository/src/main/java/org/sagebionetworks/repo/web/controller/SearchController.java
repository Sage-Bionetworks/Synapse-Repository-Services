package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.util.SearchHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * CloudSearch search controller. It currently offers two methods:
 * <ol>
 * <li>/searchRaw proxies raw CloudSearch requests though as-is except for
 * adding an authorization filter
 * <li>/search operates like /searchRaw but in addition reformats the result
 * into a Synapse model object
 * </ol>
 * 
 * @author deflaux
 * 
 */
@Controller
public class SearchController extends BaseController {
	private static final Logger log = Logger.getLogger(SearchController.class
			.getName());
	private static final String CLOUD_SEARCH_ENDPOINT = StackConfiguration
			.getSearchServiceEndpoint();

	private static final HttpClient httpClient;

	static {
		httpClient = HttpClientHelper.createNewClient(true);
		ThreadSafeClientConnManager manager = (ThreadSafeClientConnManager) httpClient
				.getConnectionManager();
		// ensure that we can have *many* simultaneous connections to
		// CloudSearch
		manager.setDefaultMaxPerRoute(StackConfiguration
				.getHttpClientMaxConnsPerRoute());
	}

	@Autowired
	UserManager userManager;

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
	@RequestMapping(value = { "/search" }, method = RequestMethod.GET)
	public @ResponseBody
	SearchResults proxySearch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = "q", required = false) String searchQuery,
			HttpServletRequest request) throws ClientProtocolException,
			IOException, HttpClientHelperException,
			DatastoreException, NotFoundException {

		log.debug("Got raw query " + searchQuery);

		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			searchQuery += "&" + SearchHelper.formulateAuthorizationFilter(userInfo);
		}

		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(searchQuery);

		String url = CLOUD_SEARCH_ENDPOINT + "?" + cleanedSearchQuery;

		log.debug("About to request from CloudSearch: " + url);
		String response = HttpClientHelper.getContent(httpClient, url);
		log.debug("Response from CloudSearch: " + response);

		try {
			return SearchHelper.csSearchResultsToSynapseSearchResults(response);
		} catch (JSONException e) {
			throw new DatastoreException("Results conversion failed for request " + url + " with response " + response, e);
		}
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

		log.debug("Got raw query " + searchQuery);

		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			searchQuery += "&" + SearchHelper.formulateAuthorizationFilter(userInfo);
		}

		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(searchQuery);

		String url = CLOUD_SEARCH_ENDPOINT + "?" + cleanedSearchQuery;
		log.debug("About to request " + url);

		String response = HttpClientHelper.getContent(httpClient, url);

		ModelAndView mav = new ModelAndView();
		mav.addObject("result", response);
		mav.addObject("url", url);
		return mav;
	}
}
