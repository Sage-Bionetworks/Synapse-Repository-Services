package org.sagebionetworks.web.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.TreeMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.sagebionetworks.web.server.UrlTemplateUtil;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;

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
	public static final String KEY_WHERE = "whereKey";
	
	/**
	 * The query key words
	 */
	public static final String SELECT = "select";
	public static final String FROM = "from";
	public static final String WHERE = "where";
	public static final String LIMIT = "limit";
	public static final String OFFSET = "offset";
	public static final String ASCENDING = "ASC";
	public static final String DESCENDING = "DESC";
	public static final String AND = "and";
	public static final String ORDER = "order";
	public static final String BY = "by";
	public static final String STAR = "*";
	public static final String WHITE_SPACE = "+";
	
	public static final String PATH_QUERY = "repo/v1/query?query=";

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
		
		// Build up the template
		builder.append(SELECT);
		builder.append(WHITE_SPACE);
		// For now all queries are select *
		builder.append(STAR);
		builder.append(WHITE_SPACE);
		if(params.fetchType() == null) throw new IllegalArgumentException("From type cannot be null");
		builder.append(FROM);
		builder.append(WHITE_SPACE);
		// Add the regular expression to the key
		appendBracketedKeyAndBindValue(builder, KEY_FROM, map, params.fetchType().name());
		// Do we have a where clause?
		if(params.getWhere() != null){
			builder.append(WHITE_SPACE);
			builder.append(WHERE);
			builder.append(WHITE_SPACE);
			appendBracketedKeyAndBindValue(builder, KEY_WHERE, map, WhereCondition.toSql(params.getWhere(), WHITE_SPACE));
		}
		// Do we have a sort string?
		if(params.getSort() != null){
			builder.append(WHITE_SPACE);
			builder.append(ORDER);
			builder.append(WHITE_SPACE);
			builder.append(BY);
			builder.append(WHITE_SPACE);
			builder.append(params.getSort());
			builder.append(WHITE_SPACE);
			String ascendingString = null;
			if(params.isAscending()){
				ascendingString = ASCENDING;
			}else{
				ascendingString = DESCENDING;
			}
			appendBracketedKeyAndBindValue(builder, KEY_ASCND, map, ascendingString);
		}
		// Set the offset and limit
		int offset = params.getOffset();
		if(offset < 1){
			offset = 1;
		}
		builder.append(WHITE_SPACE);
		builder.append(LIMIT);
		builder.append(WHITE_SPACE);
		appendBracketedKeyAndBindValue(builder, LIMIT, map, Integer.toString(params.getLimit()));
		builder.append(WHITE_SPACE);
		builder.append(OFFSET);
		builder.append(WHITE_SPACE);
		appendBracketedKeyAndBindValue(builder, OFFSET, map, Integer.toString(offset));
		String url = builder.toString();
		return UrlTemplateUtil.expandUrl(url, map);
	}
	
	/**
	 * Will append a bracketed key: "{<key>}" and bind the value to the map
	 * @param builder
	 * @param key
	 */
	private static void appendBracketedKeyAndBindValue(StringBuilder builder, String key, Map<String, String> map, String value){
		builder.append("{");
		builder.append(key);
		builder.append("}");
		map.put(key, value);
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
		queryString = queryString.replaceAll("%20", " ");
		queryString = queryString.replaceAll("%3E", ">");
		queryString = queryString.replaceAll("\\+", " ");
		queryString = queryString.replaceAll("\"", "");
		StringTokenizer tokenizer = new StringTokenizer(queryString, " ");
		while(tokenizer.hasMoreTokens()){
			String token = tokenizer.nextToken();
			String tokenLower = token.toLowerCase();
			if(FROM.equals(tokenLower)){
				params.setFromType(tokenizer.nextToken());
				continue;
			}else if(LIMIT.equals(tokenLower)){
				params.setLimit(Integer.parseInt(tokenizer.nextToken()));
				continue;
			}else if(OFFSET.equals(tokenLower)){
				params.setOffset(Integer.parseInt(tokenizer.nextToken()));
				continue;
			}else if(ORDER.equals(tokenLower)){
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
			}else if(WHERE.equals(tokenLower) || AND.equals(tokenLower) ){
				String id = tokenizer.nextToken();
				WhereOperator opperator = WhereOperator.fromSql(tokenizer.nextToken());
				String value = tokenizer.nextToken();
				params.addWhere(new WhereCondition(id, opperator, value));
			}
		}
		// Done
		return params;
	}

}
