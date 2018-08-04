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
	
	/**
	 * Create a SQL statement that can be used to restore the entire ID generator database.
	 * 
	 * @return
	 */
	public String createRestoreScript();

	/**
	 * Create a restore script for the given type.
	 * @param builder
	 * @param type
	 */
	void createRestoreScript(StringBuilder builder, IdType type);

	/**
	 * Get the max value for the given type.
	 * 
	 * @param type
	 * @return
	 */
	long getMaxValueForType(IdType type);
	
	
	/**
	 * Each ID issued inserts a row into the database. However, only the last row is
	 * needed to issue new IDs. Therefore, previous rows can be safely deleted from
	 * the database to keep the database size to a minimum.
	 * 
	 * When called this method will delete old rows for the given type up to the
	 * provided row limit.
	 * 
	 * @param rowLimit Limit the number of rows deleted per call.
	 * @param type
	 * @param rowLimit
	 */
	public void cleanupType(IdType type, long rowLimit);
	
	/**
	 * Get the row count for this type.
	 * @param type
	 * @return
	 */
	long getRowCount(IdType type);
	
}
