package org.sagebionetworks.repo.model.query;

import static org.sagebionetworks.repo.model.query.SQLConstants.ALIAS_ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.ALIAS_ANNO_OWNER;
import static org.sagebionetworks.repo.model.query.SQLConstants.ALIAS_EXPRESSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.ALIAS_SORT;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_DOUBLE;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_LONG;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_OWNER;
import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_STRING;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_ANNO_ATTRIBUTE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_ANNO_IS_PRIVATE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_ANNO_VALUE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_EVALID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_SUBID;
import static org.sagebionetworks.repo.model.query.SQLConstants.PREFIX_SUBSTATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.jdo.SizeLimitRowMapper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class QueryDAOImpl implements QueryDAO {
	
	@Autowired
	AccessControlListDAO accessControlListDAO;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	private static final long MAX_BYTES_PER_QUERY = 
			StackConfigurationSingleton.singleton().getMaximumBytesPerQueryResult();
	private static Logger log = LogManager.getLogger(QueryDAOImpl.class);
	
	private static final String ATTRIBUTE_PARAM = "ATTRIBUTE";
	private static final String SCOPE_PARAM = "SCOPE_ID";
	private static final String LONG_ATTR_NAME = "LONG_ATTR";
	private static final String DBBL_ATTR_NAME = "DBBL_ATTR";
	private static final String STRG_ATTR_NAME = "STRG_ATTR";
	
	private static final String FIND_ATTRIBUTE_SQL = 
			"select distinct s."+COL_ANNO_ATTRIBUTE+" '"+STRG_ATTR_NAME+"', l."+COL_ANNO_ATTRIBUTE+" '"+LONG_ATTR_NAME+"', d."+COL_ANNO_ATTRIBUTE+" '"+DBBL_ATTR_NAME+"' from "+
			PREFIX_SUBSTATUS+ANNO_OWNER+" o "+
			"left outer join "+PREFIX_SUBSTATUS+ANNO_STRING+" s on (s."+COL_SUBSTATUS_ANNO_SUBID+"=o."+COL_SUBSTATUS_ANNO_SUBID+" and s."+COL_ANNO_ATTRIBUTE+"=:"+ATTRIBUTE_PARAM+") "+
			"left outer join "+PREFIX_SUBSTATUS+ANNO_LONG+" l on (l."+COL_SUBSTATUS_ANNO_SUBID+"=o."+COL_SUBSTATUS_ANNO_SUBID+" and l."+COL_ANNO_ATTRIBUTE+"=:"+ATTRIBUTE_PARAM+") "+
			"left outer join "+PREFIX_SUBSTATUS+ANNO_DOUBLE+" d on (d."+COL_SUBSTATUS_ANNO_SUBID+"=o."+COL_SUBSTATUS_ANNO_SUBID+" and d."+COL_ANNO_ATTRIBUTE+"=:"+ATTRIBUTE_PARAM+") "+
			"where o."+COL_SUBSTATUS_ANNO_EVALID+"=:"+SCOPE_PARAM+"";
	
	private static ObjectType getObjectTypeFromQueryObjectType(QueryObjectType qot) {
		if (qot==QueryObjectType.EVALUATION) {
			return ObjectType.EVALUATION;
		} else {
			throw new IllegalArgumentException(qot.name());
		}
	}
	
	@Override
	public QueryTableResults executeQuery(BasicQuery userQuery, UserInfo userInfo)
			throws DatastoreException, NotFoundException, JSONObjectAdapterException, 
			DataAccessException {
		// Validate incoming query
		nullCheckQuery(userQuery);
		
		// Determine query object type and access rights
		QueryObjectType objType = QueryTools.getQueryObjectType(userQuery);
		String objId = QueryTools.getQueryObjectId(userQuery);
		if (!canAccess(userInfo, objId, objType)) {
			throw new UnauthorizedException(
					"Insufficient permissions to read from Synapse object " + objId);
		}
		boolean includePrivate = canAccessPrivate(userInfo, objId, objType);
		
		FieldType sortFieldType = null;
		if (userQuery.getSort()!=null) {
			sortFieldType = findFieldTypeForAttribute(Long.parseLong(objId), userQuery.getSort());
			// if the sort-by attribute doesn't occur, then we simply don't sort
			if (sortFieldType==null) userQuery.setSort(null);
		}
		
		// Build the SQL queries
		Map<String, Object> queryParams = new HashMap<String, Object>();
		StringBuilder countQuery = new StringBuilder();
		StringBuilder fullQuery = new StringBuilder();
		buildQueryStrings(userQuery, objType, objId, userInfo, includePrivate, 
				countQuery, fullQuery, queryParams, sortFieldType);
		
		String countQueryString = countQuery.toString();
		String fullQueryString = fullQuery.toString();
		
		// Execute the count query
		long count = namedJdbcTemplate.queryForObject(countQueryString, queryParams, Long.class);
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
		List<Map<String, Object>> results = namedJdbcTemplate.query(
				fullQueryString, queryParams, sizeLimitMapper);
		Long userId = userInfo.getId();
		
		// Log query stats
		if (log.isDebugEnabled()) {
			log.debug("user: " + userId + " query: " + fullQueryString);
			log.debug("user: " + userId + " parameters: " + queryParams);
		}
		if (log.isInfoEnabled()) {
			log.info("user: " + userId +  " query bytes returned: " + sizeLimitMapper.getBytesUsed());
		}

		// Create the results
		return QueryTools.translateResults(results, count, userQuery.getSelect(), includePrivate);
	}
	
	/**
	 * When we sort by a given attribute we need to determine what type its values are
	 * @param scopeId the scope of the attribute (e.g. the Evaluation)
	 * @param attribute
	 * @return the FieldType of the attributes or null if the attribute doesn't appear in the given scope
	 */
	public FieldType findFieldTypeForAttribute(Long scopeId, String attribute) {
		MapSqlParameterSource args = new MapSqlParameterSource();
		args.addValue(ATTRIBUTE_PARAM, attribute);
		args.addValue(SCOPE_PARAM, scopeId);
		final Map<String,Boolean> map = new HashMap<String,Boolean>();
		namedJdbcTemplate.query(FIND_ATTRIBUTE_SQL, args, new RowMapper<Object>(){
			@Override
			public Object mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				if (rs.getString(LONG_ATTR_NAME)!=null) map.put(LONG_ATTR_NAME, true);
				if (rs.getString(DBBL_ATTR_NAME)!=null) map.put(DBBL_ATTR_NAME, true);
				if (rs.getString(STRG_ATTR_NAME)!=null) map.put(STRG_ATTR_NAME, true);
				return null;
			}});
		if (map.get(LONG_ATTR_NAME)!=null) return FieldType.LONG_ATTRIBUTE;
		if (map.get(DBBL_ATTR_NAME)!=null) return FieldType.DOUBLE_ATTRIBUTE;
		if (map.get(STRG_ATTR_NAME)!=null) return FieldType.STRING_ATTRIBUTE;
		return null;
	}


	/**
	 * Build the two query strings and prepare the query parameters.
	 */
	public static void buildQueryStrings(BasicQuery userQuery, QueryObjectType objType, String objId,
			UserInfo userInfo, boolean includePrivate, StringBuilder countQuery, 
			StringBuilder fullQuery, Map<String, Object> queryParams, FieldType sortFieldType) throws DatastoreException {		
		List<String> aliases = new ArrayList<String>();

		// <select>
		String selectCount = buildSelect(true);
		String selectId = buildSelect(false);
		
		// <from>
		StringBuilder from = buildFrom(objType, objId, aliases, userQuery, sortFieldType);

		// <where>
		StringBuilder where = buildWhere(objType, objId, aliases, userQuery, queryParams, includePrivate);
		
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
	private static String buildSelect(boolean isCount) {
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
	private static StringBuilder buildFrom(QueryObjectType queryObjType, String objId, 
			List<String> aliases, BasicQuery query, FieldType sortFieldType) {
		StringBuilder builder = new StringBuilder();
		builder.append("FROM");
		String tablePrefix = queryObjType.tablePrefix();
		
		// Add the Owner and Blob Annotation tables
		appendTable(builder, null, tablePrefix, ANNO_OWNER, ALIAS_ANNO_OWNER, true);
		appendTable(builder, aliases, tablePrefix, ANNO_BLOB, ALIAS_ANNO_BLOB, false);
		
		if (query.getFilters() == null) {
			query.setFilters(new ArrayList<Expression>());
		}
		
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
			String tableName = QueryTools.getTableNameForFieldType(sortFieldType);
			appendTable(builder, aliases, tablePrefix, tableName, ALIAS_SORT, false);
		}
		return builder;
	}
	
	/**
	 * Build the WHERE clause
	 */
	private static StringBuilder buildWhere(QueryObjectType queryObjType, String scopeId, List<String> aliases, 
			BasicQuery query, Map<String, Object> queryParams, boolean includePrivate) 
			throws DatastoreException {
		StringBuilder builder = new StringBuilder();
		builder.append("WHERE ");
		String joinColumn = queryObjType.joinColumn();
		
		// Inject the user-defined "FROM" filter
		builder.append(ALIAS_ANNO_OWNER);
		builder.append('.');
		builder.append(COL_SUBSTATUS_ANNO_EVALID);
		builder.append(Comparator.EQUALS.getSql());
		builder.append(scopeId);
		
		// Join the tables
		for (int i = 0; i < aliases.size(); i++) {
			appendJoin(builder, ALIAS_ANNO_OWNER, aliases.get(i), joinColumn, false);
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
	private static StringBuilder buildSort(BasicQuery query) throws DatastoreException {		
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

	private boolean canAccess(UserInfo userInfo, String objectId, QueryObjectType objType) {
		if (userInfo.isAdmin()) return true;
		return accessControlListDAO.canAccess(userInfo.getGroups(), objectId, getObjectTypeFromQueryObjectType(objType), ACCESS_TYPE.READ);
	}

	private boolean canAccessPrivate(UserInfo userInfo, String objectId, QueryObjectType objType) {
		if (userInfo.isAdmin()) return true;
		return accessControlListDAO.canAccess(
				userInfo.getGroups(), objectId, getObjectTypeFromQueryObjectType(objType), objType.getPrivateAccessType());
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
		String operator = comparator.getSql();
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
