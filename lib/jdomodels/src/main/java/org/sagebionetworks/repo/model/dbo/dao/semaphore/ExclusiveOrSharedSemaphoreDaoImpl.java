package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.exception.LockReleaseFailedException;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Basic database backed implemention of the ExclusiveOrSharedSemaphoreDao.
 * @author jmhill
 *
 */
public class ExclusiveOrSharedSemaphoreDaoImpl implements
		ExclusiveOrSharedSemaphoreDao {

	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Override
	public String aquireSharedLock(String lockKey, long timeoutMS)
			throws LockUnavilableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releaseSharedLock(String lockKey, String token)
			throws LockReleaseFailedException {
		// TODO Auto-generated method stub

	}

	@Override
	public String requestExclusiveLockToken(String lockKey)
			throws LockUnavilableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String aquireExclusiveLock(String lockKey, String requestToken,
			long timeoutMS) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releaseExclusiveLock(String lockKey, String token)
			throws LockReleaseFailedException {
		// TODO Auto-generated method stub

	}

}
