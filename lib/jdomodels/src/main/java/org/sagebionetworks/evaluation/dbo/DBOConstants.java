package org.sagebionetworks.evaluation.dbo;

public class DBOConstants {
	
	// Evaluation
	public static final String PARAM_EVALUATION_ID 					= "id";
	public static final String PARAM_EVALUATION_ETAG 				= "eTag";
	public static final String PARAM_EVALUATION_NAME 				= "name";
	public static final String PARAM_EVALUATION_DESCRIPTION 		= "description";
	public static final String PARAM_EVALUATION_OWNER_ID 			= "ownerId";
	public static final String PARAM_EVALUATION_CREATED_ON 			= "createdOn";
	public static final String PARAM_EVALUATION_CONTENT_SOURCE 		= "contentSource";
	public static final String PARAM_EVALUATION_STATUS				= "status";
	public static final String PARAM_EVALUATION_SUB_INSTRUCT_MSG 	= "submissionInstructionsMessage";
	public static final String PARAM_EVALUATION_SUB_RECEPIT_MSG		= "submissionReceiptMessage";
	
	// Participant
	public static final String PARAM_PARTICIPANT_ID 				= "id";
	public static final String PARAM_PARTICIPANT_USER_ID 			= "userId";
	public static final String PARAM_PARTICIPANT_EVAL_ID 			= "evalId";
	public static final String PARAM_PARTICIPANT_CREATED_ON 		= "createdOn";
	
	// Submission
	public static final String PARAM_SUBMISSION_ID 					= "id";
	public static final String PARAM_SUBMISSION_USER_ID 			= "userId";
	public static final String PARAM_SUBMISSION_SUBMITTER_ALIAS 	= "submitterAlias";
	public static final String PARAM_SUBMISSION_EVAL_ID 			= "evalId";
	public static final String PARAM_SUBMISSION_ENTITY_ID 			= "entityId";
	public static final String PARAM_SUBMISSION_ENTITY_BUNDLE 		= "entityBundle";
	public static final String PARAM_SUBMISSION_ENTITY_VERSION 		= "versionNumber";
	public static final String PARAM_SUBMISSION_NAME 				= "name";
	public static final String PARAM_SUBMISSION_CREATED_ON 			= "createdOn";
		
	// SubmissionStatus
	public static final String PARAM_SUBSTATUS_ID 					= "id";
	public static final String PARAM_SUBSTATUS_ETAG 				= "eTag";
	public static final String PARAM_SUBSTATUS_STATUS 				= "status";
	public static final String PARAM_SUBSTATUS_SCORE 				= "score";
	public static final String PARAM_SUBSTATUS_MODIFIED_ON 			= "modifiedOn";
	public static final String PARAM_SUBSTATUS_SERIALIZED_ENTITY 	= "serializedEntity";
	
	// SubmissionFile
	public static final String PARAM_SUBFILE_SUBMISSION_ID 			= "submissionId";
	public static final String PARAM_SUBFILE_FILE_HANDLE_ID			= "fileHandleId";
	
	// Annotation
	public static final String PARAM_ANNOTATION_OBJECT_ID			= "objectId";
	public static final String PARAM_ANNOTATION_SCOPE_ID			= "scopeId";
}
