package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_SES_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUARANTINED_EMAILS;

import java.sql.Timestamp;
import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmailQuarantineDaoImpl implements EmailQuarantineDao {

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public EmailQuarantineDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void addToQuarantine(String email, QuarantineReason reason, String sesMessageId) {
		validateInputEmail(email);
		ValidateArgument.required(reason, "The quarantine reason");

		String sql = "INSERT INTO " + TABLE_QUARANTINED_EMAILS
				+ "(" + COL_QUARANTINED_EMAILS_EMAIL + ", "
				+ COL_QUARANTINED_EMAILS_CREATED_ON + ", "
				+ COL_QUARANTINED_EMAILS_UPDATED_ON + ", " 
				+ COL_QUARANTINED_EMAILS_REASON + ", "
				+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + ") "
				+ "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
				+ COL_QUARANTINED_EMAILS_UPDATED_ON + " = ?, " 
				+ COL_QUARANTINED_EMAILS_REASON + " = ?, "
				+ COL_QUARANTINED_EMAILS_SES_MESSAGE_ID + " = ?";

		String reasonString = reason.toString();
		Timestamp now = new Timestamp(System.currentTimeMillis());

		jdbcTemplate.update(sql, ps -> {
			int index = 1;

			// On create fields
			ps.setString(index++, email.toLowerCase());
			ps.setTimestamp(index++, now);
			ps.setTimestamp(index++, now);
			ps.setString(index++, reasonString);
			ps.setString(index++, sesMessageId);
			// On update fields
			ps.setTimestamp(index++, now);
			ps.setString(index++, reasonString);
			ps.setString(index, sesMessageId);
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
	public boolean isQuarantined(String email) {
		validateInputEmail(email);

		String sql = "SELECT EXISTS (SELECT 1 FROM " + TABLE_QUARANTINED_EMAILS + " WHERE " + COL_QUARANTINED_EMAILS_EMAIL + " = ?)";

		return jdbcTemplate.queryForObject(sql, Boolean.class, email);
	}

	@Override
	public Optional<QuarantineReason> getQuarantineReason(String email) {
		validateInputEmail(email);

		String sql = "SELECT " + COL_QUARANTINED_EMAILS_REASON + " FROM " + TABLE_QUARANTINED_EMAILS + " WHERE "
				+ COL_QUARANTINED_EMAILS_EMAIL + " = ?";

		Optional<QuarantineReason> reason = jdbcTemplate.query(sql, rs -> {
			if (rs.next()) {
				String reasonString = rs.getString(COL_QUARANTINED_EMAILS_REASON);
				return Optional.of(QuarantineReason.valueOf(reasonString));
			}
			return Optional.empty();
		}, email);

		return reason;

	}

	@Override
	public void clearAll() {
		String sql = "DELETE FROM " + TABLE_QUARANTINED_EMAILS;
		jdbcTemplate.update(sql);
	}

	private void validateInputEmail(String email) {
		ValidateArgument.requiredNotBlank(email, "The email");
	}

}
