package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.model.query.jdo.JDONodeQueryDaoImpl.AttributeDoesNotExist;

@SuppressWarnings("rawtypes")
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
		Collection<UserGroup> groups = userInfo.getGroups();
		if(groups == null) throw new IllegalArgumentException("User's groups cannot be null");
		if(groups.size() < 1) throw new IllegalArgumentException("User must belong to at least one group");
		String sql = AuthorizationSqlUtil.authorizationSQL(groups.size());
		// Bind the variables
		parameters.put(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, ACCESS_TYPE.READ.name());
		// Bind each group
		Iterator<UserGroup> it = groups.iterator();
		int index = 0;
		while(it.hasNext()){
			UserGroup ug = it.next();
			if(ug == null) throw new IllegalArgumentException("UserGroup was null");
			if(ug.getId() == null) throw new IllegalArgumentException("UserGroup.id cannot be null");
			parameters.put(AuthorizationSqlUtil.BIND_VAR_PREFIX+index, KeyFactory.stringToKey(ug.getId()));
			index++;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("inner join (");
		builder.append(sql);
		builder.append(") ");
		builder.append(SqlConstants.AUTH_FILTER_ALIAS);
		buildJoinOn(builder, SqlConstants.NODE_ALIAS,
				SqlConstants.COL_NODE_BENEFACTOR_ID, SqlConstants.AUTH_FILTER_ALIAS, SqlConstants.COL_ACL_OWNER_ID_COLUMN);
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


}
