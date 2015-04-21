package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOExclusiveLock;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSharedLock;
import org.sagebionetworks.repo.model.exception.LockReleaseFailedException;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * Basic database backed implementation of the ExclusiveOrSharedSemaphoreDao.
 * 
 * @author jmhill
 *
 */
public class ExclusiveOrSharedSemaphoreDaoImpl implements
		ExclusiveOrSharedSemaphoreDao {
	
	private static final String SQL_COUNT_SHARED_LOCKS_FOR_RESOURCE = "SELECT COUNT(*) FROM "+TABLE_SHARED_SEMAPHORE+" WHERE "+COL_SHARED_SEMAPHORE_KEY+" = ?";
	private static final String SQL_FORCE_RELEASE_EXPIRED_SHARED_LOCKS = "DELETE FROM "+TABLE_SHARED_SEMAPHORE+" WHERE "+COL_SHARED_SEMAPHORE_KEY+" = ? AND "+COL_SHARED_SEMAPHORE_EXPIRES+" < ?";
	private static final String SQL_DELETE_ALL_LOCKS = "DELETE FROM "+TABLE_EXCLUSIVE_SEMAPHORE+" WHERE "+COL_EXCLUSIVE_SEMAPHORE_KEY+" IS NOT NULL";
	private static final String SQL_SELECT_EXCLUSIVE_FOR_UPDATE = "SELECT * FROM "+TABLE_EXCLUSIVE_SEMAPHORE+" WHERE "+COL_EXCLUSIVE_SEMAPHORE_KEY+" = ? FOR UPDATE";
	private static final String SQL_RELEASE_SHARED_LOCK = "DELETE FROM "+TABLE_SHARED_SEMAPHORE+" WHERE "+COL_SHARED_SEMAPHORE_KEY+" = ? AND "+COL_SHARED_SEMAPHORE_LOCK_TOKEN+" = ?";
	private static final String SQL_RELEASE_EXCLUSIVE_LOCK = "UPDATE "+TABLE_EXCLUSIVE_SEMAPHORE+" SET "+COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN+" = NULL,"+COL_EXCLUSIVE_SEMAPHORE_PRECURSOR_TOKEN+" = NULL, "+COL_EXCLUSIVE_SEMAPHORE_EXPIRES+" = NULL WHERE "+COL_EXCLUSIVE_SEMAPHORE_KEY+" = ? AND "+COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN+" = ?";

	static private Logger log = LogManager.getLogger(DBOSemaphoreDaoImpl.class);
	
	/**
	 * Write-lock-precursor cannot be held for more than this time.
	 * Note: Each time the precursor token is used to attempt to acquire the write-lock the time is reset.
	 */
	public static final long WRITE_LOCK_PRECURSOR_TIMEOUT = 5000;// 5 SECDONS
	
	TableMapping<DBOExclusiveLock> exclusiveMapping = new DBOExclusiveLock().getTableMapping();
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private long maxSharedLockTimeoutMS;
	private long maxExclusiveLockTimeoutMS;
	

	/**
	 * Injected via Spring.
	 * @param maxSharedLockTimeoutMS
	 */
	public void setMaxSharedLockTimeoutMS(long maxSharedLockTimeoutMS) {
		this.maxSharedLockTimeoutMS = maxSharedLockTimeoutMS;
	}

	/**
	 * Injected via Spring.
	 * @param maxExclusiveLockTimeoutMS
	 */
	public void setMaxExclusiveLockTimeoutMS(long maxExclusiveLockTimeoutMS) {
		this.maxExclusiveLockTimeoutMS = maxExclusiveLockTimeoutMS;
	}

	@NewWriteTransaction
	@Override
	public String acquireSharedLock(String lockKey, long timeoutMS)	throws LockUnavilableException {
		if(lockKey == null) throw new IllegalArgumentException("Key cannot be null");
		if(timeoutMS < 1) throw new IllegalArgumentException("The lock timeout must be greater than one milliseconds");
		if(timeoutMS > maxSharedLockTimeoutMS) throw new IllegalArgumentException("Passed Timeout exceeds the maximum read-lock timeout of: "+maxExclusiveLockTimeoutMS+" ms");
		// First determine if the lock is already being held
		DBOExclusiveLock exclusiveLock = getExclusiveLockForUpdate(lockKey);
		// We cannot get a read lock if there is an outstanding write-lock request or a write-lock
		if(exclusiveLock.getExclusivePrecursorToken() != null || exclusiveLock.getExclusiveLockToken() != null){
			throw new LockUnavilableException("Cannot issue a read-lock at this time on resource: "+lockKey);
		}
		// We can issue a read-lock
		DBOSharedLock shared = new DBOSharedLock();
		shared.setKey(lockKey);
		shared.setToken(UUID.randomUUID().toString());
		shared.setExpiration(timeoutMS+System.currentTimeMillis());
		basicDao.createNew(shared);
		return shared.getToken();
	}
	
	/**
	 * All methods start here. By getting a database level exclusive lock on the row we can ensure all lock changes
	 * occur synchronously.
	 * @return
	 */
	private DBOExclusiveLock getExclusiveLockForUpdate(String key){
		try{
			// If there is no row in the exclusive lock table for this key then an EmptyResultDataAccessException will be thrown.
			DBOExclusiveLock exclusive = simpleJdbcTemplate.queryForObject(SQL_SELECT_EXCLUSIVE_FOR_UPDATE, exclusiveMapping, key);
			// Is there a lock and is it expired?
			if(exclusive.getExpiration() != null){
				long now = System.currentTimeMillis();
				if(now > exclusive.getExpiration()){
					// the current lock is expired so force its release
					exclusive.setExclusiveLockToken(null);
					exclusive.setExclusivePrecursorToken(null);
					exclusive.setExpiration(null);
					basicDao.update(exclusive);
				}
			}
			return exclusive;
		}catch(EmptyResultDataAccessException e){
			// This means the lock is not current held so attempt to get the lock
			return tryInsert(key);
		}
	}
	
	/**
	 * Try to insert a row to acquire the lock.
	 * @param timeoutMS
	 * @param id
	 * @return The token will be returned if successful, else null
	 */
	private DBOExclusiveLock tryInsert(String key) {
		try{
			DBOExclusiveLock dbo = new DBOExclusiveLock();
			dbo.setExpiration(null);
			dbo.setKey(key);
			dbo = basicDao.createNew(dbo);
			return dbo;
		}catch(Exception e){
			// This should not happen but if it does we log it.
			log.debug("Failed to insert a new row for: "+key, e);
			// We failed to get the lock
			throw new LockUnavilableException();
		}
	}
	
	@NewWriteTransaction
	@Override
	public void releaseSharedLock(String lockKey, String token) throws LockReleaseFailedException {
		// try to release the lock
		int update = simpleJdbcTemplate.update(SQL_RELEASE_SHARED_LOCK, lockKey, token);
		if(update < 1){
			throw new LockReleaseFailedException("Failed to release the lock for key: "+lockKey+" and token: "+token+".  Expired locks can be forcibly removed.");
		}
	}

	@NewWriteTransaction
	@Override
	public String acquireExclusiveLockPrecursor(String lockKey)
			throws LockUnavilableException {
		// First determine if the lock is already being held
		DBOExclusiveLock exclusiveLock = getExclusiveLockForUpdate(lockKey);
		// We cannot get a write lock if there is an outstanding write-lock request or a write-lock
		if(exclusiveLock.getExclusivePrecursorToken() != null || exclusiveLock.getExclusiveLockToken() != null){
			throw new LockUnavilableException("Cannot issue a write-lock at this time on resource: "+lockKey);
		}
		// Issue the write-lock request token
		exclusiveLock.setExclusivePrecursorToken(UUID.randomUUID().toString());
		exclusiveLock.setExpiration(WRITE_LOCK_PRECURSOR_TIMEOUT+System.currentTimeMillis());
		// update the row
		basicDao.update(exclusiveLock);
		return exclusiveLock.getExclusivePrecursorToken();
	}

	@NewWriteTransaction
	@Override
	public String acquireExclusiveLock(String lockKey, String exclusiveLockPrecursorToken,
			long timeoutMS) {
		if(lockKey == null) throw new IllegalArgumentException("Key cannot be null");
		if(exclusiveLockPrecursorToken == null) throw new IllegalArgumentException("exclusiveLockPrecursorToken cannot be null");
		if(timeoutMS < 1) throw new IllegalArgumentException("The lock timeout must be greater than one milliseconds");
		if(timeoutMS > maxExclusiveLockTimeoutMS) throw new IllegalArgumentException("Passed Timeout exceeds the maximum write-lock timeout of: "+maxExclusiveLockTimeoutMS+" ms");
		// Get the current exclusive row
		DBOExclusiveLock exclusiveLock = getExclusiveLockForUpdate(lockKey);
		// Validate that the token matches
		if(exclusiveLock.getExclusivePrecursorToken() == null){
			throw new IllegalArgumentException("This method can only be called after acquiring the write-lock-precursor token.  A precursor token does not exist for: "+lockKey);
		}
		// Next validate that it still matches
		if(!exclusiveLock.getExclusivePrecursorToken().equals(exclusiveLockPrecursorToken)){
			throw new IllegalArgumentException("The passed write-lock-precursor token is invalid. It does not match the expected token for: "+lockKey);
		}

		// Now count the remaining shared locks for this resource.
		long outstandingReadLocks = countOutstandingNonExpiredReadLock(lockKey);
		if(outstandingReadLocks > 0){
			// We cannot issue a lock yet because there are outstanding read locks.
			// We need to refresh the timer on the write-lock-precursor.
			exclusiveLock.setExpiration(WRITE_LOCK_PRECURSOR_TIMEOUT+System.currentTimeMillis());
			// update the row
			basicDao.update(exclusiveLock);
			return null;
		}
		// There are no more read locks so issue the actual write-lock token
		exclusiveLock.setExclusiveLockToken(UUID.randomUUID().toString());
		exclusiveLock.setExpiration(timeoutMS+System.currentTimeMillis());
		// update the row
		basicDao.update(exclusiveLock);
		return exclusiveLock.getExclusiveLockToken();
	}

	private long countOutstandingNonExpiredReadLock(String lockKey) {
		// First delete all expired shared locks for this resource.
		this.simpleJdbcTemplate.update(SQL_FORCE_RELEASE_EXPIRED_SHARED_LOCKS, lockKey, System.currentTimeMillis());
		// Now count the remaining locks
		return this.simpleJdbcTemplate.queryForLong(SQL_COUNT_SHARED_LOCKS_FOR_RESOURCE, lockKey);
	}

	@NewWriteTransaction
	@Override
	public void releaseExclusiveLock(String lockKey, String token)
			throws LockReleaseFailedException {
		if(lockKey == null) throw new IllegalArgumentException("Key cannot be null");
		if(token == null) throw new IllegalArgumentException("Token cannot be null");
		// try to release the lock
		int update = simpleJdbcTemplate.update(SQL_RELEASE_EXCLUSIVE_LOCK, lockKey, token);
		if(update < 1){
			throw new LockReleaseFailedException("Failed to release the lock for key: "+lockKey+" and token: "+token+".  Expired locks can be forcibly removed.");
		}
	}
	
	@Override
	public void releaseAllLocks() {
		// Force the delete of all locks
		// This is basically a truncate table with cascade deletes cleaning up the read locks.
		this.simpleJdbcTemplate.update(SQL_DELETE_ALL_LOCKS);
	}

}
