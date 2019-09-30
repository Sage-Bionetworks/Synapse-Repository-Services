package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_REASON_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_SES_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUARANTINED_EMAILS;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EmailQuarantineDaoImpl implements EmailQuarantineDao {

	private static final RowMapper<DBOQuarantinedEmail> DBO_MAPPER = new DBOQuarantinedEmail().getTableMapping();

	private static final RowMapper<QuarantinedEmail> ROW_MAPPER = new RowMapper<QuarantinedEmail>() {

		@Override
		public QuarantinedEmail mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	// @formatter:off

	private static QuarantinedEmail map(DBOQuarantinedEmail dbo) {
		return new QuarantinedEmail(dbo.getEmail(), QuarantineReason.valueOf(dbo.getReason()))
				.withCreatedOn(dbo.getCreatedOn().toInstant())
				.withUpdatedOn(dbo.getUpdatedOn().toInstant())
				.withExpiresOn(dbo.getExpiresOn() == null ? null : dbo.getExpiresOn().toInstant())
				.withReasonDetails(dbo.getReasonDetails())
				.withSesMessageId(dbo.getSesMessageId());
	}

	private static String SQL_INSERT = "INSERT INTO " + TABLE_QUARANTINED_EMAILS
			+ "(" + COL_QUARANTINED_EMAILS_EMAIL + ", "
			+ COL_QUARANTINED_EMAILS_ETAG + ", "
			+ COL_QUARANTINED_EMAILS_CREATED_ON + ", "
			+ COL_QUARANTINED_EMAILS_UPDATED_ON + ", " 
			+ COL_QUARANTINED_EMAILS_EXPIRES_ON + ", "
			+ COL_QUARANTINED_EMAILS_REASON + ", "
			+ COL_QUARANTINED_EMAILS_REASON_DETAILS + ", "
			+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + ") "
			+ "VALUES (?, UUID(), ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
			+ COL_QUARANTINED_EMAILS_ETAG + " = UUID(), "
			+ COL_QUARANTINED_EMAILS_UPDATED_ON + " = ?, " 
			+ COL_QUARANTINED_EMAILS_EXPIRES_ON + " = ?, "
			+ COL_QUARANTINED_EMAILS_REASON + " = ?, "
			+ COL_QUARANTINED_EMAILS_REASON_DETAILS + " = ?, "
			+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + " = ?";
	 
	// @formatter:on

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public EmailQuarantineDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public QuarantinedEmail addToQuarantine(QuarantinedEmail quarantinedEmail, Long expirationTimeout) {
		ValidateArgument.required(quarantinedEmail, "The quarantineEmail");
		validateExpirationTimeout(expirationTimeout);

		Instant now = Instant.now();

		jdbcTemplate.update(SQL_INSERT, ps -> {
			setPreparedStatementForInsert(ps, quarantinedEmail, now, expirationTimeout);
		});

		return getQuarantinedEmail(quarantinedEmail.getEmail()).get();
	}

	@Override
	@WriteTransaction
	public void addToQuarantine(QuarantinedEmailBatch batch) {
		ValidateArgument.required(batch, "The batch");
		validateExpirationTimeout(batch.getExpirationTimeout());

		if (batch.isEmpty()) {
			return;
		}

		jdbcTemplate.batchUpdate(SQL_INSERT, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				setPreparedStatementForInsert(ps, batch.get(i), Instant.now(), batch.getExpirationTimeout());
			}

			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});

	}

	@Override
	@WriteTransaction
	public boolean removeFromQuarantine(String email) {
		ValidateArgument.requiredNotBlank(email, "The email");

		String sql = "DELETE FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ?";

		int result = jdbcTemplate.update(sql, email);

		return result != 0;
	}

	@Override
	public Optional<QuarantinedEmail> getQuarantinedEmail(String email) {
		ValidateArgument.requiredNotBlank(email, "The email");

		String sql = "SELECT * FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ?";

		return jdbcTemplate.query(sql, rs -> {
			if (rs.next()) {
				return Optional.of(ROW_MAPPER.mapRow(rs, rs.getRow()));
			}
			return Optional.empty();
		}, email);
	}

	@Override
	public void clearAll() {
		String sql = "DELETE FROM " + TABLE_QUARANTINED_EMAILS;
		jdbcTemplate.update(sql);
	}

	private void validateExpirationTimeout(Long expirationTimeout) {
		if (expirationTimeout == null) {
			return;
		}
		ValidateArgument.requirement(expirationTimeout > 0, "The expiration timeout must be greater than zero");
	}

	private void setPreparedStatementForInsert(PreparedStatement ps, QuarantinedEmail quarantinedEmail, Instant now, Long timeout)
			throws SQLException {
		String email = quarantinedEmail.getEmail().trim().toLowerCase();
		String reason = quarantinedEmail.getReason().toString();
		String reasonDetails = quarantinedEmail.getReasonDetails();
		String messageId = quarantinedEmail.getSesMessageId();

		Timestamp updatedOn = Timestamp.from(now);
		Timestamp expiresOn = timeout == null ? null : Timestamp.from(now.plusMillis(timeout));

		int index = 1;

		// On create fields
		ps.setString(index++, email);
		ps.setTimestamp(index++, updatedOn);
		ps.setTimestamp(index++, updatedOn);
		ps.setTimestamp(index++, expiresOn);
		ps.setString(index++, reason);
		ps.setString(index++, reasonDetails);
		ps.setString(index++, messageId);
		// On update fields
		ps.setTimestamp(index++, updatedOn);
		ps.setTimestamp(index++, expiresOn);
		ps.setString(index++, reason);
		ps.setString(index++, reasonDetails);
		ps.setString(index++, messageId);
	}

}
