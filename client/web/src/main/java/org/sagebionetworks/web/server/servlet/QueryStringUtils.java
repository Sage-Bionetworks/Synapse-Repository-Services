package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
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
	
	public static final String PATH_QUERY = "repo/v1/query?query=";

	public static final String QUERY = "select+*+from+{"
			+ KEY_FROM
			+ "}+limit+{"
			+ KEY_LIMIT
			+ "}+offset+{"
			+ KEY_OFFSET
			+ "}";

	public static final String QUERY_WITH_SORT = "select+*+from+{"
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
	public static URI writeQueryUri(String root, SearchParameters params){
		StringBuilder builder = new StringBuilder();
		builder.append(root);
		builder.append(PATH_QUERY);
		Map<String, String> map = new TreeMap<String, String>();
		// Bind the type
		FromType type = params.fetchType();
		map.put(KEY_FROM, type.name());
		if (params.getSort() != null) {
			builder.append(QUERY_WITH_SORT);
			map.put(KEY_SORT, params.getSort());
			String value = null;
			if (params.isAscending()) {
				value = ASCENDING;
			} else {
				value = DESCENDING;
			}
			map.put(KEY_ASCND, value);
		} else {
			builder.append(QUERY);
		}
		// The limit must start at one not zero
		int offset = params.getOffset();
		if(offset < 1){
			offset = 1;
		}
		map.put(KEY_OFFSET, Integer.toString(offset));
		map.put(KEY_LIMIT, Integer.toString(params.getLimit()));
		String url = builder.toString();
		return UrlTemplateUtil.expandUrl(url, map);
	}

	/**
	 * Parse a query String into the original SearchParameters.
	 * @param queryString
	 * @return
	 * @throws URISyntaxException 
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
