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
	
	// Participant table constants
	public static final String DDL_FILE_PARTICIPANT				= "schema/competition/Participant-ddl.sql";
	public static final String TABLE_PARTICIPANT				= "JDOPARTICIPANT";
	public static final String COL_PARTICIPANT_ID 				= "PARTICIPANT_ID";
	public static final String COL_PARTICIPANT_USER_ID 			= "USER_ID";
	public static final String COL_PARTICIPANT_COMP_ID 			= "COMPETITION_ID";
	public static final String COL_PARTICIPANT_CREATED_ON 		= "CREATED_ON";
	
	// Submission table constants
	public static final String DDL_FILE_SUBMISSION				= "schema/competition/Submission-ddl.sql";
	public static final String TABLE_SUBMISSION					= "JDOSUBMISSION";
	public static final String COL_SUBMISSION_ID 				= "SUBMISSION_ID";
	public static final String COL_SUBMISSION_COMP_ID 			= "COMPETITION_ID";
	public static final String COL_SUBMISSION_USER_ID 			= "USER_ID";
	public static final String COL_SUBMISSION_ENTITY_ID 		= "ENTITY_ID";
	public static final String COL_SUBMISSION_NAME				= "NAME";
	public static final String COL_SUBMISSION_CREATED_ON 		= "CREATED_ON";
	public static final String COL_SUBMISSION_SCORE		 		= "SCORE";
	public static final String COL_SUBMISSION_STATUS 			= "STATUS";
}
