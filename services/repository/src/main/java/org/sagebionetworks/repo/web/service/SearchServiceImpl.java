package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.util.SearchHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
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
public class SearchServiceImpl implements SearchService {
	private static final Logger log = Logger.getLogger(SearchServiceImpl.class
			.getName());
	private static final String CLOUD_SEARCH_ENDPOINT = StackConfiguration
			.getSearchServiceEndpoint();

	private static final AwesomeSearchFactory searchResultsFactory = new AwesomeSearchFactory(new AdapterFactoryImpl());
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

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.SearchService#proxySearch(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public @ResponseBody
	SearchResults proxySearch(String userId, String searchQuery, HttpServletRequest request) 
			throws ClientProtocolException,	IOException, HttpClientHelperException,
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
			return searchResultsFactory.fromAwesomeSearchResults(response);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException("Results conversion failed for request " + url + " with response " + response, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.SearchService#proxyRawSearch(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public ModelAndView proxyRawSearch(String userId, String searchQuery,
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
