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
	public Long generateNewId(IdType type);
	
	/**
	 * Ensure that the given ID is reserved.  If the ID is not already reserved then, 
	 * this method will reserve it and all values below it.
	 * @param idToLock
	 */
	public void reserveId(Long idToLock, IdType type);
	
	/**
	 * Generate a batch of new IDs with a single call.
	 * All IDs are guaranteed to contiguous. to be in 
	 * 
	 * @param type The type of IDs to generate.
	 * @param count The total number of IDs to generate.
	 * @return
	 */
	public BatchOfIds generateBatchNewIds(IdType type, int count);
	
}
