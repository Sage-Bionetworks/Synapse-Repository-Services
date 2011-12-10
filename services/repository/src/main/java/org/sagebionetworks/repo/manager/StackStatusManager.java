package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.status.StackStatus;

/**
 * Manager for the stack status.
 * @author John
 *
 */
public interface StackStatusManager {
	
	/**
	 * Get the current stack status.
	 * @return
	 */
	public StackStatus getCurrentStatus();
	
	/**
	 * Update the stack status.
	 * @param username
	 * @param updated
	 * @throws UnauthorizedException 
	 */
	public StackStatus updateStatus(UserInfo username, StackStatus updated) throws UnauthorizedException;

}
