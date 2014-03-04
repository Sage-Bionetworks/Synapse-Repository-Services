package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AuthorizationSqlUtil {
	
	/*
	 * select acl.id from jdoaccesscontrollist acl, jdoresourceaccess ra, access_type at
	 * where
	 * ra.oid_id=acl.id and ra.groupId in :groups and at.oid_id=ra.id and at.type=:type
	 */

	private static final String AUTHORIZATION_SQL_SELECT = 
			"select distinct acl."+SqlConstants.COL_ACL_OWNER_ID+" "+SqlConstants.COL_ACL_ID;
	
	public static final String AUTHORIZATION_SQL_FROM = " from "+
			SqlConstants.TABLE_ACCESS_CONTROL_LIST+" acl, "+
			SqlConstants.TABLE_RESOURCE_ACCESS+" ra, "+
			SqlConstants.TABLE_RESOURCE_ACCESS_TYPE+" at ";
	
	private static final String AUTHORIZATION_SQL_WHERE_1 = 
		"where (ra."+SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID+
		" in (";

	/**
	 * The bind variable used to set the access type for authorization filters.
	 */
	public static final String ACCESS_TYPE_BIND_VAR = "type";
	public static final String RESOURCE_ID_BIND_VAR = "resourceId";
	public static final String RESOURCE_TYPE_BIND_VAR = SqlConstants.COL_ACL_OWNER_TYPE;
	
	private static final String AUTHORIZATION_SQL_WHERE_2 = 
		"))"+
	    " and acl."+SqlConstants.COL_ACL_ID+"=ra."+SqlConstants.COL_RESOURCE_ACCESS_OWNER+
	    " and acl."+SqlConstants.COL_ACL_OWNER_TYPE+"=:"+RESOURCE_TYPE_BIND_VAR+
		" and at."+SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID+"=ra."+SqlConstants.COL_RESOURCE_ACCESS_ID+
		" and at."+SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT+"=:"+ACCESS_TYPE_BIND_VAR;
	
	private static final String CAN_ACCESS_SQL_1 =
			"SELECT COUNT(acl." + SqlConstants.COL_ACL_ID + ") " +
			"FROM " +
					SqlConstants.TABLE_ACCESS_CONTROL_LIST + " acl, " +
					SqlConstants.TABLE_RESOURCE_ACCESS + " ra, " +
					SqlConstants.TABLE_RESOURCE_ACCESS_TYPE + " aat " +
			"WHERE " +
					"ra." + SqlConstants.COL_RESOURCE_ACCESS_OWNER + "=acl." + SqlConstants.COL_ACL_ID +
					" AND aat." + SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID + "=ra." + SqlConstants.COL_RESOURCE_ACCESS_ID +
					" AND (ra."+SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID+" IN (";

	private static final String CAN_ACCESS_SQL_2 = "))" +
					" AND aat." + SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT + "=:" + ACCESS_TYPE_BIND_VAR +
					" AND acl." + SqlConstants.COL_ACL_OWNER_ID + " =:" + RESOURCE_ID_BIND_VAR
					+ " AND acl."+SqlConstants.COL_ACL_OWNER_TYPE+"=:"+RESOURCE_TYPE_BIND_VAR;

	/**
	 * The bind variable prefix used for group ID for the authorization SQL.
	 */
	public static final String BIND_VAR_PREFIX = "g";

	/**
	 * This returns a 'select' statement suitable for using as a subquery
	 * when selecting objects matching other criteria
	 * @param n number of principals (user groups) in the parameter set
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public static String authorizationSQL(int n) {
		StringBuilder sb = new StringBuilder(AUTHORIZATION_SQL_SELECT);
		sb.append(AUTHORIZATION_SQL_FROM);
		sb.append(authorizationSQLWhere(n));
		return sb.toString();
	}
	
	/**
	 * 
	 * @param n number of principals (user groups) in the parameter set
	 * @return the 'where' clause for the authorization SQL
	 * 
	 * Can't bind a collection to a variable in the string, so we have to create n bind variables 
	 * for a collection of length n.  :^(
	 */
	public static String authorizationSQLWhere(int n) {
		StringBuilder sb = new StringBuilder(AUTHORIZATION_SQL_WHERE_1);
		for (int i=0; i<n; i++) {
			if (i>0) sb.append(",");
			sb.append(":");
			sb.append(BIND_VAR_PREFIX);
			sb.append(i);
		}
		sb.append(AUTHORIZATION_SQL_WHERE_2);
		return sb.toString();
	}
	
	/**
	 * Create the canAccess Sql
	 * @param numberUserGroups
	 * @return
	 */
	public static String authorizationCanAccessSQL(int numberUserGroups){
		StringBuilder sb = new StringBuilder(CAN_ACCESS_SQL_1);
		for (int i=0; i<numberUserGroups; i++) {
			if (i>0) sb.append(",");
			sb.append(":");
			sb.append(BIND_VAR_PREFIX);
			sb.append(i);
		}
		sb.append(CAN_ACCESS_SQL_2);
		return sb.toString();
	}
}
