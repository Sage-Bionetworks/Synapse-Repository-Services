package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ALLOWED_DOMAIN_PATTERN;
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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MSG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_ALLOWED_DOMAIN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_VERIFICATION;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.json.JSONArray;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
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
	
	private static final RowMapper<Webhook> WEBHOOK_ROW_MAPPER = (rs, rowNum) -> new Webhook()
		.setId(rs.getString(COL_WEBHOOK_ID))
		.setCreatedBy(rs.getString(COL_WEBHOOK_CREATED_BY))
		.setCreatedOn(new Date(rs.getTimestamp(COL_WEBHOOK_CREATED_ON).getTime()))
		.setModifiedOn(new Date(rs.getTimestamp(COL_WEBHOOK_MODIFIED_ON).getTime()))
		.setObjectId(rs.getString(COL_WEBHOOK_OBJECT_ID))
		.setObjectType(SynapseObjectType.valueOf(rs.getString(COL_WEBHOOK_OBJECT_TYPE)))
		.setEventTypes(eventsFromJson(rs.getString(COL_WEBHOOK_EVENT_TYPES)))
		.setInvokeEndpoint(rs.getString(COL_WEBHOOK_INVOKE_ENDPOINT))
		.setIsEnabled(rs.getBoolean(COL_WEBHOOK_IS_ENABLED))
		.setVerificationStatus(statusFromString(rs.getString(COL_WEBHOOK_VERIFICATION_STATUS)))
		.setVerificationMsg(rs.getString(COL_WEBHOOK_VERIFICATION_MSG));
	
	static WebhookVerificationStatus statusFromString(String status) {
		if (status == null) {
			return WebhookVerificationStatus.PENDING;
		}
		return WebhookVerificationStatus.valueOf(status);
	}
	
	static String eventsToJson(Set<SynapseEventType> events) {
		return new JSONArray(events).toString();
	}
	
	static Set<SynapseEventType> eventsFromJson(String json) {
		JSONArray jsonArray = new JSONArray(json);
		
		Set<SynapseEventType> events = new TreeSet<>();
		
		jsonArray.forEach( element -> {
			events.add(SynapseEventType.valueOf((String) element));
		});
		
		return events;
	}
	
	static Long mapWebhookId(String webhookId) {
		try {
			return Long.valueOf(webhookId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid webhook id.");
		}
	}

	private static final String SELECT_WITH_STATUS = "SELECT W.*, V." + COL_WEBHOOK_VERIFICATION_STATUS + ", V." + COL_WEBHOOK_VERIFICATION_MSG 
			+ " FROM " + TABLE_WEBHOOK + " W JOIN " + TABLE_WEBHOOK_VERIFICATION + " V"
			+ " ON (W." + COL_WEBHOOK_ID + " = V." + COL_WEBHOOK_VERIFICATION_ID + ") ";

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
			.setEventTypes(eventsToJson(request.getEventTypes()))
			.setInvokeEndpoint(request.getInvokeEndpoint())
			.setIsEnabled(request.getIsEnabled());
		
		try {
			dbo = dboBasicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			// DBOBasicDao.createNew wraps any DataIntegrityViolationException into an IllegalArgumentException
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException(MSG_DUPLICATE_OBJECT_ENDPOINT);
			}
			throw e;
		}
		
		dboBasicDao.createNew(new DBOWebhookVerification()
			.setWebhookId(dbo.getId())
			.setModifiedOn(Timestamp.from(now))
			.setEtag(UUID.randomUUID().toString())
			.setStatus(WebhookVerificationStatus.PENDING.name())
		);
		
		return getWebhook(dbo.getId().toString(), false).orElseThrow(() -> new IllegalStateException("The webhook was not created."));
	}
	
	@Override
	public Optional<Webhook> getWebhook(String webhookId, boolean forUpdate) {
		String sql = SELECT_WITH_STATUS + " WHERE " + COL_WEBHOOK_ID + "=?" + (forUpdate ? " FOR UPDATE" : "");
		
		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, WEBHOOK_ROW_MAPPER, mapWebhookId(webhookId)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
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
				KeyFactory.stringToKey(request.getObjectId()), 
				request.getObjectType().name(), 
				eventsToJson(request.getEventTypes()), 
				request.getIsEnabled(), 
				request.getInvokeEndpoint(),
				mapWebhookId(webhookId)
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(MSG_DUPLICATE_OBJECT_ENDPOINT);
		}
		
		return getWebhook(webhookId, false).orElseThrow(() -> new IllegalStateException("The webhook was not updated."));
	}

	@Override
	@WriteTransaction
	public void deleteWebhook(String webhookId) {
		dboBasicDao.deleteObjectByPrimaryKey(DBOWebhook.class, new SinglePrimaryKeySqlParameterSource(mapWebhookId(webhookId)));
	}

	@Override
	public List<Webhook> listUserWebhooks(Long userId, long limit, long offset) {
		String sql = SELECT_WITH_STATUS + " WHERE " + COL_WEBHOOK_CREATED_BY + "=? ORDER BY " + COL_WEBHOOK_CREATED_ON + " LIMIT ? OFFSET ?";
		
		return jdbcTemplate.query(sql, WEBHOOK_ROW_MAPPER, userId, limit, offset);
	}
	
	@Override
	public List<Webhook> listWebhooksForObjectIds(List<Long> ids, SynapseObjectType objectType, SynapseEventType eventType, long limit, long offset) {
		
		String sql = SELECT_WITH_STATUS 
			+ " WHERE " + COL_WEBHOOK_OBJECT_ID + " IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")"
			+ " AND " + COL_WEBHOOK_OBJECT_TYPE + "=?"
			+ " AND '" + eventType.name() + "' MEMBER OF(" + COL_WEBHOOK_EVENT_TYPES + ")"
			+ " AND " + COL_WEBHOOK_IS_ENABLED + " IS TRUE"
			+ " AND " + COL_WEBHOOK_VERIFICATION_STATUS + "=?"
			+ " ORDER BY " + COL_WEBHOOK_ID
			+ " LIMIT ? OFFSET ?";
		
		List<Object> queryParams = new ArrayList<>();
		
		queryParams.addAll(ids);
		queryParams.add(objectType.name());
		queryParams.add(WebhookVerificationStatus.VERIFIED.name());
		queryParams.add(limit);
		queryParams.add(offset);

		return jdbcTemplate.query(sql, WEBHOOK_ROW_MAPPER, queryParams.toArray());
	}
	
	@Override
	public DBOWebhookVerification getWebhookVerification(String webhookId) {
		return dboBasicDao.getObjectByPrimaryKey(DBOWebhookVerification.class, new SinglePrimaryKeySqlParameterSource(mapWebhookId(webhookId)))
				.orElseThrow(() -> new IllegalStateException("A webhook with id " + webhookId + " does not exist."));
	}
	
	@Override
	@WriteTransaction
	public DBOWebhookVerification setWebhookVerificationCode(String webhookId, String verificationCode, Instant expiresOn) {
		String sql = "UPDATE " + TABLE_WEBHOOK_VERIFICATION + " SET " 
			+ COL_WEBHOOK_VERIFICATION_ETAG + "=UUID(),"
			+ COL_WEBHOOK_VERIFICATION_MODIFIED_ON + "=NOW(),"
			+ COL_WEBHOOK_VERIFICATION_CODE + "=?,"
			+ COL_WEBHOOK_VERIFICATION_CODE_EXPIRES_ON + "=?,"
			+ COL_WEBHOOK_VERIFICATION_CODE_MESSAGE_ID + "=UUID(),"
			// Reset other data
			+ COL_WEBHOOK_VERIFICATION_STATUS + "='PENDING',"
			+ COL_WEBHOOK_VERIFICATION_MSG + "=NULL"
			+ " WHERE " + COL_WEBHOOK_VERIFICATION_ID + "=?";
		
		jdbcTemplate.update(sql, verificationCode, expiresOn, mapWebhookId(webhookId));
		
		return getWebhookVerification(webhookId);
	}
	
	@Override
	@WriteTransaction
	public DBOWebhookVerification setWebhookVerificationStatus(String webhookId, WebhookVerificationStatus status, String message) {
	
		String sql = "UPDATE " + TABLE_WEBHOOK_VERIFICATION + " SET " 
			+ COL_WEBHOOK_VERIFICATION_ETAG + "=UUID(),"
			+ COL_WEBHOOK_VERIFICATION_MODIFIED_ON + "=NOW(),"
			+ COL_WEBHOOK_VERIFICATION_STATUS + "=?,"
			+ COL_WEBHOOK_VERIFICATION_MSG + "=?"
			+ " WHERE " + COL_WEBHOOK_VERIFICATION_ID + "=?";
			
		jdbcTemplate.update(sql, status.name(), message, mapWebhookId(webhookId));
		
		return getWebhookVerification(webhookId);
	}
	
	@Override
	public Optional<WebhookVerificationStatus> getWebhookVerificationStatus(String webhookId, String messageId) {
		String sql = "SELECT " + COL_WEBHOOK_VERIFICATION_STATUS 
			+ " FROM " + TABLE_WEBHOOK_VERIFICATION + " WHERE " 
				+ COL_WEBHOOK_VERIFICATION_ID + "=? AND "
				+ COL_WEBHOOK_VERIFICATION_CODE_MESSAGE_ID + "=?";
		
		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, WebhookVerificationStatus.class, mapWebhookId(webhookId), messageId));
		} catch (EmptyResultDataAccessException e) {			
			return Optional.empty();
		}
		
	}
	
	@Override
	@WriteTransaction
	public void setWebhookVerificationStatusIfMessageIdMatch(String webhookId, String messageId, WebhookVerificationStatus status, String message) {
	
		String sql = "UPDATE " + TABLE_WEBHOOK_VERIFICATION + " SET " 
			+ COL_WEBHOOK_VERIFICATION_ETAG + "=UUID(),"
			+ COL_WEBHOOK_VERIFICATION_MODIFIED_ON + "=NOW(),"
			+ COL_WEBHOOK_VERIFICATION_STATUS + "=?,"
			+ COL_WEBHOOK_VERIFICATION_MSG + "=?"
			+ " WHERE " + COL_WEBHOOK_VERIFICATION_ID + "=? AND "
			+ COL_WEBHOOK_VERIFICATION_CODE_MESSAGE_ID + "=?";		
			
		jdbcTemplate.update(sql, status.name(), message, mapWebhookId(webhookId), messageId);
	}
	
	@Override
	public List<String> getAllowedDomainsPatterns() {
		return jdbcTemplate.queryForList("SELECT " + COL_WEBHOOK_ALLOWED_DOMAIN_PATTERN + " FROM " + TABLE_WEBHOOK_ALLOWED_DOMAIN, String.class);
	}
	
	@Override
	@WriteTransaction
	public DBOWebhookAllowedDomain addAllowedDomainPattern(String pattern) {
		DBOWebhookAllowedDomain dbo = new DBOWebhookAllowedDomain()
			.setId(idGenerator.generateNewId(IdType.WEBHOOK_ALLOWED_DOMAIN_ID))
			.setEtag(UUID.randomUUID().toString())
			.setPattern(pattern);
		
		return dboBasicDao.createNew(dbo);
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_WEBHOOK + " WHERE " + COL_WEBHOOK_ID + " > -1");
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_WEBHOOK_ALLOWED_DOMAIN);
	}

}
