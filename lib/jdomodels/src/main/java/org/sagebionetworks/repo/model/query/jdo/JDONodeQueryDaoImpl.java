package org.sagebionetworks.repo.model.query.jdo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.FieldTypeCache;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Implementation of the NodeQueryDao using JDO.
 * 
 * @author jmhill
 * 
 */

@SuppressWarnings("rawtypes")
public class JDONodeQueryDaoImpl implements NodeQueryDao {

	static private Logger log = LogManager.getLogger(JDONodeQueryDaoImpl.class);
	
	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private NodeAliasCache aliasCache;

	/**
	 * The maximum number of bytes allowed per query.
	 */
	public static final long MAX_BYTES_PER_QUERY = StackConfiguration.getMaximumBytesPerQueryResult();
	
	
	/**
	 * Execute the actual query
	 */
	@Override
	public NodeQueryResults executeQuery(BasicQuery query, UserInfo userInfo) throws DatastoreException {
		try {
			return executeQueryImpl(query, userInfo);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Execute a count query.
	 */
	@Override
	public long executeCountQuery(BasicQuery query, UserInfo userInfo) throws DatastoreException {
		return executeCountQueryImpl(query, userInfo);
	}

	/**
	 * Depend on a dao to get these types.
	 * @param name
	 * @return
	 */
	private FieldType getFieldType(String name) {
		// Look up this name
		return FieldTypeCache.getInstance().getTypeForName(name);
	}

	/**
	 * Run the actual query.
	 * 
	 * @param pm
	 * @param in
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	private NodeQueryResults executeQueryImpl(BasicQuery in, UserInfo userInfo)
			throws DatastoreException, NotFoundException {
		// Prepare the parameters
		Map<String, Object> parameters = new HashMap<String, Object>();
		// This will contain the count query.
		StringBuilder countQuery = new StringBuilder();
		// This will contain the full query
		StringBuilder fullQuery = new StringBuilder();
		boolean columnsExist = buildQueryStrings(in, userInfo, countQuery, fullQuery, parameters);
		if(!columnsExist){
			// For this case there will be no results
			return new NodeQueryResults();
		}
		// Run the count query
		long count = this.simpleJdbcTemplate.queryForLong(countQuery.toString(), parameters);
		// Now execute the non-count query
		SizeLimitRowMapper sizeLimitMapper = new SizeLimitRowMapper(MAX_BYTES_PER_QUERY);
		List<Map<String, Object>> results = simpleJdbcTemplate.query(fullQuery.toString(), sizeLimitMapper, parameters);
		Long userId = userInfo.getId();
		// Build the results based on on the select
		if(log.isDebugEnabled()){
			log.debug("user: "+userId+ " Query: "+fullQuery.toString());
			log.debug("user: "+userId+ " parameters: "+parameters);
		}
		if(log.isInfoEnabled()){
			log.debug("user: "+userId+ " query bytes returned: "+sizeLimitMapper.getBytesUsed()+" bytes");
		}
		// Create the results
		return QueryUtils.translateResults(results, count, in.getSelect());
	}
	
	/**
	 * Execute a count query.
	 * @param query
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 */
	private long executeCountQueryImpl(BasicQuery query, UserInfo userInfo) throws DatastoreException {
		// Prepare the parameters
		Map<String, Object> parameters = new HashMap<String, Object>();
		// This will contain the count query.
		StringBuilder countQuery = new StringBuilder();
		// This will contain the full query
		StringBuilder fullQuery = new StringBuilder();
		boolean columnsExist = buildQueryStrings(query, userInfo, countQuery, fullQuery, parameters);
		if(!columnsExist){
			// For this case there will be no results
			return 0;
		}
		// Run the count query
		long count = this.simpleJdbcTemplate.queryForLong(countQuery.toString(), parameters);
		return count;
	}

	/**
	 * Builds the two query strings and prepares the query parameters.
	 * @param in
	 * @param userInfo
	 * @param countQuery
	 * @param fullQuery
	 * @param parameters
	 * @throws DatastoreException
	 * @return True, if the query is valid and can be run, else false.
	 */
	private boolean buildQueryStrings(BasicQuery in, UserInfo userInfo, StringBuilder countQuery, StringBuilder fullQuery,
			Map<String, Object> parameters) throws DatastoreException {
		// Add a filter on type if needed
		if(in.getFrom() != null){
			// Add the type to the filter
			List<Short> ids = aliasCache.getAllNodeTypesForAlias(in.getFrom());
			in.addExpression(new Expression(new CompoundId(null, SqlConstants.TYPE_COLUMN_NAME), Comparator.IN, ids));
		}
		
		// A count query is composed of the following parts
		// <select> + <from> + <authorization_filter> + <where>
		// The real query is composed of the following parts
		// <select> + <from> + <authorization_filter> + <where>+ <orderby> + <paging>

		// Build select
		String selectCount = buildSelect(true, in.getSelect());
		String selectId = buildSelect(false, in.getSelect());
		// The following are the parts of the query.
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		StringBuilder orderByClause = new StringBuilder();

		parameters.put(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.ENTITY.name());

		// Build the authorization filter
		String authorizationFilter = QueryUtils.buildAuthorizationFilter(userInfo.isAdmin(), userInfo.getGroups(), parameters,
				SqlConstants.NODE_ALIAS, 0);

		try {
			// Build the from
			Map<String, FieldType> aliasMap = buildFrom(from, in);
			// Build the where
			buildWhere(where, authorizationFilter, aliasMap, parameters, in);
			
			// These two get built at the same time
			if (in.getSort() != null) {
				buildAllSorting(orderByClause,
						in.getSort(), in.isAscending());
			}

		} catch (AttributeDoesNotExistException e) {
			// log this and return an empty result
			log.warn(e.getMessage(), e);
			// Return an empty result
			return false;
		}
		// Build the paging
		String paging = QueryUtils.buildPaging(in.getOffset(), in.getLimit(), parameters);

		// Build the SQL strings
		// Count
		countQuery.append(selectCount);
		countQuery.append(" ");
		countQuery.append(from);
		countQuery.append(" ");
		countQuery.append(where);

		// Now build the full query
		fullQuery.append(selectId);
		fullQuery.append(" ");
		fullQuery.append(from);
		fullQuery.append(" ");
		fullQuery.append(where);
		fullQuery.append(" ");
		fullQuery.append(orderByClause);
		fullQuery.append(" ");
		fullQuery.append(paging);
		return true;
	}
	
	/**
	 * Helper to get the count from various objects
	 * 
	 * @param countObject
	 * @return
	 */
	private long extractCount(Object countObject) {
		if (countObject == null)
			throw new IllegalArgumentException("Count cannot be null");
		if (countObject instanceof Long) {
			return (Long) countObject;
		} else if (countObject instanceof Integer) {
			return ((Integer) countObject).intValue();
		} else {
			throw new IllegalArgumentException(
					"Cannot extract count from object: "
							+ countObject.getClass().getName());
		}
	}

	/**
	 * Build all parts involved in sorting
	 * 
	 * @param outerJoinAttributeSort
	 * @param orderByClause
	 * @param sort
	 * @param ascending
	 * @param parameters
	 * @throws DatastoreException
	 */
	private void buildAllSorting(StringBuilder orderByClause, String sort, boolean ascending) throws DatastoreException,
			AttributeDoesNotExistException {
		// The first thing we need to do is determine if we are sorting on a
		// primary field or an attribute.
		String ascString = null;
		if (ascending) {
			ascString = "asc";
		} else {
			ascString = "desc";
		}
		String sortColumnName = null;
		FieldType type = getFieldType(sort);
		String alias = null;
		if (FieldType.DOES_NOT_EXIST == type)
			throw new AttributeDoesNotExistException("No attribute found for: " + sort);
		if (FieldType.PRIMARY_FIELD == type) {
			NodeField nodeField = NodeField.getFieldForName(sort);
			sortColumnName = nodeField.getColumnName();
			alias = nodeField.getTableAlias();
		} else {
			// We are sorting on an attribute which means we need a left outer
			// join.
			sortColumnName = SqlConstants.ANNOTATION_VALUE_COLUMN;
			alias = SqlConstants.SORT_ALIAS;
		}
		// Add the order by
		orderByClause.append(" order by ");
		orderByClause.append(alias);
		orderByClause.append(".");
		orderByClause.append(sortColumnName);
		orderByClause.append(" ");
		orderByClause.append(ascString);
	}
	


	/**
	 * Build the select clause
	 * 
	 * @param alias
	 * @return
	 */
	private String buildSelect(boolean isCount, List<String> select) {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		if (isCount) {
			builder.append("count(");
		}
		builder.append(SqlConstants.NODE_ALIAS);
		builder.append(".");
		builder.append(SqlConstants.COLUMN_ID);
		if (isCount) {
			builder.append(")");
		}
		// Now if the select is null then we must select the rest of the data
		if(!isCount){
			if(select == null || select.size() == 0){
				// Add the primary fields
				for(int i=0; i<NodeField.values().length; i++){
					NodeField field = NodeField.values()[i];
					addNodeFieldToSelect(builder, field);
				}
				// We also need to select the annotations
				addAnnotationsToSelect(builder);
			}else{
				// Only select what we need to.
				boolean isAnnotaionsSelected = false;
				for(String selectName: select){
					// First is this a primary field?
					try{
						NodeField field = NodeField.getFieldForName(selectName);
						addNodeFieldToSelect(builder, field);
					}catch (IllegalArgumentException e){
						// This field must be an annotation.
						if(!isAnnotaionsSelected){
							addAnnotationsToSelect(builder);
							isAnnotaionsSelected = true;
						}
					}
				}
			}
		}
		return builder.toString();
	}

	private void addAnnotationsToSelect(StringBuilder builder) {
		builder.append(", ");
		builder.append(SqlConstants.REVISION_ALIAS);
		builder.append(".");
		builder.append(SqlConstants.COL_REVISION_ANNOS_BLOB);
	}

	private void addNodeFieldToSelect(StringBuilder builder, NodeField field) {
		builder.append(", ");
		builder.append(field.getTableAlias());
		builder.append(".");
		builder.append(field.getColumnName());
		builder.append(" as ");
		builder.append(field.getFieldName());
	}

	/**
	 * Build up the from
	 * 
	 * @param builder
	 * @param from
	 * @throws AttributeDoesNotExistException 
	 */
	private Map<String, FieldType> buildFrom(StringBuilder fromBuilder, BasicQuery in) throws AttributeDoesNotExistException {
		Map<String, FieldType> aliasMap = new HashMap<String, FieldType>();
		fromBuilder.append("from");

		//Append any other table we might need
		if(in.getSort() != null){
			FieldType sortType = getFieldType(in.getSort());
			if(FieldType.PRIMARY_FIELD != sortType){
				// Add the sort
				addFrom(fromBuilder, SqlConstants.SORT_ALIAS, sortType, aliasMap);				
			}
		}
		// Add each filter
		if(in.getFilters() != null){
			for(int i=0; i< in.getFilters().size(); i++){
				// First look up the column name
				Expression expression = in.getFilters().get(i);
				CompoundId id = expression.getId();
				if (id == null)
					throw new IllegalArgumentException("Compound id cannot be null");
				FieldType type = getFieldType(id.getFieldName());
				// We only need to add a from for non-primary fileds
				if(FieldType.PRIMARY_FIELD != type){
					// try to determine the type from the parameter
//					FieldType typeFromValue = determineTypeFromValue(expression.getValue());
//					if(typeFromValue != null){
//						type = typeFromValue;
//					}
					addFrom(fromBuilder, SqlConstants.EXPRESSION_ALIAS_PREFIX+i, type, aliasMap);
				}
			}
		}
		// Add the primary fields.
		if (aliasMap.size() > 0) {
			// This is not the first so it needs a comma in the from
			fromBuilder.append(",");
		}
		// Add the revision table
		fromBuilder.append(" ");
		fromBuilder.append(SqlConstants.TABLE_REVISION);
		fromBuilder.append(" ");
		fromBuilder.append(SqlConstants.REVISION_ALIAS);
		aliasMap.put(SqlConstants.REVISION_ALIAS, FieldType.PRIMARY_FIELD);
		
		// Add this table and alais to the from clause.
		fromBuilder.append(", ");
		fromBuilder.append(SqlConstants.TABLE_NODE);
		fromBuilder.append(" ");
		fromBuilder.append(SqlConstants.NODE_ALIAS);
		aliasMap.put(SqlConstants.NODE_ALIAS, FieldType.PRIMARY_FIELD);
		

		
		// The where clause is started if there is more than one alias used
		return aliasMap;
	}
	
	/**
	 * Add each type.
	 * @param builder
	 * @param type
	 * @param first
	 * @param usedTypes
	 * @throws AttributeDoesNotExistException 
	 */
	private void addFrom(StringBuilder fromBuilder, String alias, FieldType type, Map<String, FieldType> aliasMap)
			throws AttributeDoesNotExistException {
		if (FieldType.DOES_NOT_EXIST.equals(type))
			throw new AttributeDoesNotExistException("Unknown field type: " + type);

		if (aliasMap.size() > 0) {
			// This is not the first so it needs a comma in the from
			fromBuilder.append(",");
		}
		// Add this table and alais to the from clause.
		String tableName = QueryUtils.getTableNameForFieldType(type);
		fromBuilder.append(" ");
		fromBuilder.append(tableName);
		fromBuilder.append(" ");
		fromBuilder.append(alias);
		aliasMap.put(alias, type);

	}
	
	/**
	 * Build up the from
	 * 
	 * @param builder
	 * @param from
	 * @throws AttributeDoesNotExistException 
	 * @throws DatastoreException 
	 */
	private void buildWhere(StringBuilder whereBuilder, String authorizationFilter, Map<String, FieldType> aliasMap, Map parameters,
			BasicQuery in) throws AttributeDoesNotExistException, DatastoreException {
		// We need a where clause if there is more than one table in this query or if there are any filters.
		int conditionCount = 0;
		// Add the join from node to node to node revision
		whereBuilder.append("where ");
		if (!StringUtils.isBlank(authorizationFilter)) {
			whereBuilder.append(authorizationFilter);
			whereBuilder.append(" and ");
		}
		whereBuilder.append(SqlConstants.NODE_ALIAS);
		whereBuilder.append(".");
		whereBuilder.append(SqlConstants.COL_NODE_ID);
		whereBuilder.append(" = ");
		whereBuilder.append(SqlConstants.REVISION_ALIAS);
		whereBuilder.append(".");
		whereBuilder.append(SqlConstants.COL_REVISION_OWNER_NODE);
		conditionCount++;
		whereBuilder.append(" and ");
		whereBuilder.append(SqlConstants.NODE_ALIAS);
		whereBuilder.append(".");
		whereBuilder.append(SqlConstants.COL_CURRENT_REV);
		whereBuilder.append(" = ");
		whereBuilder.append(SqlConstants.REVISION_ALIAS);
		whereBuilder.append(".");
		whereBuilder.append(SqlConstants.COL_REVISION_NUMBER);
		conditionCount++;
		
		// First add all of the join conditions to the where.
		if(aliasMap.size() > 1){
			Iterator<String> keyIt = aliasMap.keySet().iterator();
			while(keyIt.hasNext()){
				String alias = keyIt.next();
				FieldType type = aliasMap.get(alias);
				if(FieldType.PRIMARY_FIELD == type) continue;
				prepareWhereBuilder(whereBuilder, conditionCount);
				// Join this alias to the node table.
				whereBuilder.append(" ");
				whereBuilder.append(SqlConstants.NODE_ALIAS);
				whereBuilder.append(".");
				whereBuilder.append(SqlConstants.COL_NODE_ID);
				whereBuilder.append(" = ");
				whereBuilder.append(alias);
				whereBuilder.append(".");
				whereBuilder.append(SqlConstants.ANNOTATION_OWNER_ID_COLUMN);
				conditionCount++;
			}
		}
		
		// We need a condition for sorting when we are not sorting on the primary field
		if(in.getSort() != null){
			FieldType sortType = getFieldType(in.getSort());
			if(FieldType.PRIMARY_FIELD != sortType){
				prepareWhereBuilder(whereBuilder, conditionCount);
				whereBuilder.append(" ");
				whereBuilder.append(SqlConstants.SORT_ALIAS);
				whereBuilder.append(".");
				whereBuilder.append(SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN);
				whereBuilder.append(" = :sortAttName ");
				parameters.put("sortAttName", in.getSort());
				conditionCount++;
			}
		}

		// Add each filter
		if(in.getFilters() != null){
			for(int i=0; i<in.getFilters().size(); i++){
				Expression exp = in.getFilters().get(i);
				// First look up the column name
				CompoundId id = exp.getId();
				if (id == null)
					throw new IllegalArgumentException("Compound id cannot be null");
				FieldType type = getFieldType(id.getFieldName());
				// Add where or and
				prepareWhereBuilder(whereBuilder, conditionCount);
				
				// Throw an exception if the field does not exist
				if (FieldType.DOES_NOT_EXIST == type)	throw new AttributeDoesNotExistException("No attribute found for: "+ id.getFieldName());
				if (FieldType.PRIMARY_FIELD == type) {
					// This is a simple primary field filter
					NodeField nodeField = NodeField.getFieldForName(exp.getId().getFieldName());
					whereBuilder.append(" ");
					whereBuilder.append(nodeField.getTableAlias());
					whereBuilder.append(".");
					whereBuilder.append(nodeField.getColumnName());
					whereBuilder.append(" ");
					if(exp.getValue() == null){
						whereBuilder.append(" IS NULL ");
					}else{
						whereBuilder.append(SqlConstants.getSqlForComparator(exp.getCompare()));
						if(Comparator.IN == exp.getCompare()){
							whereBuilder.append("(");
						}
						whereBuilder.append(" :");
						// Add a bind variable
						String bindKey = "expKey" + i;
						whereBuilder.append(bindKey);
						// Bind the value to the parameters
						if((NodeField.PARENT_ID == nodeField) || (NodeField.ID == nodeField) || (NodeField.BENEFACTOR_ID == nodeField)) {
							parameters.put(bindKey, KeyFactory.stringToKey(exp.getValue().toString()));
						}
						else {
							parameters.put(bindKey, exp.getValue());
						}
						if(Comparator.IN == exp.getCompare()){
							whereBuilder.append(")");
						}
					}
				} else {
					// This is not a primary field
					String attTableName = QueryUtils.getTableNameForFieldType(type);
					whereBuilder.append(" ");
					String alais = SqlConstants.EXPRESSION_ALIAS_PREFIX+i;
					whereBuilder.append(alais);
					whereBuilder.append(".");
					whereBuilder.append(SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN);
					whereBuilder.append(" = :");
					String attNameKey = "attName" + i;
					whereBuilder.append(attNameKey);
					// Bind the key
					parameters.put(attNameKey, exp.getId().getFieldName());
					whereBuilder.append(" and ");
					whereBuilder.append(alais);
					whereBuilder.append(".");
					whereBuilder.append(SqlConstants.ANNOTATION_VALUE_COLUMN);
					whereBuilder.append(" ");
					whereBuilder.append(SqlConstants.getSqlForComparator(exp.getCompare()));
					whereBuilder.append(" :");
					String valueKey = "valeKey" + i;
					whereBuilder.append(valueKey);
					// Bind the value
					parameters.put(valueKey, exp.getValue());
				}
				conditionCount++;
			}
		}
	}

	/**
	 * Add the where or and depending on the count.
	 * @param whereBuilder
	 * @param conditionCount
	 */
	private void prepareWhereBuilder(StringBuilder whereBuilder,
			int conditionCount) {
		if(conditionCount == 0){
			whereBuilder.append("where");
		}else{
			whereBuilder.append(" and");
		}
	}

}
