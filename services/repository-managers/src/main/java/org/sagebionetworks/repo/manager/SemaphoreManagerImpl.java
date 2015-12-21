package org.sagebionetworks.repo.manager;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

public class SemaphoreManagerImpl implements SemaphoreManager {
	
	@Autowired
	CountingSemaphore countingSemaphore;
	
	@Autowired
	CountingSemaphore userThrottleMemoryCountingSemaphore;

	@Override
	public void releaseAllLocksAsAdmin(UserInfo admin) {
		if(admin == null){
			throw new IllegalArgumentException("UserInfo cannot be null");
		}
		// Only an admin can make this call
		if(!admin.isAdmin()){
			throw new UnauthorizedException("Only an administrator can make this call");
		}
		// Release all locks
		countingSemaphore.releaseAllLocks();
		// release memory locks.
		userThrottleMemoryCountingSemaphore.releaseAllLocks();
	}
	

}
