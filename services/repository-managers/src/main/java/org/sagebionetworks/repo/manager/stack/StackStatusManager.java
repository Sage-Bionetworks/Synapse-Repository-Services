package org.sagebionetworks.repo.manager.stack;

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
	StackStatus getCurrentStatus();
	
	/**
	 * Update the stack status.
	 * @param username
	 * @param updated
	 * @throws UnauthorizedException 
	 */
	StackStatus updateStatus(UserInfo username, StackStatus updated) throws UnauthorizedException;

}
