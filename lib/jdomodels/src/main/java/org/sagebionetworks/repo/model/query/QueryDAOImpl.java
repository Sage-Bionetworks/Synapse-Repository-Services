package org.sagebionetworks.repo.model.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.jdo.SizeLimitRowMapper;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import static org.sagebionetworks.repo.model.query.SQLConstants.*;

public class QueryDAOImpl implements QueryDAO {
	
	@Autowired
	AccessControlListDAO accessControlListDAO;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final long MAX_BYTES_PER_QUERY = 
			StackConfiguration.getMaximumBytesPerQueryResult();
	private static Log log = LogFactory.getLog(QueryDAOImpl.class);
	
	@Override
	public QueryTableResults executeQuery(BasicQuery userQuery, UserInfo userInfo)
			throws DatastoreException, NotFoundException, JSONObjectAdapterException, 
			DataAccessException {
		// Validate incoming query
		nullCheckQuery(userQuery);
		
		// Determine query object type and access rights
		QueryObjectType objType = QueryTools.getQueryObjectType(userQuery);
		String objId = QueryTools.getQueryObjectId(userQuery);
		if (!canAccess(userInfo, objId)) {
			throw new UnauthorizedException(
					"Insufficient permissions to read from Synapse object " + objId);
		}
		boolean includePrivate = canAccessPrivate(userInfo, objId, objType);
		
		// Build the SQL queries
		Map<String, Object> queryParams = new HashMap<String, Object>();
		StringBuilder countQuery = new StringBuilder();
		StringBuilder fullQuery = new StringBuilder();
		buildQueryStrings(userQuery, objType, objId, userInfo, includePrivate, 
				countQuery, fullQuery, queryParams);
		
		// Execute the count query
		long count = simpleJdbcTemplate.queryForLong(countQuery.toString(), queryParams);
		if (count == 0) {
			// no results
			QueryTableResults results = new QueryTableResults();
			results.setHeaders(new ArrayList<String>());
			results.setRows(new ArrayList<Row>());
			results.setTotalNumberOfResults(0L);
			return results;
		}
		
		// Execute the full query
		SizeLimitRowMapper sizeLimitMapper = new SizeLimitRowMapper(MAX_BYTES_PER_QUERY);
		List<Map<String, Object>> results = simpleJdbcTemplate.query(
				fullQuery.toString(), sizeLimitMapper, queryParams);
		String userId = null;
		if (userInfo.getUser() != null) {
			userId = userInfo.getUser().getUserId();
		}
		
		// Log query stats
		if (log.isDebugEnabled()) {
			log.debug("user: " + userId + " query: " + fullQuery.toString());
			log.debug("user: " + userId + " parameters: " + queryParams);
		}
		if (log.isInfoEnabled()) {
			log.info("user: " + userId +  " query bytes returned: " + sizeLimitMapper.getBytesUsed());
		}

		// Create the results
		return QueryTools.translateResults(results, count, userQuery.getSelect(), includePrivate);
	}

	/**
	 * Build the two query strings and prepare the query parameters.
	 */
	private void buildQueryStrings(BasicQuery userQuery, QueryObjectType objType, String objId,
			UserInfo userInfo, boolean includePrivate, StringBuilder countQuery, 
			StringBuilder fullQuery, Map<String, Object> queryParams) throws DatastoreException {		
		List<String> aliases = new ArrayList<String>();

		// <select>
		String selectCount = buildSelect(true);
		String selectId = buildSelect(false);
		
		// <from>
		StringBuilder from = buildFrom(objType, objId, aliases, userQuery);

		// <where>
		StringBuilder where = buildWhere(objType, aliases, userQuery, queryParams, includePrivate);
		
		// <sort>
		StringBuilder sort = buildSort(userQuery);
		
		// <paging>
		String paging = QueryTools.buildPaging(
				userQuery.getOffset(), userQuery.getLimit(), queryParams);

		// Assemble the queries
		countQuery.append(selectCount);
		countQuery.append(" ");
		countQuery.append(from);
		countQuery.append(" ");
		countQuery.append(where);

		fullQuery.append(selectId);
		fullQuery.append(" ");
		fullQuery.append(from);
		fullQuery.append(" ");
		fullQuery.append(where);
		fullQuery.append(" ");
		fullQuery.append(sort);
		fullQuery.append(" ");
		fullQuery.append(paging);
	}

	/**
	 * Build the SELECT clause
	 */
	private String buildSelect(boolean isCount) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		if (isCount) {
			builder.append("count(");
		}
		builder.append(ALIAS_ANNO_BLOB);
		builder.append(".");
		builder.append(ANNO_BLOB);
		if (isCount) {
			builder.append(")");
		}
		return builder.toString();
	}

	/**
	 * Build the FROM clause
	 */
	private StringBuilder buildFrom(QueryObjectType queryObjType, String objId, 
			List<String> aliases, BasicQuery query) {
		StringBuilder builder = new StringBuilder();
		builder.append("FROM");
		String tablePrefix = queryObjType.tablePrefix();
		
		// Add the Owner and Blob Annotation tables
		appendTable(builder, null, tablePrefix, ANNO_OWNER, ALIAS_ANNO_OWNER, true);
		appendTable(builder, aliases, tablePrefix, ANNO_BLOB, ALIAS_ANNO_BLOB, false);
		
		// Inject the user-defined "FROM" filter
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_SCOPE_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, KeyFactory.stringToKey(objId));
		if (query.getFilters() == null) {
			query.setFilters(new ArrayList<Expression>());
		}
		query.getFilters().add(exp);
		
		// Add the typed table for each filter
		for (int i = 0; i < query.getFilters().size(); i++) {
			// First look up the column name
			Expression expression = query.getFilters().get(i);
			if (expression.getId() == null) {
				throw new IllegalArgumentException("Expression key cannot be null");
			}
			FieldType type = QueryTools.getFieldType(expression.getValue());
			String tableName = QueryTools.getTableNameForFieldType(type);
			String alias = ALIAS_EXPRESSION + i;
			appendTable(builder, aliases, tablePrefix, tableName, alias, false);
		}
		
		// Add the typed table for the sort
		if (query.getSort() != null) {
			// TODO: read type "hint" from query to sort as Double or Long
			FieldType type = FieldType.STRING_ATTRIBUTE;
			String tableName = QueryTools.getTableNameForFieldType(type);
			appendTable(builder, aliases, tablePrefix, tableName, ALIAS_SORT, false);
		}
		return builder;
	}
	
	/**
	 * Build the WHERE clause
	 */
	private StringBuilder buildWhere(QueryObjectType queryObjType, List<String> aliases, 
			BasicQuery query, Map<String, Object> queryParams, boolean includePrivate) 
			throws DatastoreException {
		StringBuilder builder = new StringBuilder();
		builder.append("WHERE ");
		String joinColumn = queryObjType.joinColumn();
		
		// Join the tables
		for (int i = 0; i < aliases.size(); i++) {
			appendJoin(builder, ALIAS_ANNO_OWNER, aliases.get(i), joinColumn, i == 0);
		}		
		
		// Add the sort filter
		if (query.getSort() != null) {
			String paramKey = "sortAttName";
			appendFilter(builder, ALIAS_SORT, COL_ANNO_ATTRIBUTE, paramKey, false);
			queryParams.put("sortAttName", query.getSort());
		}

		// Add each filter
		if (query.getFilters() != null) {
			for (int i = 0; i < query.getFilters().size(); i++) {
				Expression expression = query.getFilters().get(i);
				
				validateExpression(expression);

				String alias = ALIAS_EXPRESSION + i;
				String paramKey = "att" + i;
				String paramVal = "val" + i;
				String paramPriv = "priv" + i;
				
				// Bind the key
				appendFilter(builder, alias, COL_ANNO_ATTRIBUTE, paramKey, false);
				queryParams.put(paramKey, expression.getId().getFieldName());
				
				// Bind the value
				Comparator comparator = expression.getCompare();
				if (expression.getValue() == null) {
					appendNullFilter(comparator, builder, alias, COL_ANNO_VALUE, false);
				} else {
					appendFilter(comparator, builder, alias, COL_ANNO_VALUE, paramVal, false);				
					queryParams.put(paramVal, expression.getValue());
				}
				
				// filter by 'isPrivate', if applicable
				if (!includePrivate) {
					appendFilter(builder, alias, COL_ANNO_IS_PRIVATE, paramPriv, false);
					queryParams.put(paramPriv, "0");
				}
			}
		}
		
		return builder;
	}

	/**
	 * Build the ORDER BY clause
	 */
	private StringBuilder buildSort(BasicQuery query) throws DatastoreException {		
		StringBuilder builder = new StringBuilder();
		if (query.getSort() != null) {	
			String direction = query.isAscending() ? "asc" : "desc";
			builder.append(" ORDER BY ");
			builder.append(ALIAS_SORT);
			builder.append(".");
			builder.append(COL_ANNO_VALUE);
			builder.append(" ");
			builder.append(direction);
		}
		return builder;
	}

	private boolean canAccess(UserInfo userInfo, String objectId) {
		return accessControlListDAO.canAccess(userInfo.getGroups(), objectId, ACCESS_TYPE.READ);
	}

	private boolean canAccessPrivate(UserInfo userInfo, String objectId, QueryObjectType objType) {
		return accessControlListDAO.canAccess(
				userInfo.getGroups(), objectId, objType.getPrivateAccessType());
	}

	/**
	 * Helper to append a SQL table to the FROM clause. Defaults to INNER JOIN.
	 */
	private static void appendTable(StringBuilder builder, List<String> aliases, 
			String prefix, String name, String alias, boolean isFirst) {
		if (!isFirst) {
			builder.append(" ");
			builder.append(" inner join ");
		}
		builder.append(" ");
		builder.append(prefix);
		builder.append(name);
		builder.append(" ");
		builder.append(alias);
		if (aliases != null) {
			aliases.add(alias);
		}
	}

	/**
	 * Helper to append a SQL table JOIN to the WHERE clause.
	 */
	private static void appendJoin(StringBuilder builder, String alias1, String alias2, 
			String joinColumn, boolean isFirst) {
		if (!isFirst) {
			builder.append(" and ");
		}
		builder.append(alias1);
		builder.append(".");
		builder.append(joinColumn);
		builder.append(" = ");
		builder.append(alias2);
		builder.append(".");
		builder.append(joinColumn);
	}

	/**
	 * Helper to append a filter to the WHERE clause. Defaults to the '=' operator.
	 */
	private static void appendFilter(StringBuilder builder, String alias, String column, 
			String paramKey, boolean isFirst) {
		// default to 'equals'
		appendFilter(Comparator.EQUALS, builder, alias, column, paramKey, isFirst);
	}

	/**
	 * Helper to append a filter to the WHERE clause. Uses the provided operator.
	 */
	private static void appendFilter(Comparator comparator, StringBuilder builder, String alias, 
			String column, String paramKey, boolean isFirst) {
		String operator = SqlConstants.getSqlForComparator(comparator);
		if (!isFirst) {
			builder.append(" and ");
		}
		builder.append(alias);
		builder.append(".");
		builder.append(column);
		builder.append(" ");
		builder.append(operator);
		builder.append(" :");
		builder.append(paramKey);
	}
	
	private static void appendNullFilter(Comparator comparator, StringBuilder builder, String alias,
			String column, boolean isFirst) {
		if (!isFirst) {
			builder.append(" and ");
		}
		builder.append(alias);
		builder.append(".");
		builder.append(column);
		builder.append(" ");
		if (comparator.equals(Comparator.EQUALS)) {
			builder.append("is null");
		} else if (comparator.equals(Comparator.NOT_EQUALS)) {
			builder.append("is not null");
		}
	}

	private static void nullCheckQuery(BasicQuery userQuery) {
		if (userQuery == null) {
			throw new IllegalArgumentException("Query cannot be null");
		}
		if (userQuery.getFrom() == null) {
			throw new IllegalArgumentException("'From' cannot be null");
		}
	}

	private static void validateExpression(Expression expression) {
		CompoundId id = expression.getId();
		if (id == null || (id.getFieldName() == null && id.getTableName() == null)) {
			throw new IllegalArgumentException("Invalid query filter: ID cannot be null");
		}
		if (expression.getValue() == null || expression.getValue() instanceof String) {
			Comparator comp = expression.getCompare();
			if (!comp.equals(Comparator.EQUALS) && !comp.equals(Comparator.NOT_EQUALS)) {
				throw new IllegalArgumentException("Invalid comparator [" + comp + "] for value [" +
						expression.getValue() + "]");
			}
		}
	}

	// for test purposes
	@Override
	public void setAclDAO(AccessControlListDAO aclDAO) {
		this.accessControlListDAO = aclDAO;
	}
}
