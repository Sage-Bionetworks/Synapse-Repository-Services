package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class UnsuccessfulAttemptLockoutDAOImpl implements UnsuccessfulAttemptLockoutDAO {

	private static final String FROM_TABLE_FILTERED_BY_KEY = " FROM " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + " = ?";

	private static final String SELECT_EXPIRATION_AND_LOCK_ROW = "SELECT CASE WHEN " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC + " <= CURRENT_TIMESTAMP THEN null ELSE " + " UNIX_TIMESTAMP(" + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC + ") END"
			+ FROM_TABLE_FILTERED_BY_KEY
			 + " FOR UPDATE";

	private static final String SELECT_UNSUCCESSFUL_ATTEMPTS = "SELECT " + COL_UNSUCCESSFUL_ATTEMPT_COUNT
			+ FROM_TABLE_FILTERED_BY_KEY;

	private static final String REMOVE_LOCKOUT = "DELETE " + FROM_TABLE_FILTERED_BY_KEY;

	private static final String CREATE_OR_INCREMENT_ATTEMPT_COUNT = "INSERT INTO " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " (" + COL_UNSUCCESSFUL_ATTEMPT_KEY + "," + COL_UNSUCCESSFUL_ATTEMPT_COUNT+","+COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC+")"
			+ " VALUES (?, 1, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE " + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "=" + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "+1";

	private static final String UPDATE_EXPIRATION = "UPDATE " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " SET " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC + "=CURRENT_TIMESTAMP + INTERVAL ? SECOND" + " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + "=?";

	@Autowired
	JdbcTemplate jdbcTemplate;

	@WriteTransactionReadCommitted
	@Override
	public long incrementNumFailedAttempts(String key) {
		jdbcTemplate.update(CREATE_OR_INCREMENT_ATTEMPT_COUNT, key);
		return getNumFailedAttempts(key);
	}

	@WriteTransactionReadCommitted
	@Override
	public long getNumFailedAttempts(String key){
		return jdbcTemplate.queryForObject(SELECT_UNSUCCESSFUL_ATTEMPTS, Long.class, key);
	}

	@WriteTransactionReadCommitted
	@Override
	public void setExpiration(String key, long expirationSecondsFromNow) {
		jdbcTemplate.update(UPDATE_EXPIRATION, expirationSecondsFromNow, key);
	}

	@WriteTransactionReadCommitted
	@Override
	public void removeLockout(String key) {
		jdbcTemplate.update(REMOVE_LOCKOUT, key);
	}

	@WriteTransactionReadCommitted
	@Override
	public Long getUnexpiredLockoutTimestampSec(String key) {
		try {
			return jdbcTemplate.queryForObject(SELECT_EXPIRATION_AND_LOCK_ROW, Long.class, key);
		}catch (EmptyResultDataAccessException e){
			return null;
		}
	}

	public void truncateTable(){
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT);
	}
}
