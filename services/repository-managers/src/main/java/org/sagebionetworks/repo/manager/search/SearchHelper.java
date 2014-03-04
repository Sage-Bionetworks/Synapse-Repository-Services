package org.sagebionetworks.repo.manager.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * Helper functions for search functionality
 * 
 * @author deflaux
 * 
 */
public class SearchHelper {

	private static final Logger log = LogManager.getLogger(SearchHelper.class
			.getName());
	private static final Pattern facetFieldConstraintPattern = Pattern.compile("facet-\\w-constraints");

	/**
	 * Formulate the boolean query to enforce an access controll list for a
	 * particular user
	 * 
	 * @param userInfo
	 * @return the acl boolean query
	 * @throws DatastoreException
	 */
	public static String formulateAuthorizationFilter(UserInfo userInfo)
			throws DatastoreException {
		Set<Long> groups = userInfo.getGroups();
		if (0 == groups.size()) {
			// being extra paranoid here, this is unlikely
			throw new DatastoreException("no groups for user " + userInfo);
		}

		// Make our boolean query
		String authorizationFilter = "";
		for (Long group : groups) {
			if (0 < authorizationFilter.length()) {
				authorizationFilter += " ";
			}
			authorizationFilter += SearchDocumentDriverImpl.ACL_INDEX_FIELD
					+ ":'" + group + "'";
		}
		if (1 == groups.size()) {
			authorizationFilter = "bq=" + authorizationFilter;
		} else {
			authorizationFilter = "bq=(or " + authorizationFilter + ")";
		}
		return authorizationFilter;
	}

	/**
	 * Merge and urlescape CS boolean queries as needed. urlescapse free text
	 * queries as needed too.
	 * 
	 * @param query
	 * @return the cleaned up query string
	 * @throws UnsupportedEncodingException
	 */
	public static String cleanUpSearchQueries(String query)
			throws UnsupportedEncodingException {

		// check that query is properly encoded. If there are no '=', then likely it is not escaped properly
		if (!query.contains("=")) {
			throw new IllegalArgumentException("Query is incorrectly encoded: " + query);
		}

		
		String booleanQuery = "";
		String escapedQuery = "";
		int numAndClauses = 0;
		for(String matched : query.split("&")) {
			String[] parts = matched.split("=");
			if(parts.length != 2) {
				throw new UnsupportedEncodingException("Query parameter is malformed: "+ matched);
			}
			String key = parts[0];
			String value = URLDecoder.decode(parts[1], "UTF-8");			
			if ("bq".equals(key)) {
				if (booleanQuery.length() > 0) {
					booleanQuery += " ";
				}
				if (0 == value.indexOf("(and ")) {
					value = value.substring(5, value.length() - 1);
					numAndClauses++;
				}
				booleanQuery += value;
				numAndClauses++;
			} else {
				if (escapedQuery.length() > 0) {
					escapedQuery += "&";
				}
				if ("q".equals(key)) {
					escapedQuery += "q="
							+ URLEncoder
									.encode(value, "UTF-8");
				} else {
					escapedQuery += matched;
				}
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

	private static String escapeFacetFieldConstraintCommas(String value) {
		// TODO Auto-generated method stub
		return null;
	}
}
