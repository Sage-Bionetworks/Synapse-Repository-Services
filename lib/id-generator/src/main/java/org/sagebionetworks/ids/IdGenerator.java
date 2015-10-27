package org.sagebionetworks.ids;

/**
 * 
 * @author jmhill
 *
 */
public interface IdGenerator {
	
	public enum TYPE {
		DOMAIN_IDS,
		FILE_IDS,
		WIKI_ID,
		CHANGE_ID,
		PARTICIPANT_ID,
		FAVORITE_ID,
		ACL_RES_ACC_ID,
		COLUMN_MODEL_ID, 
		MESSAGE_ID,
		PRINCIPAL_ID,
		PRINCIPAL_ALIAS_ID,
		NOTIFICATION_EMAIL_ID,
		ACL_ID,
		ASYNCH_JOB_STATUS_ID,
		CHALLENGE_ID,
		CHALLENGE_TEAM_ID,
		SUBMISSION_CONTRIBUTOR_ID,
		STORAGE_LOCATION_ID,
		VERIFICATION_SUBMISSION_ID
	}


	/**
	 * Generate a new Id.
	 * @return
	 */
	public Long generateNewId();
	
	/**
	 * Generate a new Id.
	 * @return
	 */
	public Long generateNewId(TYPE type);
	
	/**
	 * Ensure that the given ID is reserved.  If the ID is not already reserved then, 
	 * this method will reserve it and all values below it.
	 * @param idToLock
	 */
	public void reserveId(Long idToLock, TYPE type);
	
}
