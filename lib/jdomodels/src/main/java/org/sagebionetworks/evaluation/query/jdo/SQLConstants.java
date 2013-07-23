package org.sagebionetworks.evaluation.query.jdo;

public class SQLConstants {
	
	// standard range parameters
	public static final String OFFSET_PARAM_NAME = "OFFSET";
	public static final String LIMIT_PARAM_NAME = "LIMIT";
	
	// Evaluation table constants
	public static final String DDL_FILE_EVALUATION				= "schema/evaluation/Evaluation-ddl.sql";
	public static final String TABLE_EVALUATION					= "JDOEVALUATION";
	public static final String COL_EVALUATION_ID				= "ID";
	public static final String COL_EVALUATION_ETAG				= "ETAG";
	public static final String COL_EVALUATION_NAME				= "NAME";
	public static final String COL_EVALUATION_DESCRIPTION 		= "DESCRIPTION";
	public static final String COL_EVALUATION_OWNER_ID			= "OWNER_ID";
	public static final String COL_EVALUATION_CREATED_ON 		= "CREATED_ON";
	public static final String COL_EVALUATION_CONTENT_SOURCE 	= "CONTENT_SOURCE";
	public static final String COL_EVALUATION_STATUS 			= "STATUS";
	public static final String COL_EVALUATION_SUB_INSTRUCT_MSG	= "SUBMISSION_INSTRUCTIONS_MESSAGE";
	public static final String COL_EVALUATION_SUB_RECEIPT_MSG	= "SUBMISSION_RECEIPT_MESSAGE";
	
	// Participant table constants
	public static final String DDL_FILE_PARTICIPANT				= "schema/evaluation/Participant-ddl.sql";
	public static final String TABLE_PARTICIPANT				= "JDOPARTICIPANT";
	public static final String COL_PARTICIPANT_ID 				= "PARTICIPANT_ID";
	public static final String COL_PARTICIPANT_USER_ID 			= "USER_ID";
	public static final String COL_PARTICIPANT_EVAL_ID 			= "EVALUATION_ID";
	public static final String COL_PARTICIPANT_CREATED_ON 		= "CREATED_ON";
	
	// Submission table constants
	public static final String DDL_FILE_SUBMISSION				= "schema/evaluation/Submission-ddl.sql";
	public static final String TABLE_SUBMISSION					= "JDOSUBMISSION";
	public static final String COL_SUBMISSION_ID 				= "ID";
	public static final String COL_SUBMISSION_EVAL_ID 			= "EVALUATION_ID";
	public static final String COL_SUBMISSION_USER_ID 			= "USER_ID";
	public static final String COL_SUBMISSION_SUBMITTER_ALIAS 	= "SUBMITTER_ALIAS";
	public static final String COL_SUBMISSION_ENTITY_ID 		= "ENTITY_ID";
	public static final String COL_SUBMISSION_ENTITY_BUNDLE 	= "ENTITY_BUNDLE";
	public static final String COL_SUBMISSION_ENTITY_VERSION	= "VERSION";
	public static final String COL_SUBMISSION_NAME				= "NAME";
	public static final String COL_SUBMISSION_CREATED_ON 		= "CREATED_ON";
	
	// SubmissionStatus table constants
	public static final String DDL_FILE_SUBSTATUS				= "schema/evaluation/SubmissionStatus-ddl.sql";
	public static final String TABLE_SUBSTATUS					= "JDOSUBMISSION_STATUS";
	public static final String COL_SUBSTATUS_SUBMISSION_ID 		= "ID";
	public static final String COL_SUBSTATUS_ETAG				= "ETAG";
	public static final String COL_SUBSTATUS_MODIFIED_ON 		= "MODIFIED_ON";
	public static final String COL_SUBSTATUS_SCORE		 		= "SCORE";
	public static final String COL_SUBSTATUS_STATUS 			= "STATUS";
	public static final String COL_SUBSTATUS_SERIALIZED_ENTITY	= "SERIALIZED_ENTITY";
	
	// SubmissionFile table constants
	public static final String DDL_FILE_SUBFILE					= "schema/evaluation/SubmissionFile-ddl.sql";
	public static final String TABLE_SUBFILE					= "JDOSUBMISSION_FILE";
	public static final String COL_SUBFILE_SUBMISSION_ID 		= "SUBMISSION_ID";
	public static final String COL_SUBFILE_FILE_HANDLE_ID 		= "FILE_HANDLE_ID";
}
