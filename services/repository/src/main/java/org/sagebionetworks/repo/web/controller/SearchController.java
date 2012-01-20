package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

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
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * CloudSearch search controller. It basically just proxies raw CloudSearch
 * requests though as-is except for adding an authorization filter.
 * 
 * @author deflaux
 * 
 */
@Controller
public class SearchController extends BaseController {
	private static final String ACL_INDEX_FIELD = "acl";
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
	public ModelAndView proxySearch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = "q", required = false) String searchQuery,
			HttpServletRequest request) throws ClientProtocolException,
			IOException, HttpClientHelperException, JSONException,
			DatastoreException, NotFoundException {

		log.debug("Got raw query " + searchQuery);

		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) {
			searchQuery += "&" + formulateAuthorizationFilter(userInfo);
		}

		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = cleanUpBooleanSearchQueries(searchQuery);

		String url = CLOUD_SEARCH_ENDPOINT + "?" + cleanedSearchQuery;
		log.debug("About to request " + url);

		String response = HttpClientHelper.getFileContents(httpClient, url);

		// TODO make a response pojo in the pojo2schema project
		ModelAndView mav = new ModelAndView();
		mav.addObject("result", response);
		mav.addObject("url", url);
		return mav;
	}

	static String formulateAuthorizationFilter(UserInfo userInfo)
			throws DatastoreException {
		Collection<UserGroup> groups = userInfo.getGroups();
		if (0 == groups.size()) {
			// being extra paranoid here, this is unlikely
			throw new DatastoreException("no groups for user " + userInfo);
		}

		// Make our boolean query
		String authorizationFilter = "";
		for (UserGroup group : groups) {
			if (0 < authorizationFilter.length()) {
				authorizationFilter += " ";
			}
			authorizationFilter += ACL_INDEX_FIELD + ":'" + group.getName()
					+ "'";
		}
		if (1 == groups.size()) {
			authorizationFilter = "bq=" + authorizationFilter;
		} else {
			authorizationFilter = "bq=(or " + authorizationFilter + ")";
		}
		return authorizationFilter;
	}

	static String cleanUpBooleanSearchQueries(String query)
			throws UnsupportedEncodingException {
		// Make sure the url is well-formed so that we can correctly clean it up
		String decodedQuery = URLDecoder.decode(query, "UTF-8");
		if (decodedQuery.contains("%")) {
			throw new IllegalArgumentException("Query is incorrectly encoded: "
					+ decodedQuery);
		}

		String booleanQuery = "";
		String escapedQuery = "";
		int numAndClauses = 0;
		String splits[] = decodedQuery.split("&");
		for (int i = 0; i < splits.length; i++) {
			if (0 == splits[i].indexOf("bq=")) {
				if (0 < booleanQuery.length()) {
					booleanQuery += " ";
				}
				String bqValue = splits[i].substring(3);
				if (0 == bqValue.indexOf("(and ")) {
					bqValue = bqValue.substring(5, bqValue.length() - 1);
					numAndClauses++;
				}
				booleanQuery += bqValue;
				numAndClauses++;
			} else {
				if (0 < escapedQuery.length()) {
					escapedQuery += "&";
				}
				escapedQuery += splits[i];
			}
		}

		if (0 != booleanQuery.length()) {
			if (1 < numAndClauses) {
				booleanQuery = "(and " + booleanQuery + ")";
			}

			if (0 < escapedQuery.length()) {
				escapedQuery += "&";
			}
			escapedQuery += "bq=" + URLEncoder.encode(booleanQuery, "UTF-8");
		}
		return escapedQuery;
	}
}
