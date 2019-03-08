package org.sagebionetworks.repo.model.dbo.auth;

import org.sagebionetworks.repo.model.auth.PasswordResetTokenDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBOPasswordResetTokenDAOImpl implements PasswordResetTokenDAO {
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public String createOrRefreshResetToken(final long principalId, final long expirationDurationMillis) {
		return null;
	}

	@Override
	public Long getUserIdIfValid(final String token) {
		return null;
	}

	@Override
	public void nullifyToken(final String token) {

	}
}
