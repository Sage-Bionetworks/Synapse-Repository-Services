package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;

/**
 * DAO for interacting with the stack status.
 * 
 * @author jmhill
 *
 */
public interface StackStatusDao {
	
	/**
	 * Get the current stack status.
	 * @return
	 */
	public StackStatus getFullCurrentStatus();
	
	/**
	 * Get the current status.
	 * @return
	 */
	public StatusEnum getCurrentStatus();
	
	/**
	 * Update the current status.
	 * @param status
	 */
	public void updateStatus(StackStatus status);
	
	/**
	 * Is the stack currently in READ_WRITE mode?
	 * 
	 * @return
	 */
	public boolean isStackReadWrite();

}
