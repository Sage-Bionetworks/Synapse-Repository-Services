package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_EVENT_TYPES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_INVOKE_ENDPOINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_IS_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookDaoImpl implements WebhookDao {

	private static final String MSG_DUPLICATE_OBJECT_ENDPOINT = "The same invokeEndpoint cannot be used for the same object.";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private DBOBasicDao dboBasicDao;
	
	private static final RowMapper<DBOWebhook> ROW_MAPPER = new DBOWebhook().getTableMapping();

	@Override
	@WriteTransaction
	public Webhook createWebhook(Long userId, CreateOrUpdateWebhookRequest request) {
		
		Instant now = Instant.now();
		
		DBOWebhook dbo = new DBOWebhook()
			.setId(idGenerator.generateNewId(IdType.WEBHOOK_ID))
			.setCreatedBy(userId)
			.setCreatedOn(Timestamp.from(now))
			.setModifiedOn(Timestamp.from(now))
			.setEtag(UUID.randomUUID().toString())
			.setObjectId(KeyFactory.stringToKey(request.getObjectId()))
			.setObjectType(request.getObjectType().name())
			.setEventTypes(WebhookUtils.eventsToJson(request.getEventTypes()))
			.setInvokeEndpoint(request.getInvokeEndpoint())
			.setIsEnabled(request.getIsEnabled())
			.setVerificationStatus(WebhookVerificationStatus.PENDING.name())
			.setVerificationMessage(null);
		
		try {
			dbo = dboBasicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			// DBOBasicDao.createNew wraps any DataIntegrityViolationException into an IllegalArgumentException
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException(MSG_DUPLICATE_OBJECT_ENDPOINT);
			}
			throw e;
		}
		
		String id = dbo.getId().toString();
		
		return getWebhook(id).orElseThrow(() -> new IllegalStateException("A webhook with id " + id + " does not exist."));
	}

	@Override
	public Optional<Webhook> getWebhook(String webhookId) {
		return dboBasicDao.getObjectByPrimaryKey(DBOWebhook.class, new SinglePrimaryKeySqlParameterSource(webhookId))
			.map(WebhookUtils::translateDboToDto);
	}

	@Override
	@WriteTransaction
	public Webhook updateWebhook(String webhookId, CreateOrUpdateWebhookRequest request) {
		String sql = "UPDATE " + TABLE_WEBHOOK + " SET " 
			+ COL_WEBHOOK_ETAG + "=UUID(),"
			+ COL_WEBHOOK_MODIFIED_ON + "=NOW(),"
			+ COL_WEBHOOK_OBJECT_ID + "=?,"
			+ COL_WEBHOOK_OBJECT_TYPE + "=?,"
			+ COL_WEBHOOK_EVENT_TYPES + "=?,"
			+ COL_WEBHOOK_IS_ENABLED + "=?,"
			+ COL_WEBHOOK_INVOKE_ENDPOINT + "=? "
			+ " WHERE " + COL_WEBHOOK_ID + "=?";

		try {
			jdbcTemplate.update(sql, 
				request.getObjectId(), 
				request.getObjectType().name(), 
				WebhookUtils.eventsToJson(request.getEventTypes()), 
				request.getIsEnabled(), 
				request.getInvokeEndpoint(),
				webhookId
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(MSG_DUPLICATE_OBJECT_ENDPOINT);
		}
		
		return getWebhook(webhookId).orElseThrow(() -> new IllegalStateException("A webhook with id " + webhookId + " does not exist."));
	}

	@Override
	@WriteTransaction
	public void deleteWebhook(String webhookId) {
		dboBasicDao.deleteObjectByPrimaryKey(DBOWebhook.class, new SinglePrimaryKeySqlParameterSource(webhookId));
	}

	@Override
	public List<Webhook> listUserWebhooks(Long userId, long limit, long offset) {
		String sql = "SELECT * FROM " + TABLE_WEBHOOK + " WHERE " + COL_WEBHOOK_CREATED_BY + "=? ORDER BY " + COL_WEBHOOK_CREATED_ON + " LIMIT ? OFFSET ?";
		
		return jdbcTemplate.query(sql, ROW_MAPPER, userId, limit, offset).stream()
			.map(WebhookUtils::translateDboToDto)
			.collect(Collectors.toList());
	}	
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_WEBHOOK + " WHERE " + COL_WEBHOOK_ID + " > -1");
	}

}
