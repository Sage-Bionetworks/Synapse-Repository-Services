package org.sagebionetworks.repo.model.query.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.jdo.BasicIdentifierFactory;
import org.sagebionetworks.repo.model.jdo.JDOAuthorizationDAOImpl;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoCallback;
import org.springframework.orm.jdo.JdoTemplate;

/**
 * Implementation of the NodeQueryDao using JDO.
 * 
 * @author jmhill
 * 
 */
public class JDONodeQueryDaoImpl implements NodeQueryDao {

	private static Logger log = Logger.getLogger(JDONodeQueryDaoImpl.class
			.getName());

	@Autowired
	JdoTemplate jdoTemplate;
	@Autowired
	FieldTypeDAO fieldTypeDao;

	public static final int MAX_LIMIT = 50000000; // MySQL's upper bound on
	// LIMIT
	
	private Map<String, String> classTables = null;
	private String authorizationSQL = null;
	private String adminSQL = null;
	
	private void initClassTables() {
		Map<String, Object> parameters = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		List<Object[]> resultsSet = (List<Object[]>)executeQuery("select class_name, table_name from nucleus_tables", parameters);
		classTables = new HashMap<String,String>();
		for (Object[] row : resultsSet) {
			if (row.length!=2) throw new IllegalStateException("Unexpected number of columns: "+row.length);
			classTables.put((String)row[0], (String)row[1]);
		}
	}
	
	private String getFromClassTables(String key) {
		if (!classTables.containsKey(key)) throw new IllegalStateException("ClassTables is missing "+key);
		return classTables.get(key);
	}
	
	private void initAuthorizationSQL() {
		if (classTables==null) initClassTables();
		authorizationSQL = "select distinct ra.resource_id from "+
			getFromClassTables(JDOResourceAccess.class.getName())+" ra, "+
			getFromClassTables(JDOResourceAccess.class.getName()+".accessType")+" t, "+
			getFromClassTables(JDOUserGroup.class.getName())+" ug, "+
			getFromClassTables(JDOUserGroup.class.getName()+".users")+" ugu, "+
			getFromClassTables(JDOUser.class.getName())+" u "+
			"where "+
			"ra.owner_id_oid=ug.id and ra.id=t.id_oid and t.string_ele = :accessType and "+
			" ra.resource_type='"+JDOAuthorizationDAOImpl.NODE_RESOURCE_TYPE+"' and "+
			"( "+
			// the user group contains the given user
			"(ug.id = ugu.id_oid and ugu.long_ele=u.id and u.user_id = :userName) or"+
			// the user group is Public
			"(ug.is_system_group=true and ug.is_individual=false and ug.name='"+AuthorizationConstants.PUBLIC_GROUP_NAME+"')"+
			")"
			;
	
	}
	
	private void initAdminSQL() {
		if (classTables==null) initClassTables();
		// count the users matching the given user name in the admin group
		// thus it returns 1 if an admin, 0 otherwise
		adminSQL = "select count(*) from "+
			getFromClassTables(JDOUserGroup.class.getName())+" ug, "+
			getFromClassTables(JDOUserGroup.class.getName()+".users")+" ugu, "+
			getFromClassTables(JDOUser.class.getName())+" u "+
			"where "+
			// it's the admin group
			"ug.is_system_group=true and ug.is_individual=false and ug.name='"+AuthorizationConstants.ADMIN_GROUP_NAME+"' and "+
			// and the user is in it
			"ug.id = ugu.id_oid and ugu.long_ele=u.id and u.user_id = :userName"
			;

	}
	
	
	/**
	 * @return the SQL to find the root-accessible nodes that a specified user can access
	 * using a specified access type
	 */
	public String authorizationSQL() {
		if (authorizationSQL==null) initAuthorizationSQL();
		return authorizationSQL;
	}
	
	/**
	 * @return the SQL to determine whether a user is an administrator
	 * Returns 1 if an admin, 0 otherwise
	 */
	public String adminSQL() {
		if (adminSQL==null) initAdminSQL();
		return adminSQL;
	}

	/**
	 * Execute the actual query
	 */
	@Override
	public NodeQueryResults executeQuery(BasicQuery query) throws DatastoreException {
		try {
			return executeQueryImpl(query);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Depend on a dao to get these types.
	 * @param name
	 * @return
	 */
	private FieldType getFieldType(String name) {
		// Look up this name
		return fieldTypeDao.getTypeForName(name);
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
	private NodeQueryResults executeQueryImpl(BasicQuery in)
			throws DatastoreException, NotFoundException {
		if (in.getFrom() == null)
			throw new IllegalArgumentException(
					"The query 'from' cannot be null");
		// Add the type to the filter
		in.addExpression(new Expression(new CompoundId(null, SqlConstants.TYPE_COLUMN_NAME), Compartor.EQUALS, in.getFrom().name()));

		// A count query is composed of the following parts
		// <select> + <primaryFrom> + (<inner_join_attribute_filter>)* +
		// (<outer_join_attribute_sort>)+ <primary_where>
		// The real query is composed of the following parts
		// <select> + <primaryFrom> + (<inner_join_attribute_filter>)* +
		// (<outer_join_attribute_sort>)+ <primary_where> + <orderby> + <paging>

		// Build the from
		String fromString = buildFrom(in.getFrom());
		// Build select
		String selectCount = buildSelect(true);
		String selectId = buildSelect(false);
		// Bind variables go in the map.
		Map<String, Object> parameters = new HashMap<String, Object>();
		// Build the outer join attribute sort (empty string if not needed)
		StringBuilder outerJoinAttributeSort = new StringBuilder();
		StringBuilder orderByClause = new StringBuilder();
		// Build the attribute filters
		StringBuilder innerJoinAttributeFilters = new StringBuilder();
		StringBuilder primaryWhere = new StringBuilder();

		try {
			// These two get built at the same time
			if (in.getSort() != null) {
				buildAllSorting(outerJoinAttributeSort, orderByClause,
						in.getSort(), in.isAscending(), parameters);
			}

			// Handle all filters
			if (in.getFilters() != null) {
				buildAllFilters(innerJoinAttributeFilters, primaryWhere,
						in.getFilters(), parameters);
			}
		} catch (AttributeDoesNotExist e) {
			// log this and return an empty result
			log.log(Level.INFO, e.getMessage(), e);
			// Return an empty result
			return new NodeQueryResults(new ArrayList<String>(), 0);
		}
		// Build the paging
		String paging = buildPaging(in.getOffset(), in.getLimit(), parameters);

		// Build the SQL strings
		// Count
		StringBuilder builder = new StringBuilder();
		builder.append(selectCount);
		builder.append(" ");
		builder.append(fromString);
		builder.append(" ");
		builder.append(innerJoinAttributeFilters);
		builder.append(" ");
		builder.append(primaryWhere);
		String countQueryString = builder.toString();
		if (log.isLoggable(Level.FINEST)) {
			log.finest("SQL Count query: " + countQueryString);
		}
		// Now build the full query
		builder = new StringBuilder();
		builder.append(selectId);
		builder.append(" ");
		builder.append(fromString);
		builder.append(" ");
		builder.append(innerJoinAttributeFilters);
		builder.append(" ");
		builder.append(outerJoinAttributeSort);
		builder.append(" ");
		builder.append(primaryWhere);
		builder.append(" ");
		builder.append(orderByClause);
		builder.append(" ");
		builder.append(paging);

		// Query
		String queryString = builder.toString();
		if (log.isLoggable(Level.FINEST)) {
			log.finest("SQL query: " + queryString);
			System.out.println(queryString);
		}

		// Run the count query
		List countResults = executeQuery(countQueryString, parameters);
		Object countObject = countResults.get(0);
		long count = extractCount(countObject);
		// Now execute the non-count query
		List resultSet = (List) executeQuery(queryString, parameters);
		// Process each object
		List<String> allRows = translateResults(resultSet);
		// Create the results
		return new NodeQueryResults(allRows, count);
	}
	
	/**
	 * Execute a given SQL query using the JDO template.
	 * @param <T>
	 * @param clazz
	 * @param sql
	 * @param parameters
	 * @return
	 */
	public List executeQuery(final String sql, final Map<String, Object> parameters){
		return this.jdoTemplate.execute(new JdoCallback<List>() {
			@SuppressWarnings("unchecked")
			@Override
			public List doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				return (List) query.executeWithMap(parameters);
			}
		});
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
	 * Build all filters. For attributes this involves adding an inner join
	 * sub-query, for primary fields it involves a simple where clause.
	 * 
	 * @param innerJoinAttributeFilters
	 * @param primaryWhere
	 * @param filters
	 * @param parameters
	 * @throws DatastoreException
	 */
	private void buildAllFilters(StringBuilder innerJoinAttributeFilters,
			StringBuilder primaryWhere, List<Expression> filters,
			Map<String, Object> parameters) throws DatastoreException,
			AttributeDoesNotExist {
		// We only write the where clause the first time
		int primaryFieldCount = 0;
		int attributeFilterCount = 0;
		// Process each expression
		for (Expression exp : filters) {
			// First look up the column name
			CompoundId id = exp.getId();
			if (id == null)
				throw new IllegalArgumentException("Compound id cannot be null");
			FieldType type = getFieldType(id.getFieldName());
			// Throw an exception if the field does not exist
			if (FieldType.DOES_NOT_EXIST == type)
				throw new AttributeDoesNotExist("No attribute found for: "
						+ id.getFieldName());
			if (FieldType.PRIMARY_FIELD == type) {
				// This is a simple primary field filter
				buildPrimaryWhere(primaryWhere, parameters, primaryFieldCount,
						exp);
				// increment the count
				primaryFieldCount++;
			} else {
				// This is not a primary field so we add an inner join sub-query
				buildInnerJoinAnnotationFilter(innerJoinAttributeFilters, exp,
						type, parameters, attributeFilterCount);

				// Increment the count
				attributeFilterCount++;
			}
		}
	}

	/**
	 * Build up an inner join sub-query to filter on an attribute.
	 * 
	 * @param innerJoinAttributeFilters
	 * @param exp
	 * @param type
	 * @param parameters
	 * @param attributeFilterCount
	 */
	private void buildInnerJoinAnnotationFilter(
			StringBuilder innerJoinAttributeFilters, Expression exp,
			FieldType type, Map<String, Object> parameters,
			int attributeFilterCount) {
		String attTableName = QueryUtils.getTableNameForFieldType(type);
		String joinColumnName = SqlConstants
				.getForeignKeyColumnNameForType(type);
		innerJoinAttributeFilters.append("inner join (select * from ");
		innerJoinAttributeFilters.append(attTableName);
		innerJoinAttributeFilters.append(" where ");
		innerJoinAttributeFilters
				.append(SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN);
		innerJoinAttributeFilters.append(" = :");
		String attNameKey = "attName" + attributeFilterCount;
		innerJoinAttributeFilters.append(attNameKey);
		// Bind the key
		parameters.put(attNameKey, exp.getId().getFieldName());
		innerJoinAttributeFilters.append(" and ");
		innerJoinAttributeFilters.append(SqlConstants.ANNOTATION_VALUE_COLUMN);
		innerJoinAttributeFilters.append(" ");
		innerJoinAttributeFilters.append(SqlConstants.getSqlForComparator(exp
				.getCompare()));
		innerJoinAttributeFilters.append(" :");
		String valueKey = "valeKey" + attributeFilterCount;
		innerJoinAttributeFilters.append(valueKey);
		// Bind the value
		parameters.put(valueKey, exp.getValue());
		innerJoinAttributeFilters.append(") ");
		String filterAlias = "filter" + attributeFilterCount;
		innerJoinAttributeFilters.append(filterAlias);
		buildJoinOn(innerJoinAttributeFilters, SqlConstants.PRIMARY_ALIAS,
				SqlConstants.PRIMARY_ANNOTATION_ID, filterAlias, joinColumnName);
	}

	/**
	 * Append a single primary where clause.
	 * 
	 * @param primaryWhere
	 * @param parameters
	 * @param primaryFieldCount
	 * @param exp
	 */
	private void buildPrimaryWhere(StringBuilder primaryWhere,
			Map<String, Object> parameters, int primaryFieldCount,
			Expression exp) {
		// First gets a where, all others get an and
		if (primaryFieldCount == 0) {
			primaryWhere.append("where ");
		} else {
			primaryWhere.append(" and ");
		}
		// Write the expression
		primaryWhere.append(SqlConstants.PRIMARY_ALIAS);
		primaryWhere.append(".");
		primaryWhere.append(SqlConstants.getColumnNameForPrimaryField(exp
				.getId().getFieldName()));
		primaryWhere.append(" ");
		primaryWhere.append(SqlConstants.getSqlForComparator(exp.getCompare()));
		primaryWhere.append(" :");
		// Add a bind variable
		String bindKey = "primaryValue" + primaryFieldCount;
		primaryWhere.append(bindKey);
		// Bind the value to the parameters
		parameters.put(bindKey, exp.getValue());
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
	private void buildAllSorting(StringBuilder outerJoinAttributeSort,
			StringBuilder orderByClause, String sort, boolean ascending,
			Map<String, Object> parameters) throws DatastoreException,
			AttributeDoesNotExist {
		// The first thing we need to do is determine if we are sorting on a
		// primary field or an attribute.
		String ascString = null;
		if (ascending) {
			ascString = "asc";
		} else {
			ascString = "desc";
		}
		String sortOnAlias = null;
		String sortColumnName = null;
		FieldType type = getFieldType(sort);
		if (FieldType.DOES_NOT_EXIST == type)
			throw new AttributeDoesNotExist("No attribute found for: " + sort);
		if (FieldType.PRIMARY_FIELD == type) {
			sortOnAlias = SqlConstants.PRIMARY_ALIAS;
			sortColumnName = SqlConstants.getColumnNameForPrimaryField(sort);
		} else {
			// We are sorting on an attribute which means we need a left outer
			// join.
			String tableName = QueryUtils.getTableNameForFieldType(type);
			String foreignKey = SqlConstants
					.getForeignKeyColumnNameForType(type);
			sortColumnName = SqlConstants.ANNOTATION_VALUE_COLUMN;
			// We are going to be sorting on a sub query
			sortOnAlias = SqlConstants.ANNOTATION_SORT_SUB_ALIAS;

			// We have enough information to add the left outer joint
			outerJoinAttributeSort.append(" left outer join (select * from ");
			outerJoinAttributeSort.append(tableName);
			outerJoinAttributeSort.append(" where attribute = :sortAttName ");
			// Bind the value to the map.
			parameters.put("sortAttName", sort);
			outerJoinAttributeSort.append(") ");
			outerJoinAttributeSort
					.append(SqlConstants.ANNOTATION_SORT_SUB_ALIAS);
			buildJoinOn(outerJoinAttributeSort, SqlConstants.PRIMARY_ALIAS,
					SqlConstants.PRIMARY_ANNOTATION_ID,
					SqlConstants.ANNOTATION_SORT_SUB_ALIAS, foreignKey);
		}
		// Add the order by
		orderByClause.append(" order by ");
		orderByClause.append(sortOnAlias);
		orderByClause.append(".");
		orderByClause.append(sortColumnName);
		orderByClause.append(" ");
		orderByClause.append(ascString);
	}

	/**
	 * Build up "on (oneAlias.oneColumn = twoAias.twoColumn)"
	 * 
	 * @param builder
	 * @param oneAlias
	 * @param oneColumn
	 * @param twoAlias
	 * @param twoColumn
	 */
	private void buildJoinOn(StringBuilder builder, String oneAlias,
			String oneColumn, String twoAlias, String twoColumn) {
		builder.append(" on (");
		builder.append(oneAlias);
		builder.append(".");
		builder.append(oneColumn);
		builder.append(" = ");
		builder.append(twoAlias);
		builder.append(".");
		builder.append(twoColumn);
		builder.append(")");
	}

	private String buildPaging(long offset, long limit,
			Map<String, Object> parameters) {
		// We need to convert from offset and limit to "fromIncl" and "toExcl"
		if (offset < 0) {
			offset = 0;
		}
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT - 1;
		}
		String paging = "limit :limitVal offset :offsetVal";
		parameters.put("limitVal", limit);
		parameters.put("offsetVal", offset);
		return paging;
	}

	/**
	 * Build the select clause
	 * 
	 * @param alias
	 * @return
	 */
	public String buildSelect(boolean isCount) {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		if (isCount) {
			builder.append("count(");
		}
		builder.append(SqlConstants.PRIMARY_ALIAS);
		builder.append(".");
		builder.append(SqlConstants.COLUMN_ID);
		if (isCount) {
			builder.append(")");
		}
		return builder.toString();
	}

	/**
	 * Build up the from
	 * 
	 * @param builder
	 * @param from
	 */
	private String buildFrom(ObjectType from) {
		StringBuilder builder = new StringBuilder();
		builder.append("from ");
		builder.append(QueryUtils.getTableNameForClass(JDONode.class));
		builder.append(" ");
		builder.append(SqlConstants.PRIMARY_ALIAS);
		return builder.toString();
	}

	/**
	 * Translate the results from a result set to the list of maps.
	 * 
	 * @param resultSet
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private List<String> translateResults(List resultSet) throws DatastoreException {
		try {
			List<String> allRows = new ArrayList<String>();
			int countReturned = resultSet.size();
			for (int i = 0; i < countReturned; i++) {
				Object ob = resultSet.get(i);
				Long id = (Long) ob;
				allRows.add(KeyFactory.keyToString(id));
			}
			return allRows;
		} catch (Throwable e) {
			throw new DatastoreException(e);
		}
	}



	public static class AttributeDoesNotExist extends Exception {

		public AttributeDoesNotExist(String message) {
			super(message);
		}
	}

	/**
	 * Runs the actual query.
	 * 
	 * @param pm
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static Map<Class, String> getAllClassTables() {
		// Load all table names from the database
		// Use the BasicIdentifierFactory to create the table names.
		Map<Class, String> map = new HashMap<Class, String>();
		BasicIdentifierFactory nameFactory = new BasicIdentifierFactory();
		nameFactory.setWordSeparator("");
		map.put(JDONode.class, nameFactory.generateIdentifierNameForJavaName(JDONode.class.getSimpleName()));
		map.put(JDOStringAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDOStringAnnotation.class.getSimpleName()));
		map.put(JDOLongAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDOLongAnnotation.class.getSimpleName()));
		map.put(JDODoubleAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDODoubleAnnotation.class.getSimpleName()));
		map.put(JDODateAnnotation.class, nameFactory.generateIdentifierNameForJavaName(JDODateAnnotation.class.getSimpleName()));
		map.put(JDOUser.class, nameFactory.generateIdentifierNameForJavaName(JDOUser.class.getSimpleName()));
		map.put(JDOUserGroup.class, nameFactory.generateIdentifierNameForJavaName(JDOUserGroup.class.getSimpleName()));
		map.put(JDOResourceAccess.class, nameFactory.generateIdentifierNameForJavaName(JDOResourceAccess.class.getSimpleName()));
		return map;
	}

	/**
	 * Creates and executes the query.
	 * 
	 * @param pm
	 * @param clazz
	 * @param name
	 * @return
	 */
	private Long getCountForName(final Class clazz, final String name) {
		// Create the query
	return jdoTemplate.execute(new JdoCallback<Long>(){
			@Override
			public Long doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(clazz);
				query.setResult("count(attribute)");
				query.setFilter("this.attribute == vAtt");
				query.declareParameters(String.class.getName() + " vAtt");
				return (Long) query.execute(name);
			}
		});
	}


}
