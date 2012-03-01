package org.sagebionetworks.repo.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.backup.SearchDocumentDriverImpl;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;

/**
 * Helper functions for search functionality
 * 
 * @author deflaux
 * 
 */
public class SearchHelper {

	private static final Logger log = Logger.getLogger(SearchHelper.class
			.getName());

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
			authorizationFilter += SearchDocumentDriverImpl.ACL_INDEX_FIELD
					+ ":'" + group.getName() + "'";
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

		// Make sure the url is well-formed so that we can correctly clean it up
		String decodedQuery = URLDecoder.decode(query, "UTF-8");
		if (decodedQuery.contains("%") || decodedQuery.contains("+")) {
			throw new IllegalArgumentException("Query is incorrectly encoded: "
					+ decodedQuery);
		}

		String booleanQuery = "";
		String escapedQuery = "";
		int numAndClauses = 0;
		String splits[] = decodedQuery.split("&");
		for (int i = 0; i < splits.length; i++) {
			if (splits[i].startsWith("bq=")) {
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
				if (splits[i].startsWith("q=")) {
					escapedQuery += "q="
							+ URLEncoder
									.encode(splits[i].substring(2), "UTF-8");
				} else {
					escapedQuery += splits[i];
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
}
