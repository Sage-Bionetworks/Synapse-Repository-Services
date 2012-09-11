package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface JDONodeLockChecker {
	
	public void aquireAndHoldLock(String nodeId, String currentETag) throws InterruptedException, NotFoundException, NumberFormatException, ConflictingUpdateException, DatastoreException;
	
	/**
	 * True when the lock is released
	 */
	public void releaseLock();
	/**
	 * True when the lock is aquired
	 * @return
	 */
	public boolean isLockAcquired();
	
	public boolean failedDueToConflict();
	
	/**
	 * Get the new etag.
	 * @return
	 */
	public String getEtag();
	
	/**
	 * Throw an exception while waiting for a lock
	 * @param toThrow
	 */
	public void throwException(DatastoreException toThrow);

}
