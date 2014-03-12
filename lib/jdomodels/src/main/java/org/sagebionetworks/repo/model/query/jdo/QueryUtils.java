package org.sagebionetworks.repo.model.query.jdo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.FieldType;

public class QueryUtils {
	
	public static final int MAX_LIMIT = 50000000; // MySQL's upper bound on
	// LIMIT

	private static final HashSet<String> primaryFields;
	
	// Import the primary fields from the dao
	static {
		primaryFields = new HashSet<String>();
		// First add all of the fields by name from the JDONOde object
		Field[] fields = DBONode.class.getDeclaredFields();
		for(Field field: fields){
			primaryFields.add(field.getName());
		}
		
		// This is a bit of a hack to filter on layers.
		primaryFields.add(SqlConstants.INPUT_DATA_LAYER_DATASET_ID);
	}

	/**
	 * What is the table name for a given field type.
	 * 
	 * @param type
	 * @return
	 * @throws AttributeDoesNotExist
	 */
	public static String getTableNameForFieldType(FieldType type) {
		// Get the class and use it to lookup the table
		if(FieldType.BLOB_ATTRIBUTE == type){
			throw new UnsupportedOperationException("Blob annotaions are no longer supported");
		}else if(FieldType.DATE_ATTRIBUTE == type){
			return SqlConstants.TABLE_DATE_ANNOTATIONS;
		}else if(FieldType.DOUBLE_ATTRIBUTE == type){
			return SqlConstants.TABLE_DOUBLE_ANNOTATIONS;
		}else if(FieldType.STRING_ATTRIBUTE == type){
			return SqlConstants.TABLE_STRING_ANNOTATIONS;
		}else if(FieldType.LONG_ATTRIBUTE == type){
			return SqlConstants.TABLE_LONG_ANNOTATIONS;
		}else{
			throw new IllegalArgumentException("Unknown type: "+type);
		}
	}

	/**
	 * Build up the authorization filter
	 * @param userInfo
	 * @param parameters a mutable parameter list
	 * @return
	 * @throws DatastoreException 
	 */
	public static String buildAuthorizationFilter(UserInfo userInfo, Map<String, Object> parameters) throws DatastoreException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(parameters == null) throw new IllegalArgumentException("Parameters cannot be null");
		// First off, if the user is an administrator then there is no filter
		if(userInfo.isAdmin()){
			return "";
		}
		// For all other cases we build up a filter
		Set<Long> groups = userInfo.getGroups();
		if(groups == null) throw new IllegalArgumentException("User's groups cannot be null");
		if(groups.size() < 1) throw new IllegalArgumentException("User must belong to at least one group");
		String sql = AuthorizationSqlUtil.authorizationSQL(groups.size());
		// Bind the variables
		parameters.put(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, ACCESS_TYPE.READ.name());
		// Bind each group
		Iterator<Long> it = groups.iterator();
		int index = 0;
		while(it.hasNext()){
			Long ug = it.next();
			if(ug == null) throw new IllegalArgumentException("UserGroup was null");
			parameters.put(AuthorizationSqlUtil.BIND_VAR_PREFIX+index, ug);
			index++;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("inner join (");
		builder.append(sql);
		builder.append(") ");
		builder.append(SqlConstants.AUTH_FILTER_ALIAS);
		buildJoinOn(builder, SqlConstants.NODE_ALIAS,
				SqlConstants.COL_NODE_BENEFACTOR_ID, SqlConstants.AUTH_FILTER_ALIAS, SqlConstants.COL_ACL_ID);
		return builder.toString();
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
	private static void buildJoinOn(StringBuilder builder, String oneAlias,
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

	public static String buildPaging(long offset, long limit,
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
	 * Assemble query results into a ResourceQueryResults object.
	 * 
	 * @param fromDB
	 * @param select
	 * @return
	 * @throws DatastoreException 
	 */
	public static NodeQueryResults translateResults(List<Map<String, Object>> fromDB, long totalCount, List<String> select) throws DatastoreException{
		// First build up the list of ID
		List<String> idList = new ArrayList<String>();
		for(Map<String, Object> row: fromDB){
			// Remove the annotations from the map if there
			byte[] zippedAnnos = (byte[]) row.remove(SqlConstants.COL_REVISION_ANNOS_BLOB);
			// If select is null then add all
			if(zippedAnnos != null){
				try {
					NamedAnnotations named = JDOSecondaryPropertyUtils.decompressedAnnotations(zippedAnnos);
					// Add the primary
					addNewToMap(row, named.getPrimaryAnnotations(), select);
					// Now add the secondary.
					addNewToMap(row, named.getAdditionalAnnotations(), select);
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			}
			// Replace the ID with a string if needed
			Long idLong = (Long) row.remove(NodeField.ID.getFieldName());
			if(idLong != null){
				String id = KeyFactory.keyToString(idLong);
				row.put(NodeField.ID.getFieldName(), id);
				idList.add(id);
			}
			// Replace the parentID with a string if needed
			Long parentIdLong = (Long) row.get(NodeField.PARENT_ID.getFieldName());
			if(parentIdLong != null){
				String parentId = KeyFactory.keyToString(parentIdLong);
				row.put(NodeField.PARENT_ID.getFieldName(), parentId);
			}
			//			System.out.println(row);
		}
		// Return the results.
		return new NodeQueryResults(idList, fromDB, totalCount);
	}
	
	
	/**
	 * Try to determine the type from the value
	 * @param value
	 * @return
	 */
	public static FieldType determineTypeFromValue(Object value){
		if(value == null) return null;
		if(value instanceof Long){
			return FieldType.LONG_ATTRIBUTE;
		}
		if(value instanceof Double){
			return FieldType.DOUBLE_ATTRIBUTE;
		}
		if(value instanceof String){
			String string = (String) value;
			// Try to parse long
			try{
				Long.parseLong(string);
				return FieldType.LONG_ATTRIBUTE;
			}catch (NumberFormatException e) {
				// Not a long
				try{
					Double.parseDouble(string);
					return FieldType.DOUBLE_ATTRIBUTE;
				}catch(NumberFormatException e2){
					// Not a long or a double so it is a string
					return FieldType.STRING_ATTRIBUTE;
				}
			}
	
		}
		// All others are treated as strings
		return FieldType.STRING_ATTRIBUTE;
	}
	
	private static void addNewToMap(Map<String, Object> row, Annotations annotations, List<String> select) {
		if(annotations != null){
			addNewOnly(row, annotations.getStringAnnotations(), select);
			addNewOnly(row, annotations.getDateAnnotations(), select);
			addNewOnly(row, annotations.getLongAnnotations(), select);
			addNewOnly(row, annotations.getDoubleAnnotations(), select);
		}
	}
	
	/**
	 * Only add values that are not already in the map
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param toAdd
	 */
	protected static <K> void addNewOnly(Map<String, Object> map, Map<String, ? extends K> toAdd, List<String> select){
		if(toAdd != null){
			// If the select list is null then add all keys
			Iterator<String> keyIt = null;
			if(select == null){
				// Add all keys
				keyIt = toAdd.keySet().iterator();
			}else{
				// Only add keys from the list
				keyIt = select.iterator();
			}
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Object value = map.get(key);
				if(value == null){
					map.put(key, toAdd.get(key));
				}
			}
		}
	}

}
