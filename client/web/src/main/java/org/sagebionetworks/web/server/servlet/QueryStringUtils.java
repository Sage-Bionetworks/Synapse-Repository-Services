package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.util.TreeMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.sagebionetworks.web.server.UrlTemplateUtil;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.SearchParameters.FromType;

/**
 * Creates query string from SearchParameters, and parses a query string into SearchParameters.
 * 
 * @author jmhill
 *
 */
public class QueryStringUtils {
	
	public static final String KEY_FROM = "fromKey";
	public static final String KEY_OFFSET = "offestKey";
	public static final String KEY_LIMIT = "limitKey";
	public static final String KEY_SORT = "sortKey";
	public static final String KEY_ASCND = "ascendingKey";

	public static final String PATH_QUERY = "select+*+from+{"
			+ KEY_FROM
			+ "}+limit+{"
			+ KEY_LIMIT
			+ "}+offset+{"
			+ KEY_OFFSET
			+ "}";

	public static final String PATH_QUERY_WITH_SORT = "select+*+from+{"
			+ KEY_FROM
			+ "}+order+by+\"{"
			+ KEY_SORT
			+ "}\"+{"
			+ KEY_ASCND
			+ "}+limit+{" + KEY_LIMIT + "}+offset+{" + KEY_OFFSET + "}";

	public static final String ASCENDING = "ASC";
	public static final String DESCENDING = "DESC";
	
	/**
	 * Build up a query String from the given parameters.
	 * @param params
	 * @return
	 */
	public static String writeQueryString(SearchParameters params){
		StringBuilder builder = new StringBuilder();
		Map<String, String> map = new TreeMap<String, String>();
		// Bind the type
		FromType type = params.fetchType();
		map.put(KEY_FROM, type.name());
		if (params.getSort() != null) {
			builder.append(PATH_QUERY_WITH_SORT);
			map.put(KEY_SORT, params.getSort());
			String value = null;
			if (params.isAscending()) {
				value = ASCENDING;
			} else {
				value = DESCENDING;
			}
			map.put(KEY_ASCND, value);
		} else {
			builder.append(PATH_QUERY);
		}
		map.put(KEY_OFFSET, Integer.toString(params.getOffset()));
		// The limit must start at one not zero
		int limit = params.getLimit();
		if(limit < 1){
			limit = 1;
		}
		map.put(KEY_LIMIT, Integer.toString(params.getLimit()));
		String url = builder.toString();
		URI uri = UrlTemplateUtil.expandUrl(url, map);
		return uri.toString();
	}

	/**
	 * Parse a query String into the original SearchParameters.
	 * @param queryString
	 * @return
	 */
	public static SearchParameters parseQueryString(String queryString) {
		SearchParameters params = new SearchParameters();
		queryString = queryString.replaceAll("%22", "");
		queryString = queryString.replaceAll("\\+", " ");
		StringTokenizer tokenizer = new StringTokenizer(queryString, " ");
		while(tokenizer.hasMoreTokens()){
			String token = tokenizer.nextToken();
			String tokenLower = token.toLowerCase();
			if("from".equals(tokenLower)){
				params.setFromType(tokenizer.nextToken());
				continue;
			}else if("limit".equals(tokenLower)){
				params.setLimit(Integer.parseInt(tokenizer.nextToken()));
				continue;
			}else if("offset".equals(tokenLower)){
				params.setOffset(Integer.parseInt(tokenizer.nextToken()));
				continue;
			}else if("order".equals(tokenLower)){
				// The next token should be by
				String by = tokenizer.nextToken();
				// after the 'by' should be the key
				params.setSort(tokenizer.nextToken());
				// after the key should be asc/desc
				String ascDesc = tokenizer.nextToken();
				if(ASCENDING.equals(ascDesc)){
					params.setAscending(true);
				}else if(DESCENDING.equals(ascDesc)){
					params.setAscending(false);
				}
				continue;
			}
		}
		// Done
		return params;
	}

}
