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
		ACL_RES_ACC_ID
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
