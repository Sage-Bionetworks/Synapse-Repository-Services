package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_SES_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_TIMEOUT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUARANTINED_EMAILS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
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

	private static QuarantinedEmail map(DBOQuarantinedEmail dbo) {
		QuarantinedEmail dto = new QuarantinedEmail();

		dto.setEmail(dbo.getEmail());
		dto.setCreatedOn(dbo.getCreatedOn().toInstant());
		dto.setUpdatedOn(dbo.getCreatedOn().toInstant());

		if (dbo.getTimeout() != null) {
			dto.setTimeout(dbo.getTimeout().toInstant());
		}

		dto.setReason(QuarantineReason.valueOf(dbo.getReason()));
		dto.setSesMessageId(dbo.getSesMessageId());

		return dto;
	}

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public EmailQuarantineDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public QuarantinedEmail addToQuarantine(QuarantinedEmail quarantinedEmail) {
		ValidateArgument.required(quarantinedEmail, "The quarantineEmail");
		ValidateArgument.requiredNotBlank(quarantinedEmail.getEmail(), "The email address");
		ValidateArgument.required(quarantinedEmail.getReason(), "The quarantine reason");
		
		Instant now = Instant.now();
		
		if (quarantinedEmail.getTimeout() != null) {			
			ValidateArgument.requirement(quarantinedEmail.getTimeout().isAfter(now), "The timeout value must be in the future");
		}

		// @formatter:off

		String sql = "INSERT INTO " + TABLE_QUARANTINED_EMAILS
				+ "(" + COL_QUARANTINED_EMAILS_EMAIL + ", "
				+ COL_QUARANTINED_EMAILS_CREATED_ON + ", "
				+ COL_QUARANTINED_EMAILS_UPDATED_ON + ", " 
				+ COL_QUARANTINED_EMAILS_TIMEOUT + ", "
				+ COL_QUARANTINED_EMAILS_REASON + ", "
				+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + ") "
				+ "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
				+ COL_QUARANTINED_EMAILS_UPDATED_ON + " = ?, " 
				+ COL_QUARANTINED_EMAILS_TIMEOUT + " = ?, "
				+ COL_QUARANTINED_EMAILS_REASON + " = ?, "
				+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + " = ?";
		 
		// @formatter:on

		Timestamp nowTimestamp = Timestamp.from(now);
		String email = quarantinedEmail.getEmail().toLowerCase();
		String reason = quarantinedEmail.getReason().toString();
		String messageId = quarantinedEmail.getSesMessageId();
		Timestamp timeout = quarantinedEmail.getTimeout() == null ? null : Timestamp.from(quarantinedEmail.getTimeout());

		jdbcTemplate.update(sql, ps -> {
			int index = 1;

			// On create fields
			ps.setString(index++, email);
			ps.setTimestamp(index++, nowTimestamp);
			ps.setTimestamp(index++, nowTimestamp);
			ps.setTimestamp(index++, timeout);
			ps.setString(index++, reason);
			ps.setString(index++, messageId);
			// On update fields
			ps.setTimestamp(index++, nowTimestamp);
			ps.setTimestamp(index++, timeout);
			ps.setString(index++, reason);
			ps.setString(index++, messageId);
		});

		return getQuarantinedEmail(email).get();
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

}
