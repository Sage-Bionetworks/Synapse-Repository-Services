package org.sagebionetworks.competition.query.jdo;

public class SQLConstants {
	
	// standard range parameters
	public static final String OFFSET_PARAM_NAME = "OFFSET";
	public static final String LIMIT_PARAM_NAME = "LIMIT";
	
	// Competition table constants
	public static final String DDL_FILE_COMPETITION				= "schema/competition/Competition-ddl.sql";
	public static final String TABLE_COMPETITION				= "JDOCOMPETITION";
	public static final String COL_COMPETITION_ID				= "ID";
	public static final String COL_COMPETITION_ETAG				= "ETAG";
	public static final String COL_COMPETITION_NAME				= "NAME";
	public static final String COL_COMPETITION_DESCRIPTION 		= "DESCRIPTION";
	public static final String COL_COMPETITION_OWNER_ID			= "OWNER_ID";
	public static final String COL_COMPETITION_CREATED_ON 		= "CREATED_ON";
	public static final String COL_COMPETITION_CONTENT_SOURCE 	= "CONTENT_SOURCE";
	public static final String COL_COMPETITION_STATUS 			= "STATUS";
	
}
