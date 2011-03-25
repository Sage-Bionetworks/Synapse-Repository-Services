package org.sagebionetworks.repo.model.query.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.JDODatasetDAOImpl;
import org.sagebionetworks.repo.model.jdo.PMF;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.model.query.QueryDAO;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * The JDO implementation of the Query DAO.
 * 
 * @author jmhill
 * 
 */
public class JDOQueryDAOImpl implements QueryDAO {

	private static Logger log = Logger.getLogger(JDOQueryDAOImpl.class
			.getName());

	public static final int MAX_LIMIT = 50000000; // MySQL's upper bound on
													// LIMIT

	// This map contains the table names for each class
	private static final Map<Class, String> classTableMap = JDOQueryDAOImpl
			.getAllClassTables();;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HashSet<String> primaryFields;
	// Import the primary fields from the dao
	static {
		primaryFields = new HashSet<String>();
		JDODAOFactoryImpl factory = new JDODAOFactoryImpl();
		DatasetDAO dao = factory.getDatasetDAO(null);
		Collection<String> fromDao = dao.getPrimaryFields();
		// Add everything from a datasets
		primaryFields.addAll(fromDao);
		try {
			// Add everything from layers
			Collection<String> layerPrimary = dao.getInputDataLayerDAO(
					"99999999").getPrimaryFields();
			primaryFields.addAll(layerPrimary);
		} catch (DatastoreException e) {
			throw new IllegalArgumentException(e);
		}
		// This is a bit of a hack to filter on layers.
		primaryFields.add(SqlConstants.INPUT_DATA_LAYER_DATASET_ID);
	}

	DatasetDAO datasetDao;

	public JDOQueryDAOImpl() {
		JDODAOFactoryImpl factory = new JDODAOFactoryImpl();
		datasetDao = factory.getDatasetDAO(null);
	}

	/**
	 * Get the table name for a given class.
	 * 
	 * @param clazz
	 * @return
	 */
	String getTableNameForClass(Class clazz) {
		String tableName = classTableMap.get(clazz);
		if (tableName == null)
			throw new IllegalArgumentException(
					"A table name does not exist for class: " + clazz.getName());
		return tableName;
	}

	/**
	 * What is the table name for a given object type.
	 * 
	 * @param type
	 * @return
	 */
	String getTableNameForObjectType(ObjectType type) {
		// First look up the class
		Class clazz = SqlConstants.getJdoClassForObjectType(type);
		// Use the class to lookup the table
		return getTableNameForClass(clazz);
	}

	/**
	 * What is the table name for a given field type.
	 * 
	 * @param type
	 * @return
	 * @throws AttributeDoesNotExist
	 */
	String getTableNameForFieldType(FieldType type) {
		// Get the class and use it to lookup the table
		Class clazz = SqlConstants.getJdoClassForFieldType(type);
		// Get the table for this class
		return getTableNameForClass(clazz);
	}

	/**
	 * Execute the actual query
	 */
	@Override
	public QueryResults executeQuery(BasicQuery query)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			return executeQuery(pm, query);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		}catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
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
	private QueryResults executeQuery(PersistenceManager pm, BasicQuery in)
			throws DatastoreException, NotFoundException {
		if (in.getFrom() == null)
			throw new IllegalArgumentException(
					"The query 'from' cannot be null");

		// Pre-process a layer query
		String layerDatasetId = null;
		if (ObjectType.layer == in.getFrom()) {
			layerDatasetId = preprocessLayerQuery(in);
		}

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
				buildAllSorting(outerJoinAttributeSort, orderByClause,	in.getSort(), in.isAscending(), parameters);
			}

			// Handle all filters
			if (in.getFilters() != null) {
				buildAllFilters(innerJoinAttributeFilters, primaryWhere, in.getFilters(), parameters);
			}
		} catch (AttributeDoesNotExist e) {
			// log this and return an empty result
			log.log(Level.INFO, e.getMessage(), e);
			// Return an empty result
			return 	new QueryResults(new ArrayList<Map<String,Object>>() , 0);
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

		// Create the two queries
		Query countQuery = pm.newQuery("javax.jdo.query.SQL", countQueryString);
		Query query = pm.newQuery("javax.jdo.query.SQL", queryString);

		// Run the count query
		List countResults = (List) countQuery.executeWithMap(parameters);
		Object countObject = countResults.get(0);
		long count = extractCount(countObject);
		// Now execute the non-count query
		List resultSet = (List) query.executeWithMap(parameters);
		// Process each object
		List<Map<String, Object>> allRows = translateResults(resultSet,
				in.getFrom(), layerDatasetId);
		// Create the results
		return new QueryResults(allRows, (int) count);
	}

	private String preprocessLayerQuery(BasicQuery in) {
		String layerDatasetId = null;
		// Find the where
		if (in.getFilters() == null)
			throw new IllegalArgumentException(
					"Layer queries must include a 'WHERE dataset.id == <the id>' clause");
		for (Expression expressiom : in.getFilters()) {
			if (expressiom.getId() == null)
				throw new IllegalArgumentException(
						"Compound id cannot be null");
			if ("id".equals(expressiom.getId().getFieldName())) {
				layerDatasetId = (String) expressiom.getValue();
				// Replace this id with the real table id
				expressiom.setId(new CompoundId("layer",SqlConstants.INPUT_DATA_LAYER_DATASET_ID));
			}
		}
		if(layerDatasetId == null) throw new IllegalArgumentException("Layer queries must include a 'WHERE dataset.id == <the id>' clause");
		return layerDatasetId;
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
				throw new AttributeDoesNotExist("No attribute found for: "+ id.getFieldName());
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
		String attTableName = this.getTableNameForFieldType(type);
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
		primaryWhere.append(exp.getId().getFieldName());
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
			sortColumnName = sort;
		} else {
			// We are sorting on an attribute which means we need a left outer
			// join.
			String tableName = getTableNameForFieldType(type);
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
		builder.append(getTableNameForObjectType(from));
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
	private List<Map<String, Object>> translateResults(List resultSet,
			ObjectType type, String layerDatasetId) throws DatastoreException {
		try {
			List<Map<String, Object>> allRows = new ArrayList<Map<String, Object>>();
			JDODatasetDAOImpl dao = new JDODatasetDAOImpl(null);
			int countReturned = resultSet.size();
			for (int i = 0; i < countReturned; i++) {
				Object ob = resultSet.get(i);
				if (type == ObjectType.dataset) {
					Long id = (Long) ob;
					Dataset dto = datasetDao.get(id.toString());
					Map<String, Object> row = OBJECT_MAPPER.convertValue(dto,
							Map.class);
					Annotations annotations = dao.getAnnotations(id.toString());
					row.putAll(annotations.getStringAnnotations());
					row.putAll(annotations.getDoubleAnnotations());
					row.putAll(annotations.getLongAnnotations());
					row.putAll(annotations.getDateAnnotations());
					// Add this row
					allRows.add(row);
				} else if (type == ObjectType.layer) {
					Long id = (Long) ob;
					InputDataLayer dto = datasetDao.getInputDataLayerDAO(
							layerDatasetId).get(id.toString());
					Map<String, Object> row = OBJECT_MAPPER.convertValue(dto,
							Map.class);
					Annotations annotations = datasetDao.getInputDataLayerDAO(
							id.toString()).getAnnotations(id.toString());
					row.putAll(annotations.getStringAnnotations());
					row.putAll(annotations.getDoubleAnnotations());
					row.putAll(annotations.getLongAnnotations());
					row.putAll(annotations.getDateAnnotations());
					// Add this row
					allRows.add(row);
				}
			}
			return allRows;
		} catch (Throwable e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public FieldType getFieldType(String fieldName) throws DatastoreException {
		if (primaryFields.contains(fieldName)) {
			return FieldType.PRIMARY_FIELD;
		} else {
			// Try each annotation class
			// String
			long count = getCountForName(JDOStringAnnotation.class, fieldName);
			if (count > 0) {
				return FieldType.STRING_ATTRIBUTE;
			}
			// Double
			count = getCountForName(JDODoubleAnnotation.class, fieldName);
			if (count > 0) {
				return FieldType.DOUBLE_ATTRIBUTE;
			}
			// Long
			count = getCountForName(JDOLongAnnotation.class, fieldName);
			if (count > 0) {
				return FieldType.LONG_ATTRIBUTE;
			}
			// Date
			count = getCountForName(JDODateAnnotation.class, fieldName);
			if (count > 0) {
				return FieldType.DATE_ATTRIBUTE;
			}
		}
		// Unknown
		return FieldType.DOES_NOT_EXIST;
	}

	public static class AttributeDoesNotExist extends Exception {

		public AttributeDoesNotExist(String message) {
			super(message);
		}
	}

	/**
	 * Count the number of attributes of this type that have the given name.
	 */
	public Long getCountForName(Class clazz, String name)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			return getCountForName(pm, clazz, name);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Fetch all class tables
	 * 
	 * @return
	 */
	private static Map<Class, String> getAllClassTables() {
		PersistenceManager pm = PMF.get();
		try {
			return getAllClassTables(pm);
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Runs the actual query.
	 * 
	 * @param pm
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static Map<Class, String> getAllClassTables(PersistenceManager pm) {
		// Load all table names from the database
		// Query query = pm.newQuery("javax.jdo.query.SQL",
		// "SELECT CLASS_NAME, TABLE_NAME FROM NUCLEUS_TABLES");
		// List resultSet = (List) query.execute();
		Map<Class, String> map = new HashMap<Class, String>();
		// // If the list is empty then do this manually
		// if(!resultSet.isEmpty() || resultSet.size() < 20){
		// for(int i=0; i<resultSet.size(); i++){
		// Object[] array = (Object[]) resultSet.get(i);
		// Class clazz;
		// try {
		// clazz = Class.forName((String) array[0]);
		// map.put(clazz, (String)array[1]);
		// } catch (ClassNotFoundException e) {
		// e.printStackTrace();
		// }
		// }
		// }else{
		// It is empty so we do this manually
		map.put(JDODataset.class, "jdodataset");
		map.put(JDOInputDataLayer.class, "jdoinputdatalayer");
		map.put(JDOStringAnnotation.class, "jdostringannotation");
		map.put(JDOLongAnnotation.class, "jdolongannotation");
		map.put(JDODoubleAnnotation.class, "jdodoubleannotation");
		map.put(JDODateAnnotation.class, "jdodateannotation");
		// }

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
	private Long getCountForName(PersistenceManager pm, Class clazz, String name) {
		// Create the query
		Query query = pm.newQuery(clazz);
		query.setResult("count(attribute)");
		query.setFilter("this.attribute == vAtt");
		query.declareParameters(String.class.getName() + " vAtt");
		return (Long) query.execute(name);
	}

}
