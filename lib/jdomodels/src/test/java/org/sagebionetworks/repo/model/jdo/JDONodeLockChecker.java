package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.web.NotFoundException;

public interface JDONodeLockChecker {
	
	public void aquireAndHoldLock(String nodeId) throws InterruptedException, NotFoundException;
	
	public void releaseLock();
	
	public boolean isLockAcquired();

}
