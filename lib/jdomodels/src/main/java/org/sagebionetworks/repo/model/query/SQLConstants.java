package org.sagebionetworks.repo.model.query;

public class SQLConstants {
	
	// standard range parameters
	public static final String OFFSET_PARAM_NAME 				= "OFFSET";
	public static final String LIMIT_PARAM_NAME 				= "LIMIT";
	
	// Annotation table constants
	public static final String PREFIX_SUBSTATUS					= "SUBSTATUS_";
	public static final String ANNO_OWNER						= "ANNOTATIONS_OWNER";
	public static final String ANNO_STRING						= "STRINGANNOTATION";
	public static final String ANNO_LONG						= "LONGANNOTATION";
	public static final String ANNO_DOUBLE						= "DOUBLEANNOTATION";
	public static final String ANNO_BLOB						= "ANNOTATIONS_BLOB";
	public static final String COL_ANNO_BLOB					= "ANNOTATIONS_BLOB";
	public static final String COL_ANNO_ATTRIBUTE				= "ATTRIBUTE";
	public static final String COL_ANNO_VALUE					= "VALUE";
	public static final String COL_ANNO_IS_PRIVATE				= "IS_PRIVATE";
	
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
	
	// SubmissionStatus Annotations
	public static final String DDL_FILE_SUBSTATUS_ANNO_OWNER	= "schema/evaluation/SubmissionStatusAnnotationsOwner-ddl.sql";
	public static final String DDL_FILE_SUBSTATUS_STRINGANNO	= "schema/evaluation/SubmissionStatusStringAnnotation-ddl.sql";
	public static final String DDL_FILE_SUBSTATUS_DOUBLEANNO	= "schema/evaluation/SubmissionStatusDoubleAnnotation-ddl.sql";
	public static final String DDL_FILE_SUBSTATUS_LONGANNO		= "schema/evaluation/SubmissionStatusLongAnnotation-ddl.sql";
	public static final String DDL_FILE_SUBSTATUS_ANNO_BLOB		= "schema/evaluation/SubmissionStatusAnnotationsBlob-ddl.sql";
	public static final String TABLE_SUBSTATUS_ANNO_OWNER		= PREFIX_SUBSTATUS + ANNO_OWNER;
	public static final String TABLE_SUBSTATUS_STRINGANNO		= PREFIX_SUBSTATUS + ANNO_STRING;
	public static final String TABLE_SUBSTATUS_DOUBLEANNO		= PREFIX_SUBSTATUS + ANNO_DOUBLE;
	public static final String TABLE_SUBSTATUS_LONGANNO			= PREFIX_SUBSTATUS + ANNO_LONG;
	public static final String TABLE_SUBSTATUS_ANNO_BLOB		= PREFIX_SUBSTATUS + ANNO_BLOB;
	public static final String COL_SUBSTATUS_ANNO_ID			= "ID";
	public static final String COL_SUBSTATUS_ANNO_SUBID			= "SUBMISSION_ID";
	public static final String COL_SUBSTATUS_ANNO_EVALID		= "EVALUATION_ID";
	public static final String COL_SUBSTATUS_ANNO_ATTRIBUTE		= COL_ANNO_ATTRIBUTE;
	public static final String COL_SUBSTATUS_ANNO_VALUE			= COL_ANNO_VALUE;
	public static final String COL_SUBSTATUS_ANNO_IS_PRIVATE	= COL_ANNO_IS_PRIVATE;
	public static final String COL_SUBSTATUS_ANNO_BLOB			= ANNO_BLOB;
	
	// Aliases for use with Annotations tables
	public static final String ALIAS_ANNO_OWNER					= "annoOwner";
	public static final String ALIAS_STRING_ANNO				= "stringAnno";
	public static final String ALIAS_LONG_ANNO					= "longAnno";
	public static final String ALIAS_DOUBLE_ANNO				= "doubleAnno";
	public static final String ALIAS_ANNO_BLOB					= "annoBlob";
	public static final String ALIAS_EXPRESSION					= "exp";
	public static final String ALIAS_SORT						= "srt";

}
