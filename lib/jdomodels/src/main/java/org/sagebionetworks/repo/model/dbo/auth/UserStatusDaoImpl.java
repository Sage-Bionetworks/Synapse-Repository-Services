package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_DISABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_LAST_SEEN_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL;

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
		
		String sql = "INSERT INTO " + TABLE_USER_STATUS + " ("
				+ COL_USER_STATUS_PRINCIPAL_ID + ", "
				+ COL_USER_STATUS_ETAG + ","
				+ COL_USER_STATUS_LAST_SEEN_ON + ","
				+ COL_USER_STATUS_DISABLED + ") "
				+ "VALUES (?, UUID(), ?, false) "
				+ "ON DUPLICATE KEY UPDATE "
				+ COL_USER_STATUS_ETAG + " = UUID(),"
				+ COL_USER_STATUS_LAST_SEEN_ON + " = ?";
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, principalIds.get(i));
				ps.setTimestamp(2, new Timestamp(lastSeenOn.getTime()));
				ps.setTimestamp(3, new Timestamp(lastSeenOn.getTime()));
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
				"SELECT " + COL_USER_STATUS_LAST_SEEN_ON + " FROM " + TABLE_USER_STATUS + " WHERE " + COL_USER_STATUS_PRINCIPAL_ID + "=?",
				Date.class, principalId).stream().findFirst();
	}
	
	@Override
	@WriteTransaction
	public void setDisabled(long principalId, boolean disabled) {
		String sql = "INSERT INTO " + TABLE_USER_STATUS + " ("
			+ COL_USER_STATUS_PRINCIPAL_ID + ", "
			+ COL_USER_STATUS_ETAG + ","
			+ COL_USER_STATUS_DISABLED + ") "
			+ "VALUES (?, UUID(), ?) "
			+ "ON DUPLICATE KEY UPDATE "
			+ COL_USER_STATUS_ETAG + " = UUID(),"
			+ COL_USER_STATUS_DISABLED + " = ?";
		
		jdbcTemplate.update(sql, principalId, disabled, disabled);
	}
	
	@Override
	public boolean isDisabled(long principalId) {
		return jdbcTemplate.queryForList(
				"SELECT " + COL_USER_STATUS_DISABLED + " FROM " + TABLE_USER_STATUS + " WHERE " + COL_USER_STATUS_PRINCIPAL_ID + "=?",
				Boolean.class, principalId).stream().findFirst().orElse(false);
	}
	
	@Override
	public List<Long> getInactiveUsersBatch(Date lastSeenOnThreshold, int batchSize) {
		return jdbcTemplate.queryForList(
				"SELECT " + COL_USER_STATUS_PRINCIPAL_ID + " FROM " + TABLE_USER_STATUS + " WHERE "
				+ COL_USER_STATUS_DISABLED + " = false AND "
				+ COL_USER_STATUS_LAST_SEEN_ON + " < ? LIMIT ?",
				Long.class, lastSeenOnThreshold, batchSize);
	}

	@Override
	public List<Long> getNeverSeenUsersBatch(int batchSize) {
		return jdbcTemplate.queryForList(
				"SELECT U." + COL_USER_GROUP_ID + " FROM " + TABLE_USER_GROUP + " U LEFT JOIN " + TABLE_USER_STATUS + " S"
				+ " ON U." + COL_USER_GROUP_ID + " = S." + COL_USER_STATUS_PRINCIPAL_ID + " WHERE "
				+ "U." + COL_USER_GROUP_IS_INDIVIDUAL + " = true AND "
				+ "S." + COL_USER_STATUS_LAST_SEEN_ON + " IS NULL LIMIT ?",
				Long.class, batchSize);
	}
	
}
