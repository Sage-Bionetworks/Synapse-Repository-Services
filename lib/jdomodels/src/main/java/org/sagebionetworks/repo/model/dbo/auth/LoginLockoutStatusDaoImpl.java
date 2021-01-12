package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT;

import java.sql.ResultSet;

import org.sagebionetworks.repo.model.auth.LockoutInfo;
import org.sagebionetworks.repo.model.auth.LoginLockoutStatusDao;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoginLockoutStatusDaoImpl implements LoginLockoutStatusDao {

	public static final String CURRENT_TIME_AS_UNSIGNED = "CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED)";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public LockoutInfo getLockoutInfo(Long userId) {
		ValidateArgument.required(userId, "userId");
		try {
			String sql = "SELECT " + COL_UNSUCCESSFUL_LOGIN_COUNT + ", "
					+ COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + " - " + CURRENT_TIME_AS_UNSIGNED
					+ " AS REMAINING  FROM " + TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT + " WHERE " + COL_UNSUCCESSFUL_LOGIN_KEY
					+ " = ?";
			return jdbcTemplate.queryForObject(sql, (ResultSet rs, int rowNum) -> {
				Long failedAttempts = rs.getLong(COL_UNSUCCESSFUL_LOGIN_COUNT);
				Long remaining = rs.getLong("REMAINING");
				return new LockoutInfo().withNumberOfFailedLoginAttempts(failedAttempts)
						.withRemainingMillisecondsToNextLoginAttempt(remaining);
			}, userId);
		} catch (EmptyResultDataAccessException e) {
			return new LockoutInfo().withNumberOfFailedLoginAttempts(0L)
					.withRemainingMillisecondsToNextLoginAttempt(0L);
		}
	}

	@NewWriteTransaction
	@Override
	public void incrementLockoutInfoWithNewTransaction(Long userId) {
		ValidateArgument.required(userId, "userId");
		String sql = "INSERT INTO " + TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT + "(" + COL_UNSUCCESSFUL_LOGIN_KEY + ","
				+ COL_UNSUCCESSFUL_LOGIN_COUNT + "," + COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS
				+ ") VALUES (?, 1, " + CURRENT_TIME_AS_UNSIGNED + "+(1 << 1)) ON DUPLICATE KEY UPDATE "
				+ COL_UNSUCCESSFUL_LOGIN_COUNT + " = " + COL_UNSUCCESSFUL_LOGIN_COUNT + "+1, "
				+ COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + " = " + CURRENT_TIME_AS_UNSIGNED
				+ "+(1 << " + COL_UNSUCCESSFUL_LOGIN_COUNT + ")";
		jdbcTemplate.update(sql, userId);
	}

	@NewWriteTransaction
	@Override
	public void resetLockoutInfoWithNewTransaction(Long userId) {
		ValidateArgument.required(userId, "userId");
		String sql = "INSERT INTO " + TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT + "(" + COL_UNSUCCESSFUL_LOGIN_KEY + ","
				+ COL_UNSUCCESSFUL_LOGIN_COUNT + "," + COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS
				+ ") VALUES (?, 0, 0) ON DUPLICATE KEY UPDATE " + COL_UNSUCCESSFUL_LOGIN_COUNT + " = 0 , "
				+ COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + " = 0";
		jdbcTemplate.update(sql, userId);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.batchUpdate("TRUNCATE TABLE "+TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT);
	}

}
