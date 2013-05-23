package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;

/**
 * Simple database backed implementation of SemaphoreDao.
 * @author jmhill
 *
 */
public class DBOSemaphoreDaoImpl implements SemaphoreDao {
		

	@Override
	public String attemptToAcquireLock(LockType type, long timeoutMS) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean releaseLock(LockType type, String token) {
		// TODO Auto-generated method stub
		return false;
	}

}
