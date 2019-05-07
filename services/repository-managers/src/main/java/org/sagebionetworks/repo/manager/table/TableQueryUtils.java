package org.sagebionetworks.repo.manager.table;

import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.util.ValidateArgument;

public class TableQueryUtils {

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.alias("Query", Query.class)
			.allowTypes(Query.class)
			.build();

	/**
	 * Extract a query from a next page token.
	 * @param nextPageToken
	 * @return
	 */
	public static Query createQueryFromNextPageToken(QueryNextPageToken nextPageToken) {
		if (nextPageToken == null || StringUtils.isEmpty(nextPageToken.getToken())) {
			throw new IllegalArgumentException("Next page token cannot be empty");
		}
		try {
			Query query = (Query) X_STREAM.fromXML(nextPageToken.getToken(), new Query());
			return query;
		} catch (Throwable t) {
			throw new IllegalArgumentException("Not a valid next page token", t);
		}
	}
	
	/**
	 * Create a QueryNextPageToken from a sql string.
	 * @param sql
	 * @param nextOffset
	 * @param limit
	 * @param isConsistent
	 * @return
	 */
	public static QueryNextPageToken createNextPageToken(String sql, List<SortItem> sortList, Long nextOffset, Long limit, boolean isConsistent, List<FacetColumnRequest> selectedFacets) {
		Query query = new Query();
		query.setSql(sql);
		query.setSort(sortList);
		query.setOffset(nextOffset);
		query.setLimit(limit);
		query.setIsConsistent(isConsistent);
		query.setSelectedFacets(selectedFacets);

		StringWriter writer = new StringWriter(sql.length() + 50);
		X_STREAM.toXML(query, writer);
		QueryNextPageToken nextPageToken = new QueryNextPageToken();
		nextPageToken.setToken(writer.toString());
		return nextPageToken;
	}
	
	/**
	 * Get the TableId from a CacheableRequestBody
	 * 
	 * @param body
	 * @return
	 */
	public static String getTableIdFromRequestBody(CacheableRequestBody body){
		if(body == null){
			throw new IllegalArgumentException("Body cannot be null");
		}
		if(body instanceof DownloadFromTableRequest){
			return getTableId((DownloadFromTableRequest)body);
		}else if(body instanceof QueryBundleRequest){
			return getTableId((QueryBundleRequest)body);
		}else if(body instanceof QueryNextPageToken){
			return getTableId((QueryNextPageToken)body);
		}else{
			throw new IllegalArgumentException("Unknown request body type: "+body.getClass());
		}
	}
	
	/**
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	public static String getTableId(QueryNextPageToken body){
		ValidateArgument.required(body, "QueryNextPageToken");
		Query query = createQueryFromNextPageToken(body);
		return extractTableIdFromSql(query.getSql());
	}
	
	/**
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	public static String getTableId(QueryBundleRequest body){
		ValidateArgument.required(body, "QueryBundleRequest");
		if(body.getQuery() == null){
			throw new IllegalArgumentException("QueryBundleRequest.query cannot be null");
		}
		return extractTableIdFromSql(body.getQuery().getSql());
	}
	
	/**
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	public static String getTableId(DownloadFromTableRequest body){
		ValidateArgument.required(body, "DownloadFromTableRequest");
		return extractTableIdFromSql(body.getSql());
	}
	
	/**
	 * Helper to determine the tableId from the SQL.
	 * @param sql
	 * @return
	 */
	public static String extractTableIdFromSql(String sql){
		ValidateArgument.required(sql, "SQL string");
		try {
			return new TableQueryParser(sql).querySpecification().getTableExpression().getFromClause().getTableReference().getTableName();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
