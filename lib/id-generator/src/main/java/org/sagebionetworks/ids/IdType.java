package org.sagebionetworks.ids;

public enum IdType {
	

	
	FILE_IDS							(null),
	WIKI_ID								(null),
	CHANGE_ID							(null),
	FAVORITE_ID							(null),
	ACL_RES_ACC_ID						(null),
	COLUMN_MODEL_ID						(null), 
	MESSAGE_ID							(null),
	PRINCIPAL_ID						(null),
	PRINCIPAL_ALIAS_ID					(null),
	NOTIFICATION_EMAIL_ID				(null),
	ACL_ID								(null),
	ASYNCH_JOB_STATUS_ID				(null),
	CHALLENGE_ID						(null),
	CHALLENGE_TEAM_ID					(null),
	SUBMISSION_CONTRIBUTOR_ID			(null),
	STORAGE_LOCATION_ID					(null),
	VERIFICATION_SUBMISSION_ID			(null),
	FORUM_ID							(null),
	DISCUSSION_THREAD_ID				(null),
	DISCUSSION_REPLY_ID					(null),
	MULTIPART_UPLOAD_ID					(null),
	SUBSCRIPTION_ID						(null),
	AUTHENTICATION_RECEIPT_ID			(null),
	DOCKER_COMMIT_ID					(null),
	RESEARCH_PROJECT_ID					(null),
	DATA_ACCESS_REQUEST_ID				(null),
	DATA_ACCESS_SUBMISSION_ID			(null),
	DATA_ACCESS_SUBMISSION_ACCESSOR_ID	(null),
	ACCESS_APPROVAL						(9602619L),
	ACCESS_REQUIRMENT					(9602619L),
	ACTIVITY							(9602619L),
	EVALUATION							(9602619L),
	EVALUATION_SUBMISSION				(9602619L),
	DOI									(9602619L),
	ENTITY								(9602619L),
	MEMBERSHIP_INVITATION				(9602619L),
	MEMBERSHIP_REQUEST_SUBMISION		(9602619L),
	PROJECT_SETTINGS					(9602619L),
	PROJECT_STATS						(9602619L),
	QUIZ_RESPONSE						(9602619L),
	DOCKER_REPOSITORY					(9602619L),
	;
	
	Long startingId;
	
	/**
	 * 
	 * @param startingId The ID sequence will start from this number.  Ignored if null.
	 */
	IdType(Long startingId){
		this.startingId = startingId;
	}
}