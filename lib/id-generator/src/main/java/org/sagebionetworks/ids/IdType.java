package org.sagebionetworks.ids;

/**
 * Enumeration that defines the ID sequence for each type of object.
 */
public enum IdType {
	
	ACCESS_APPROVAL_ID(9602619L),
	ACCESS_REQUIREMENT_ID(9602619L),
	ACTIVITY_ID(9602619L),
	EVALUATION_ID(9602619L),
	EVALUATION_ROUND_ID(null),
	EVALUATION_SUBMISSION_ID(9602619L),
	DOI_ID(9602619L),
	ENTITY_ID(9602619L),
	MEMBERSHIP_INVITATION_ID(9602619L),
	MEMBERSHIP_REQUEST_SUBMISSION_ID(9602619L),
	PROJECT_SETTINGS_ID(9602619L),
	PROJECT_STATS_ID(9602619L),
	QUIZ_RESPONSE_ID(9602619L),
	FILE_IDS(null),
	WIKI_ID(null),
	CHANGE_ID(null),
	FAVORITE_ID(null),
	ACL_RES_ACC_ID(null),
	COLUMN_MODEL_ID(null), 
	MESSAGE_ID(null),
	PRINCIPAL_ID(null),
	PRINCIPAL_ALIAS_ID(null),
	NOTIFICATION_EMAIL_ID(null),
	ACL_ID(null),
	ASYNCH_JOB_STATUS_ID(null),
	CHALLENGE_ID(null),
	CHALLENGE_TEAM_ID(null),
	SUBMISSION_CONTRIBUTOR_ID(null),
	STORAGE_LOCATION_ID(null),
	VERIFICATION_SUBMISSION_ID(null),
	FORUM_ID(null),
	DISCUSSION_THREAD_ID(null),
	DISCUSSION_REPLY_ID(null),
	MULTIPART_UPLOAD_ID(null),
	SUBSCRIPTION_ID(null),
	AUTHENTICATION_RECEIPT_ID(null),
	DOCKER_COMMIT_ID(null),
	RESEARCH_PROJECT_ID(null),
	DATA_ACCESS_REQUEST_ID(null),
	DATA_ACCESS_SUBMISSION_ID(null),
	DATA_ACCESS_SUBMISSION_SUBMITTER_ID(null),
	DOWNLOAD_ORDER_ID(null),
	FORM_GROUP_ID(null),
	FORM_DATA_ID(null),
	DATA_TYPE_ID(null),
	TABLE_TRANSACTION_ID(null),
	OAUTH_SECTOR_IDENTIFIER_ID(null),
	OAUTH_CLIENT_ID(100000L),
	OAUTH_AUTHORIZATION_CONSENT(null),
	OAUTH_REFRESH_TOKEN_ID(null),
	PERSONAL_ACCESS_TOKEN_ID(null),
	SES_NOTIFICATION_ID(null),
	QUARANTINED_EMAIL_ID(null),
	VIEW_SNAPSHOT_ID(null),
	ORGANIZATION_ID(null),
	JSON_SCHEMA_ID(null),
	JSON_SCHEMA_BLOB_ID(null),
	JSON_SCHEMA_VERSION_ID(null),
	JSON_SCHEMA_BIND_OBJECT_ID(null),
	DATA_ACCESS_NOTIFICATION_ID(null),
	FEATURE_STATUS_ID(null),
	FILES_SCANNER_STATUS_ID(null)
	;
	
	Long startingId;
	
	/**
	 * 
	 * @param startingId The ID that the sequence for this type will start from. 
	 * If null the sequence will start from one.
	 */
	IdType(Long startingId){
		this.startingId = startingId;
	}

	/**
	 * The ID that the sequence for this type will start from. If null the
	 * sequence will start from one.
	 * 
	 * @return
	 */
	public Long getStartingId() {
		return startingId;
	}
	
	
}