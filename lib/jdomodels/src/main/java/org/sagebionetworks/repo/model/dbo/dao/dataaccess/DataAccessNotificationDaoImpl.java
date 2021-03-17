package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOAccessApprovalDAOImpl;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessNotificationDaoImpl implements DataAccessNotificationDao {

	private static final String SQL_FIND = "SELECT * FROM " + TABLE_DATA_ACCESS_NOTIFICATION
			+ " WHERE " + COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?" 
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = ?" 
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " = ?";
	
	private static final String SQL_SELECT_FOR_RECIPIENTS = "SELECT * FROM " + TABLE_DATA_ACCESS_NOTIFICATION
			+ " WHERE " + COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = :" + COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID 
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " IN (:" + COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + ")"
			+ " ORDER BY " + COL_DATA_ACCESS_NOTIFICATION_SENT_ON;
	
	// We want all the access approvals for submitters (submitter == accessor) whose expiration date is in a 
	// given range (e.g. on a specific day) for which a notification of a given type WAS NOT sent within a
	// given range (e.g. on a specific day). Additionally we need to make sure that we do not include approvals
	// for which other approval exist with the same requirement and submitter that do not expire (expiredOn = 0)
	// or that expire in the future.
	//
	// For example: If today I'm trying to process all the approvals that expire 30 days from now, I want to get back
	// the list of approvals whose expiration date is 30 days (today + 30) excluding those approvals that have a notification
	// of the same type that was sent today.
	
	/*
	 WITH EXPIRING_APPROVALS AS (
	 		SELECT * FROM ACCESS_APPROVAL 
			WHERE EXPIRED_ON >= ? AND EXPIRED_ON < ? 
	 		AND STATE = 'APPROVED' AND SUBMITTER_ID = ACCESSOR_ID
	 ),
	 EXPIRED_APPROVALS AS (
	 		SELECT E.* FROM EXPIRING_APPROVALS E LEFT JOIN ACCESS_APPROVAL A ON (
				E.REQUIREMENT_ID = A.REQUIREMENT_ID 
				AND E.SUBMITTER_ID = A.SUBMITTER_ID 
				AND E.ACCESSOR_ID = A.ACCESSOR_ID 
				AND E.STATE = A.STATE 
				AND (A.EXPIRED_ON = 0 OR A.EXPIRED_ON > E.EXPIRED_ON) 
			) WHERE A.ID IS NULL
	 )
	 SELECT DISTINCT(EXPIRED_APPROVALS.ID) FROM EXPIRED_APPROVALS LEFT JOIN DATA_ACCESS_NOTIFICATION N ON (
			EXPIRED_APPROVALS.ID = N.ACCESS_APPROVAL_ID 
			AND N.NOTIFICATION_TYPE = ? 
			AND N.SENT_ON >= ? AND N.SENT_ON < ?
	 ) WHERE N.ID IS NULL LIMIT ?
	*/
	private static final String SQL_SELECT_UNSENT_EXPIRING_SUBMMITER_APPROVALS = 
			// This is the set of approvals that expire in a given interval (e.g. in 30 days from now)
			"WITH EXPIRING_APPROVALS AS ("
			+ "SELECT * FROM " + TABLE_ACCESS_APPROVAL
			// Filter by the approval expiration date
			+ " WHERE " + COL_ACCESS_APPROVAL_EXPIRED_ON + " >= ?"
			+ " AND " + COL_ACCESS_APPROVAL_EXPIRED_ON + " < ?"
			// We are interested only in the approved ones
			+ " AND " + COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED + "'"
			// We are interested only in submitters
			+ " AND " + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = " + COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ "),"
			
			// Now we exclude from the set the approvals for which other approvals with the same requirement and submitter 
			// exist that do not expire (expiredOn is 0) or that expire in the future
			+ "EXPIRED_APPROVALS AS ("
			+ " SELECT E.* FROM EXPIRING_APPROVALS E LEFT JOIN " + TABLE_ACCESS_APPROVAL + " A"
			+ " ON ("
			+ " E." + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = A." + COL_ACCESS_APPROVAL_REQUIREMENT_ID 
			+ " AND E." + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = A." + COL_ACCESS_APPROVAL_SUBMITTER_ID 
			+ " AND E." + COL_ACCESS_APPROVAL_ACCESSOR_ID + " = A." + COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ " AND E." + COL_ACCESS_APPROVAL_STATE + " = A." + COL_ACCESS_APPROVAL_STATE
			+ " AND (A." + COL_ACCESS_APPROVAL_EXPIRED_ON + " = " + DBOAccessApprovalDAOImpl.DEFAULT_NOT_EXPIRED
			+ " OR A." + COL_ACCESS_APPROVAL_EXPIRED_ON + " > E." + COL_ACCESS_APPROVAL_EXPIRED_ON + ")"
			+ " ) WHERE A." + COL_ACCESS_APPROVAL_ID + " IS NULL"
			+ ")"
			
			// Now we can filter the approvals for which we sent a notification in the given sentOn range
			+ " SELECT DISTINCT(EXPIRED_APPROVALS." + COL_ACCESS_APPROVAL_ID + ") FROM EXPIRED_APPROVALS "
			// Left outer join on the approval id, plus the type and range of the notification (this allows to filter notifications from the join)
			+ " LEFT JOIN " + TABLE_DATA_ACCESS_NOTIFICATION + " N"
			+ " ON ("
			+ " EXPIRED_APPROVALS." + COL_ACCESS_APPROVAL_ID + " = N." + COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID
			+ " AND N." + COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?"
			+ " AND N." + COL_DATA_ACCESS_NOTIFICATION_SENT_ON + " >= ?"
			+ " AND N." + COL_DATA_ACCESS_NOTIFICATION_SENT_ON + " < ?"
			+ ") WHERE N." + COL_DATA_ACCESS_NOTIFICATION_ID + " IS NULL"
			+ " LIMIT ?";

	private static final RowMapper<DBODataAccessNotification> ROW_MAPPER = new DBODataAccessNotification()
			.getTableMapping()::mapRow;

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	private DBOBasicDao basicDao;

	@Autowired
	public DataAccessNotificationDaoImpl(
			final IdGenerator idGenerator, 
			final JdbcTemplate jdbcTemplate,
			final NamedParameterJdbcTemplate namedJdbcTemplate,
			final DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.basicDao = basicDao;
	}

	@Override
	@WriteTransaction
	public DBODataAccessNotification create(DBODataAccessNotification notification) {
		ValidateArgument.required(notification, "The notification");
		ValidateArgument.requirement(notification.getId() == null, "The id must be unassigned.");

		// Sets the new id
		notification.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_NOTIFICATION_ID));
		notification.setEtag(UUID.randomUUID().toString());

		validateForStorage(notification);

		return basicDao.createNew(notification);
	}

	@Override
	@WriteTransaction
	public void update(Long id, DBODataAccessNotification notification) {
		ValidateArgument.required(id, "The id");
		ValidateArgument.required(notification, "The notification");

		notification.setId(id);
		notification.setEtag(UUID.randomUUID().toString());

		validateForStorage(notification);

		basicDao.update(notification);
	}

	static void validateForStorage(DBODataAccessNotification notification) {
		ValidateArgument.required(notification.getId(), "The id");
		ValidateArgument.required(notification.getNotificationType(), "The notification type");
		ValidateArgument.required(notification.getRequirementId(), "The requirement id");
		ValidateArgument.required(notification.getRecipientId(), "The recipient id");
		ValidateArgument.required(notification.getAccessApprovalId(), "The approval id");
		ValidateArgument.required(notification.getMessageId(), "The message id");
		ValidateArgument.required(notification.getSentOn(), "The sent on");
	}

	@Override
	public Optional<DBODataAccessNotification> find(DataAccessNotificationType type, Long requirementId,
			Long recipientId) {
		return find(type, requirementId, recipientId, false);
	}

	@Override
	public Optional<DBODataAccessNotification> findForUpdate(DataAccessNotificationType type, Long requirementId,
			Long recipientId) {
		return find(type, requirementId, recipientId, true);
	}
	
	@Override
	public List<DBODataAccessNotification> listForRecipients(Long requirementId, List<Long> recipientIds) {
		if (recipientIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		
		params.addValue(COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID, requirementId);
		params.addValue(COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID, recipientIds);
	
		return namedJdbcTemplate.query(SQL_SELECT_FOR_RECIPIENTS, params, ROW_MAPPER);
	}
	
	@Override
	public List<Long> listSubmmiterApprovalsForUnSentReminder(DataAccessNotificationType notificationType, LocalDate sentOn,
			int limit) {		
		final LocalDate expirationDate = sentOn.plus(notificationType.getReminderPeriod());
		
		final Instant startOfExpiration = expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant endOfExpiration = expirationDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant startOfSentOn = sentOn.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant endOfSentOn = sentOn.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		
		return jdbcTemplate.queryForList(SQL_SELECT_UNSENT_EXPIRING_SUBMMITER_APPROVALS, Long.class, 
				startOfExpiration.toEpochMilli(),
				endOfExpiration.toEpochMilli(),
				notificationType.name(),
				Timestamp.from(startOfSentOn),
				Timestamp.from(endOfSentOn),
				limit);
	}

	private Optional<DBODataAccessNotification> find(DataAccessNotificationType type, Long requirementId,
			Long recipientId, boolean forUpdate) {
		StringBuilder sql = new StringBuilder(SQL_FIND);

		if (forUpdate) {
			sql.append(" FOR UPDATE");
		}

		try {
			return Optional.of(jdbcTemplate.queryForObject(sql.toString(), ROW_MAPPER, type.name(), requirementId, recipientId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_DATA_ACCESS_NOTIFICATION);
	}
}
