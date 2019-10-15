package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_ID;
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

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.principal.EmailQuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EmailQuarantineDaoImpl implements EmailQuarantineDao {

	private static final RowMapper<DBOQuarantinedEmail> DBO_MAPPER = new DBOQuarantinedEmail().getTableMapping();
	
	private static final ResultSetExtractor<Optional<QuarantinedEmail>> RS_EXTRACTOR = (ResultSet rs) -> {
		if (rs.next()) {
			return Optional.of(map(DBO_MAPPER.mapRow(rs, rs.getRow())));
		}
		return Optional.empty();
	};

	// @formatter:off

	private static QuarantinedEmail map(DBOQuarantinedEmail dbo) {
		return new QuarantinedEmail(dbo.getEmail(), EmailQuarantineReason.valueOf(dbo.getReason()))
				.withCreatedOn(dbo.getCreatedOn().toInstant())
				.withUpdatedOn(dbo.getUpdatedOn().toInstant())
				.withExpiresOn(dbo.getExpiresOn() == null ? null : dbo.getExpiresOn().toInstant())
				.withReasonDetails(dbo.getReasonDetails())
				.withSesMessageId(dbo.getSesMessageId());
	}

	private static String SQL_INSERT = "INSERT INTO " + TABLE_QUARANTINED_EMAILS
			+ "(" + COL_QUARANTINED_EMAILS_ID + ", " 
			+ COL_QUARANTINED_EMAILS_EMAIL + ", "
			+ COL_QUARANTINED_EMAILS_ETAG + ", "
			+ COL_QUARANTINED_EMAILS_CREATED_ON + ", "
			+ COL_QUARANTINED_EMAILS_UPDATED_ON + ", " 
			+ COL_QUARANTINED_EMAILS_EXPIRES_ON + ", "
			+ COL_QUARANTINED_EMAILS_REASON + ", "
			+ COL_QUARANTINED_EMAILS_REASON_DETAILS + ", "
			+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + ") "
			+ "VALUES (?, ?, UUID(), ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
			+ COL_QUARANTINED_EMAILS_ETAG + " = UUID(), "
			+ COL_QUARANTINED_EMAILS_UPDATED_ON + " = ?, " 
			+ COL_QUARANTINED_EMAILS_EXPIRES_ON + " = ?, "
			+ COL_QUARANTINED_EMAILS_REASON + " = ?, "
			+ COL_QUARANTINED_EMAILS_REASON_DETAILS + " = ?, "
			+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + " = ?";
	 
	// @formatter:on

	private JdbcTemplate jdbcTemplate;

	private IdGenerator idGenerator;

	@Autowired
	public EmailQuarantineDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@Override
	@WriteTransaction
	public void addToQuarantine(QuarantinedEmailBatch batch) {
		ValidateArgument.required(batch, "The batch");

		if (batch.isEmpty()) {
			return;
		}

		jdbcTemplate.batchUpdate(SQL_INSERT, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				QuarantinedEmail dto = batch.get(i);
				
				String email = dto.getEmail().trim().toLowerCase();
				String reason = dto.getReason().toString();
				String reasonDetails = dto.getReasonDetails();
				String messageId = dto.getSesMessageId();

				Timestamp updatedOn = Timestamp.from(Instant.now());
				Timestamp expiresOn = batch.getExpiration().map(Timestamp::from).orElse(null);

				int index = 1;

				// On create fields
				ps.setLong(index++, idGenerator.generateNewId(IdType.QUARANTINED_EMAIL_ID));
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

			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});

	}

	@Override
	@WriteTransaction
	public boolean removeFromQuarantine(String email) {
		validateInputEmail(email);

		String sql = "DELETE FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ?";

		int result = jdbcTemplate.update(sql, email);

		return result != 0;
	}

	@Override
	public Optional<QuarantinedEmail> getQuarantinedEmail(String email) {
		return getQuarantinedEmail(email, true);
	}

	@Override
	public Optional<QuarantinedEmail> getQuarantinedEmail(String email, boolean expirationCheck) {
		validateInputEmail(email);

		StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ?");

		if (expirationCheck) {
			sql.append(" AND (" + COL_QUARANTINED_EMAILS_EXPIRES_ON + " IS NULL OR " + COL_QUARANTINED_EMAILS_EXPIRES_ON + " > ?)");
			return jdbcTemplate.query(sql.toString(), RS_EXTRACTOR, email, Timestamp.from(Instant.now()));
		} else {
			return jdbcTemplate.query(sql.toString(), RS_EXTRACTOR, email);
		}
	}

	@Override
	public boolean isQuarantined(String email) {
		validateInputEmail(email);

		String sql = "SELECT COUNT(*) FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ? AND ("
				+ COL_QUARANTINED_EMAILS_EXPIRES_ON + " IS NULL OR " + COL_QUARANTINED_EMAILS_EXPIRES_ON + " > ?)";

		return jdbcTemplate.queryForObject(sql, Long.class, email, Timestamp.from(Instant.now())) > 0;
	}

	@Override
	public void clearAll() {
		String sql = "DELETE FROM " + TABLE_QUARANTINED_EMAILS;
		jdbcTemplate.update(sql);
	}

	private void validateInputEmail(String email) {
		ValidateArgument.requiredNotBlank(email, "The email address");
	}

}
