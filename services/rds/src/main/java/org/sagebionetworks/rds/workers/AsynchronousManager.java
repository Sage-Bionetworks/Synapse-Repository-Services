package org.sagebionetworks.rds.workers;

/**
 * Abstraction for the asynchronous manager.
 * 
 * @author jmhill
 *
 */
public interface AsynchronousManager {
	
	/**
	 * Called when an entity is created.
	 * @param id
	 */
	public void createEntity(String id);
	
	/**
	 * Called when an entity is updated.
	 * @param id
	 */
	public void updateEntity(String id);
	
	/**
	 * Called when an entity is deleted.
	 * @param id
	 */
	public void deleteEntity(String id);
}
