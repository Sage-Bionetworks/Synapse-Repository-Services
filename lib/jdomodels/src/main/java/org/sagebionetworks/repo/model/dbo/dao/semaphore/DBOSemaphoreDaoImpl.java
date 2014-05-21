package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_EXPIRES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEMAPHORE;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple database backed implementation of SemaphoreDao.
 * @author jmhill
 *
 */
public class DBOSemaphoreDaoImpl implements SemaphoreDao {
	
	static private Logger log = LogManager.getLogger(DBOSemaphoreDaoImpl.class);

	private static final String SQL_RELEASE_LOCK =  "UPDATE "+TABLE_SEMAPHORE+" SET "+COL_SEMAPHORE_TOKEN+" = NULL, "+COL_SEMAPHORE_EXPIRES+" = NULL WHERE "+COL_SEMAPHORE_KEY+" = ? AND "+COL_SEMAPHORE_TOKEN+" = ?";

	private static final String UPDATE_LOCKED_ROW_WITH_NEW_TOKEN_AND_EXPIRES = "UPDATE "+TABLE_SEMAPHORE+" SET "+COL_SEMAPHORE_TOKEN+" = ?, "+COL_SEMAPHORE_EXPIRES+" = ? WHERE "+COL_SEMAPHORE_KEY+" = ?";

	private static final String SQL_SELECT_EXPIRES_FOR_UPDATE = "SELECT "+COL_SEMAPHORE_EXPIRES+" FROM "+TABLE_SEMAPHORE+" WHERE "+COL_SEMAPHORE_KEY+" = ? FOR UPDATE";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private TableMapping<DBOSemaphore> mapping = new DBOSemaphore().getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String attemptToAcquireLock(String key, long timeoutMS) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// First determine if the lock is already being held
		try{
			// If there is no lock then an EmptyResultDataAccessException will be thrown.
			// If there is a lock then we will hold a lock on this row in the database.
			Long expires = simpleJdbcTemplate.queryForObject(SQL_SELECT_EXPIRES_FOR_UPDATE, Long.class, key);
			// Is the lock expired?
			long currentTime = System.currentTimeMillis();
			if(expires == null || currentTime > expires){
				// the current lock is expired so we can grab it.
				// Issue the lock to the caller by updating the currently locked row.
				long newExpires = currentTime+timeoutMS;
				String newToken = UUID.randomUUID().toString();
				simpleJdbcTemplate.update(UPDATE_LOCKED_ROW_WITH_NEW_TOKEN_AND_EXPIRES, newToken, newExpires, key);
				return newToken;
			}else{
				/// The lock is not expired so it cannot be acquired at this time
				return null;
			}
		}catch(EmptyResultDataAccessException e){
			// This means the lock is not current held so attempt to get the lock
			return tryInsert(timeoutMS, key);
		}
	}

	/**
	 * Try to insert a row to acquire the lock.
	 * @param timeoutMS
	 * @param id
	 * @return The token will be returned if successful, else null
	 */
	private String tryInsert(long timeoutMS, String key) {
		try{
			long expires = System.currentTimeMillis()+timeoutMS;
			DBOSemaphore dbo = new DBOSemaphore();
			dbo.setExpiration(expires);
			dbo.setKey(key);
			String token = UUID.randomUUID().toString();
			dbo.setToken(token);
			dbo = basicDao.createNew(dbo);
			return token;
		}catch(Exception e){
			// if we fail to get the lock return null
			log.warn("Failed to acquire lock: " + key + " " + e.getMessage());
			return null;
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean releaseLock(String key, String token) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(token == null) throw new IllegalArgumentException("Token cannot be null");
		// Attempt to release a lock.  If the token does not match the current token it will not work.
		int result = simpleJdbcTemplate.update(SQL_RELEASE_LOCK, key, token);
		return result == 1;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void forceReleaseAllLocks() {
		// Set all locks to timeout
		Long now = System.currentTimeMillis();
		List<DBOSemaphore> allActiveLocks = simpleJdbcTemplate.query("SELECT * FROM "+TABLE_SEMAPHORE+" WHERE "+COL_SEMAPHORE_EXPIRES+" IS NOT NULL", mapping);
		// release each lock
		for(DBOSemaphore dbo: allActiveLocks){
			// Clear each token and timestamp
			dbo.setExpiration(null);
			dbo.setToken(null);
		}
		if(!allActiveLocks.isEmpty()){
			this.basicDao.createOrUpdateBatch(allActiveLocks);
		}
	}

}
