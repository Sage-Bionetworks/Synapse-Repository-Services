package org.sagebionetworks.repo.model.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

public class AuthorizationSqlUtil {
	
	public static final String AUTHORIZATION_SQL_TABLES =
			TABLE_ACCESS_CONTROL_LIST+" acl, "+
			TABLE_RESOURCE_ACCESS+" ra, "+
			TABLE_RESOURCE_ACCESS_TYPE+" at ";
	
	public static final String AUTHORIZATION_SQL_JOIN =
		    " acl."+COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER+
		    " and at."+COL_RESOURCE_ACCESS_TYPE_ID+"=ra."+COL_RESOURCE_ACCESS_ID;

	/**
	 * The bind variable used to set the access type for authorization filters.
	 */
	public static final String ACCESS_TYPE_BIND_VAR = "type";
	public static final String RESOURCE_ID_BIND_VAR = "resourceId";
	public static final String PRINCIPAL_IDS_BIND_VAR = "principalIds";
	public static final String RESOURCE_TYPE_BIND_VAR = COL_ACL_OWNER_TYPE;
	
	/**
	 * The bind variable prefix used for group ID for the authorization SQL.
	 */
	public static final String BIND_VAR_PREFIX = "g";
	
}
