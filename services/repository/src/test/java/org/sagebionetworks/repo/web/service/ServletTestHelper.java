package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Helper class that formulates a mock HTTP client request for the dispatch
 * servlet.
 * 
 * @author jmhill
 * 
 */
public class ServletTestHelper {

	private static final Log log = LogFactory.getLog(ServletTestHelper.class);

	/**
	 * Get search results
	 * @throws JSONObjectAdapterException 
	 */
	public static SearchResults getSearchResults(String userId, SearchQuery query)
			throws ServletException, IOException, JSONException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json");
		request.setRequestURI("/search");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setContent(EntityFactory.createJSONStringForEntity(query).getBytes("UTF-8"));
		org.sagebionetworks.repo.web.controller.DispatchServletSingleton.getInstance().service(request, response);
		log.info("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new RuntimeException(response.getContentAsString());
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), SearchResults.class);
	}
}
