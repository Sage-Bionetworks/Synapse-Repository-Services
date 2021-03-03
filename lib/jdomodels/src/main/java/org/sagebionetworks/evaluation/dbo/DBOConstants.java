package org.sagebionetworks.evaluation.dbo;

public class DBOConstants {

	public static final String PARAM_LIMIT = "limit";
	public static final String PARAM_OFFSET = "offset";


	// Evaluation
	public static final String PARAM_EVALUATION_ID 					= "id";
	public static final String PARAM_EVALUATION_ETAG 				= "eTag";
	public static final String PARAM_EVALUATION_NAME 				= "name";
	public static final String PARAM_EVALUATION_DESCRIPTION 		= "description";
	public static final String PARAM_EVALUATION_OWNER_ID 			= "ownerId";
	public static final String PARAM_EVALUATION_CREATED_ON 			= "createdOn";
	public static final String PARAM_EVALUATION_CONTENT_SOURCE 		= "contentSource";
	public static final String PARAM_EVALUATION_SUB_INSTRUCT_MSG 	= "submissionInstructionsMessage";
	public static final String PARAM_EVALUATION_SUB_RECEIPT_MSG		= "submissionReceiptMessage";
	public static final String PARAM_EVALUATION_SUBMISSIONS_ETAG 	= "submissionsEtag";
	public static final String PARAM_EVALUATION_QUOTA				= "quota";
	public static final String PARAM_EVALUATION_START_TIMESTAMP		= "startTimestamp";
	public static final String PARAM_EVALUATION_END_TIMESTAMP		= "endTimestamp";

	//Evaluation Round
	public static final String PARAM_EVALUATION_ROUND_ID			= "id";
	public static final String PARAM_EVALUATION_ROUND_ETAG			= "etag";
	public static final String PARAM_EVALUATION_ROUND_EVALUATION_ID	= "evaluationId";
	public static final String PARAM_EVALUATION_ROUND_ROUND_START	= "roundStart";
	public static final String PARAM_EVALUATION_ROUND_ROUND_END		= "roundEnd";
	public static final String PARAM_EVALUATION_ROUND_LIMITS_JSON		= "limitsJson";

	// Submission
	public static final String PARAM_SUBMISSION_ID 					= "id";
	public static final String PARAM_SUBMISSION_USER_ID 			= "userId";
	public static final String PARAM_SUBMISSION_SUBMITTER_ALIAS 	= "submitterAlias";
	public static final String PARAM_SUBMISSION_EVAL_ID 			= "evalId";
	public static final String PARAM_SUBMISSION_EVAL_ROUND_ID		= "evalRoundId";
	public static final String PARAM_SUBMISSION_ENTITY_ID 			= "entityId";
	public static final String PARAM_SUBMISSION_ENTITY_BUNDLE 		= "entityBundle";
	public static final String PARAM_SUBMISSION_ENTITY_VERSION 		= "versionNumber";
	public static final String PARAM_SUBMISSION_NAME 				= "name";
	public static final String PARAM_SUBMISSION_CREATED_ON 			= "createdOn";
	public static final String PARAM_SUBMISSION_TEAM_ID 			= "teamId";
	public static final String PARAM_SUBMISSION_DOCKER_REPOSITORY_NAME = "dockerRepositoryName";
	public static final String PARAM_SUBMISSION_DOCKER_DIGEST 		= "dockerDigest";
	public static final String PARAM_SUBMISSION_SUBMITTER_ID		= "submitterId";
		
	// SubmissionStatus
	public static final String PARAM_SUBSTATUS_ID 					= "id";
	public static final String PARAM_SUBSTATUS_ETAG 				= "eTag";
	public static final String PARAM_SUBSTATUS_VERSION 				= "version";
	public static final String PARAM_SUBSTATUS_STATUS 				= "status";
	public static final String PARAM_SUBSTATUS_ANNOTATIONS			= "annotations";
	public static final String PARAM_SUBSTATUS_SCORE 				= "score";
	public static final String PARAM_SUBSTATUS_MODIFIED_ON 			= "modifiedOn";
	public static final String PARAM_SUBSTATUS_SERIALIZED_ENTITY 	= "serializedEntity";
	public static final long SUBSTATUS_INITIAL_VERSION_NUMBER 		= 0L;
	
	// SubmissionFile
	public static final String PARAM_SUBFILE_SUBMISSION_ID 			= "submissionId";
	public static final String PARAM_SUBFILE_FILE_HANDLE_ID			= "fileHandleId";
	
	// Annotation
	public static final String PARAM_ANNOTATION_OBJECT_ID			= "objectId";
	public static final String PARAM_ANNOTATION_SCOPE_ID			= "scopeId";
}
