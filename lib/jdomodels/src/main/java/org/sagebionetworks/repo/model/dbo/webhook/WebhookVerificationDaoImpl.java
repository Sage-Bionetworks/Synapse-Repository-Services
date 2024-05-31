package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ATTEMPTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_VERIFICATION;

import java.sql.ResultSet;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.webhook.WebhookVerification;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationState;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookVerificationDaoImpl implements WebhookVerificationDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private DBOBasicDao dboBasicDao;

	public static final String WEBHOOK_VERIFICATION_WEBHOOK_ID_FK = "WEBHOOK_VERIFICATION_WEBHOOK_ID_FK";
	public static final String WEBHOOK_VERIFICATION_WEBHOOK_ID_FK_MESSAGE = "Cannot create a WebhookVerification as there is no corresponding Webhook.";

	static final RowMapper<WebhookVerification> WEBHOOK_VERIFICATION_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		return new WebhookVerification().setWebhookId(rs.getString(COL_WEBHOOK_VERIFICATION_WEBHOOK_ID))
				.setVerificationCode(rs.getString(COL_WEBHOOK_VERIFICATION_CODE))
				.setExpiresOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_EXPIRES_ON))
				.setAttempts(rs.getLong(COL_WEBHOOK_VERIFICATION_ATTEMPTS))
				.setCreatedOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_CREATED_ON))
				.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_MODIFIED_ON))
				.setCreatedBy(rs.getString(COL_WEBHOOK_VERIFICATION_CREATED_BY))
				.setModifiedBy(rs.getString(COL_WEBHOOK_VERIFICATION_MODIFIED_BY));
	};

	@Override
	public WebhookVerification createWebhookVerification(WebhookVerification verification) {
		DBOWebhookVerification dbo = WebhookUtils.translateWebhookVerificationToDBOWebhookVerification(verification);
		dbo.setVerificationId(idGenerator.generateNewId(IdType.WEBHOOK_VERIFICATION_ID));
		dbo.setState(WebhookVerificationState.PENDING_VERIFICATION.name());
		dbo.setEtag(UUID.randomUUID().toString());

		try {
			dboBasicDao.createOrUpdate(dbo);
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage().contains(WEBHOOK_VERIFICATION_WEBHOOK_ID_FK)) {
				throw new NotFoundException(WEBHOOK_VERIFICATION_WEBHOOK_ID_FK_MESSAGE);
			} else {
				throw e;
			}
		}

		return getWebhookVerification(verification.getWebhookId());
	}

	@Override
	public WebhookVerification getWebhookVerification(String webhookId) {
		String selectSql = "SELECT * FROM " + TABLE_WEBHOOK_VERIFICATION + " WHERE "
				+ COL_WEBHOOK_VERIFICATION_WEBHOOK_ID + " = ?";

		try {
			return jdbcTemplate.queryForObject(selectSql, WEBHOOK_VERIFICATION_ROW_MAPPER, webhookId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("WebhookVerification for Webhook of ID: " + webhookId + " not found.", e);
		}
	}

	@Override
	public Long incrementAttempts(String webhookId) {
		String updateSql = "UPDATE " + TABLE_WEBHOOK_VERIFICATION + " SET " + COL_WEBHOOK_VERIFICATION_ATTEMPTS + " = "
				+ COL_WEBHOOK_VERIFICATION_ATTEMPTS + " + 1" + " WHERE " + COL_WEBHOOK_VERIFICATION_WEBHOOK_ID + " = ?";

		String selectSql = "SELECT " + COL_WEBHOOK_VERIFICATION_ATTEMPTS + " FROM " + TABLE_WEBHOOK_VERIFICATION
				+ " WHERE " + COL_WEBHOOK_VERIFICATION_WEBHOOK_ID + " = ?";

		try {
			jdbcTemplate.update(updateSql, webhookId);
			return jdbcTemplate.query(selectSql, (ResultSet rs, int rowNum) -> {
				return rs.getLong(COL_WEBHOOK_VERIFICATION_ATTEMPTS);
			}, webhookId).get(0);
		} catch (EmptyResultDataAccessException | NotFoundException e) {
			throw new NotFoundException("WebhookVerification for Webhook of ID: " + webhookId + " not found.", e);
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_WEBHOOK_VERIFICATION + " WHERE " + COL_WEBHOOK_VERIFICATION_ID + " > -1");
	}

}
