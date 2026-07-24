package org.sagebionetworks.repo.model.dbo.auth;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserStatusDaoImpl implements UserStatusDao {

	private JdbcTemplate jdbcTemplate;

	public UserStatusDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void setLastSeenOn(List<Long> principalIds, Date lastSeenOn) {
		jdbcTemplate.batchUpdate(
				"INSERT INTO USER_STATUS (PRINCIPAL_ID, ETAG, LAST_SEEN_ON, DISABLED)"
				+ " VALUES (?, UUID(), ?, false) AS new_row"
				+ " ON DUPLICATE KEY UPDATE ETAG = UUID(), LAST_SEEN_ON = new_row.LAST_SEEN_ON, DISABLE_WARNING_SENT_ON = NULL",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						ps.setLong(1, principalIds.get(i));
						ps.setTimestamp(2, new Timestamp(lastSeenOn.getTime()));
					}
					@Override
					public int getBatchSize() {
						return principalIds.size();
					}
				});
	}

	@Override
	public Optional<Date> getLastSeenOn(long principalId) {
		return jdbcTemplate.queryForList(
				"SELECT LAST_SEEN_ON FROM USER_STATUS WHERE PRINCIPAL_ID = ?",
				Date.class, principalId).stream().findFirst();
	}

	@Override
	@WriteTransaction
	public void setDisabled(long principalId, boolean disabled) {
		jdbcTemplate.update(
				"INSERT INTO USER_STATUS (PRINCIPAL_ID, ETAG, DISABLED)"
				+ " VALUES (?, UUID(), ?) AS new_row"
				+ " ON DUPLICATE KEY UPDATE ETAG = UUID(), DISABLED = new_row.DISABLED",
				principalId, disabled);
	}

	@WriteTransaction
	@Override
	public void enableUser(long principalId) {
		jdbcTemplate.update(
				"INSERT INTO USER_STATUS (PRINCIPAL_ID, ETAG, LAST_SEEN_ON, DISABLED)"
				+ " VALUES (?, UUID(), NOW(3), false)"
				+ " ON DUPLICATE KEY UPDATE ETAG = UUID(), LAST_SEEN_ON = NOW(3), DISABLED = false, DISABLE_WARNING_SENT_ON = NULL",
				principalId);
	}

	@Override
	public boolean isDisabled(long principalId) {
		return jdbcTemplate.queryForList(
				"SELECT DISABLED FROM USER_STATUS WHERE PRINCIPAL_ID = ?",
				Boolean.class, principalId).stream().findFirst().orElse(false);
	}

	@Override
	public List<Long> getInactiveUsersBatch(Date lastSeenOnThreshold, int batchSize) {
		return jdbcTemplate.queryForList(
				"SELECT PRINCIPAL_ID FROM USER_STATUS WHERE DISABLED = false AND LAST_SEEN_ON < ? LIMIT ?",
				Long.class, lastSeenOnThreshold, batchSize);
	}

	@Override
	public List<Long> getInactiveUsersToWarnBatch(Date warningThreshold, Date disableThreshold, int batchSize) {
		return jdbcTemplate.queryForList(
				"SELECT PRINCIPAL_ID FROM USER_STATUS WHERE DISABLED = false AND LAST_SEEN_ON < ? AND LAST_SEEN_ON >= ? AND DISABLE_WARNING_SENT_ON IS NULL LIMIT ?",
				Long.class, warningThreshold, disableThreshold, batchSize);
	}

	@Override
	@WriteTransaction
	public void setDisableWarningSentOn(List<Long> principalIds) {
		jdbcTemplate.batchUpdate(
				"UPDATE USER_STATUS SET DISABLE_WARNING_SENT_ON = NOW(3), ETAG = UUID() WHERE PRINCIPAL_ID = ?",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						ps.setLong(1, principalIds.get(i));
					}
					@Override
					public int getBatchSize() {
						return principalIds.size();
					}
				});
	}

}
