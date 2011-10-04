package org.sagebionetworks.ids;

/**
 * 
 * @author jmhill
 *
 */
public interface IdGenerator {
	

	/**
	 * Generate a new Id.
	 * @return
	 */
	public Long generateNewId();
	
	/**
	 * Ensure that the given ID is reserved.  If the ID is not already reserved then, 
	 * this method will reserve it and all values below it.
	 * @param idToLock
	 */
	public void reserveId(Long idToLock);
}
