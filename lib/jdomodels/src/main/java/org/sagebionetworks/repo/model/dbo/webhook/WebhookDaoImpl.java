package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_INVOKE_ENDPOINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_AUTHENTICATION_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_WEBHOOK_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_VERIFICATION;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationState;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookDaoImpl implements WebhookDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private DBOBasicDao dboBasicDao;

	public static final String WEBHOOK_UID_FK = "WEBHOOK_UID_FK";
	public static final String WEBHOOK_UID_FK_MESSAGE = "Cannot create a Webhook as there is no corresponding user.";

	static final RowMapper<Webhook> WEBHOOK_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
		return new Webhook().setWebhookId(rs.getString(COL_WEBHOOK_ID)).setObjectId(rs.getString(COL_WEBHOOK_OBJECT_ID))
				.setObjectType(WebhookObjectType.valueOf(rs.getString(COL_WEBHOOK_OBJECT_TYPE)))
				.setUserId(rs.getString(COL_WEBHOOK_USER_ID))
				.setInvokeEndpoint(rs.getString(COL_WEBHOOK_INVOKE_ENDPOINT))
				.setIsVerified(
						WebhookVerificationState.VERIFIED.name().equals(rs.getString(COL_WEBHOOK_VERIFICATION_STATE)))
				.setIsWebhookEnabled(rs.getBoolean(COL_WEBHOOK_IS_WEBHOOK_ENABLED))
				.setIsAuthenticationEnabled(rs.getBoolean(COL_WEBHOOK_IS_AUTHENTICATION_ENABLED))
				.setEtag(rs.getString(COL_WEBHOOK_ETAG)).setCreatedBy(rs.getString(COL_WEBHOOK_CREATED_BY))
				.setModifiedBy(rs.getString(COL_WEBHOOK_MODIFIED_BY))
				.setCreatedOn(rs.getTimestamp(COL_WEBHOOK_CREATED_ON))
				.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_MODIFIED_ON));
	};

	// Use LEFT JOIN to handle the case where a WebhookVerification hasn't been
	// created yet.
	// In this case, we want to represent the state as not yet verified, rather than
	// throw a NotFoundException.
	private static final String SELECT_LEFT_JOIN_SQL = "SELECT W.*, V." + COL_WEBHOOK_VERIFICATION_STATE + " FROM "
			+ TABLE_WEBHOOK + " W LEFT JOIN " + TABLE_WEBHOOK_VERIFICATION + " V ON (W." + COL_WEBHOOK_ID + " = V."
			+ COL_WEBHOOK_VERIFICATION_WEBHOOK_ID + ") ";

	@WriteTransaction
	@Override
	public Webhook createWebhook(Webhook dto) {
		DBOWebhook dbo = WebhookUtils.translateWebhookToDBOWebhook(dto);
		dbo.setId(idGenerator.generateNewId(IdType.WEBHOOK_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		Timestamp now = new Timestamp(System.currentTimeMillis());
		dbo.setCreatedOn(now);
		dbo.setModifiedOn(now);

		try {
			dboBasicDao.createNew(dbo);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("A Webhook with the objectId: " + dbo.getObjectId() + ", objectType: "
					+ dbo.getObjectType() + ", and invokeEndpoint: " + dbo.getInvokeEndpoint() + " already exists.", e);
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage().contains(WEBHOOK_UID_FK)) {
				throw new NotFoundException(WEBHOOK_UID_FK_MESSAGE);
			} else {
				throw e;
			}
		}

		return getWebhook(String.valueOf(dbo.getId()));
	}

	@Override
	public Webhook getWebhook(String webhookId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_LEFT_JOIN_SQL + "WHERE W." + COL_WEBHOOK_ID + " = ?",
					WEBHOOK_ROW_MAPPER, webhookId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Webhook of ID: " + webhookId + " not found.", e);
		}
	}

	@Override
	public Webhook getWebhookForUpdate(String webhookId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_LEFT_JOIN_SQL + "WHERE W." + COL_WEBHOOK_ID + " = ? FOR UPDATE",
					WEBHOOK_ROW_MAPPER, webhookId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Webhook of ID: " + webhookId + " not found.", e);
		}
	}

	@WriteTransaction
	@Override
	public Webhook updateWebhook(Webhook dto) {
		DBOWebhook dbo = WebhookUtils.translateWebhookToDBOWebhook(dto);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setModifiedOn(new Timestamp(System.currentTimeMillis()));

		try {
			dboBasicDao.createOrUpdate(dbo);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("A Webhook with the objectId: " + dto.getObjectId() + ", objectType: "
					+ dto.getObjectType() + ", and invokeEndpoint: " + dto.getInvokeEndpoint() + " already exists.", e);
		} catch (DataIntegrityViolationException e) {
			if (e.getMessage().contains(WEBHOOK_UID_FK)) {
				throw new NotFoundException(WEBHOOK_UID_FK_MESSAGE);
			} else {
				throw e;
			}
		}

		return getWebhook(dto.getWebhookId());
	}

	@WriteTransaction
	@Override
	public void deleteWebhook(String webhookId) {
		String deleteSql = "DELETE FROM " + TABLE_WEBHOOK + " WHERE " + COL_WEBHOOK_ID + " = ?";

		int rowsAffected = jdbcTemplate.update(deleteSql, webhookId);
		if (rowsAffected == 0) {
			throw new NotFoundException("Webhook of ID: " + webhookId + " not found.");
		}
	}

	@Override
	public List<Webhook> listUserWebhooks(Long userId, long limit, long offset) {
		String selectSql = SELECT_LEFT_JOIN_SQL + "WHERE " + COL_WEBHOOK_USER_ID + " = ? ORDER BY "
				+ COL_WEBHOOK_CREATED_ON + " LIMIT ? OFFSET ?";

		return jdbcTemplate.query(selectSql, WEBHOOK_ROW_MAPPER, userId, limit, offset);
	}

	@Override
	public List<Webhook> listVerifiedAndEnabledWebhooksForObject(String objectId, ObjectType objectType) {
		String selectSql = SELECT_LEFT_JOIN_SQL + "WHERE " + COL_WEBHOOK_OBJECT_ID + " = ? AND "
				+ COL_WEBHOOK_OBJECT_TYPE + " = ? AND " + COL_WEBHOOK_VERIFICATION_STATE + " = '"
				+ WebhookVerificationState.VERIFIED + "' AND " + COL_WEBHOOK_IS_WEBHOOK_ENABLED + " = TRUE ORDER BY "
				+ COL_WEBHOOK_ID;

		return jdbcTemplate.query(selectSql, WEBHOOK_ROW_MAPPER, objectId, objectType);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_WEBHOOK + " WHERE " + COL_WEBHOOK_ID + " > -1");
	}

}
