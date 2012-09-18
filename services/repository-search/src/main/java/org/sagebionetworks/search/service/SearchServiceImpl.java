package org.sagebionetworks.search.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.controller.SearchUtil;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SearchServiceImpl implements SearchService {
	private static final Logger log = Logger.getLogger(SearchServiceImpl.class
			.getName());

	
	@Autowired
	SearchDao searchDao;

	@Autowired
	UserManager userManager;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.SearchService#proxySearch(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public @ResponseBody
	SearchResults proxySearch(String userId, SearchQuery searchQuery) 
			throws ClientProtocolException,	IOException, HttpClientHelperException,
			DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return proxySearch(userInfo, searchQuery);
	}

	/**
	 * @param userInfo
	 * @param searchQuery
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery)	throws UnsupportedEncodingException, ClientProtocolException,
			IOException, HttpClientHelperException {
		String serchQueryString = SearchUtil.generateQueryString(searchQuery);
		serchQueryString = filterSeachForAuthorization(userInfo, serchQueryString);
		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(serchQueryString);
		return searchDao.executeSearch(cleanedSearchQuery);
	}

	/**
	 * @param userInfo
	 * @param searchQuery
	 * @return
	 */
	public String filterSeachForAuthorization(UserInfo userInfo,String searchQuery) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if (!userInfo.isAdmin()) {
			StringBuilder builder = new StringBuilder(searchQuery);
			builder.append("&");
			builder.append(SearchHelper.formulateAuthorizationFilter(userInfo));
			searchQuery = builder.toString();
		}
		return searchQuery;
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
		return proxyRawSearch(searchQuery, userInfo);
	}

	/**
	 * @param searchQuery
	 * @param userInfo
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public ModelAndView proxyRawSearch(String searchQuery, UserInfo userInfo)
			throws UnsupportedEncodingException, ClientProtocolException,
			IOException, HttpClientHelperException {
		searchQuery = filterSeachForAuthorization(userInfo, searchQuery);

		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(searchQuery);
		String response = searchDao.executeRawSearch(cleanedSearchQuery);

		ModelAndView mav = new ModelAndView();
		mav.addObject("result", response);
		mav.addObject("url", "private");
		return mav;
	}
}
