package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;

public interface SemaphoreManager {
	
	/**
	 * Only an administrator can make this call.
	 * 
	 * @param admin
	 */
	void releaseAllLocksAsAdmin(UserInfo admin);

}
