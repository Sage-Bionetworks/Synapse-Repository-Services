package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEMAPHORE;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
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
	
	static private Log log = LogFactory.getLog(DBOSemaphoreDaoImpl.class);

	private static final String SQL_RELEASE_LOCK = "DELETE FROM "+TABLE_SEMAPHORE+" WHERE "+COL_SEMAPHORE_KEY+" = ? AND "+COL_SEMAPHORE_TOKEN+" = ?";

	private static final String UPDATE_LOCKED_ROW_WITH_NEW_TOKEN_AND_EXPIRES = "UPDATE "+TABLE_SEMAPHORE+" SET "+COL_SEMAPHORE_TOKEN+" = ?, "+COL_SEMAPHORE_EXPIRES+" = ? WHERE "+COL_SEMAPHORE_KEY+" = ?";

	private static final String SQL_SELECT_EXPIRES_FOR_UPDATE = "SELECT "+COL_SEMAPHORE_EXPIRES+" FROM "+TABLE_SEMAPHORE+" WHERE "+COL_SEMAPHORE_KEY+" = ? FOR UPDATE";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String attemptToAcquireLock(String key, long timeoutMS) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// First determine if the lock is already being held
		try{
			// If there is no lock then an EmptyResultDataAccessException will be thrown.
			// If there is a lock then we will hold a lock on this row in the database.
			long expires = simpleJdbcTemplate.queryForLong(SQL_SELECT_EXPIRES_FOR_UPDATE, key);
			// Is the lock expired?
			long currentTime = System.currentTimeMillis();
			if(currentTime > expires){
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
			log.warn("Failed to acquire lock: "+e.getMessage());
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

}
