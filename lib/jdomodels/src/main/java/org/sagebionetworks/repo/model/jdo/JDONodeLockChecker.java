package org.sagebionetworks.repo.model.jdo;

public interface JDONodeLockChecker {
	
	public void aquireAndHoldLock(String nodeId) throws InterruptedException;
	
	public void releaseLock();
	
	public boolean isLockAcquired();

}
