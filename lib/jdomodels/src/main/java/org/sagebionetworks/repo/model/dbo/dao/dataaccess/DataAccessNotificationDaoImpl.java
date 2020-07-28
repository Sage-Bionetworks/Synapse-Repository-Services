package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessNotificationDaoImpl implements DataAccessNotificationDao {

	private static final String SQL_FIND = "SELECT * FROM " + SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION + " WHERE "
			+ SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?" + " AND "
			+ SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = ?" + " AND "
			+ SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " = ?";
	
	// We want all the access approvals for submitters (submitter == accessor) whose expiration date is in a 
	// given range (e.g. on a specific day) for which a notification of a given type WAS NOT sent in within 
	// given range (e.g. on a specific day)
	//
	// For example: If today I'm trying to process all the approvals that expire 30 days from now, I want to get back
	// the list of approvals whose expiration date is 30 days (today + 30) excluding those approvals that have a notification
	// of the same type that was sent today.
	//
	// WITH EXPIRING_APPROVALS AS (
	// 	SELECT ID, ROW_NUMBER() OVER (PARTITION BY REQUIREMENT_ID, SUBMITTER_ID ORDER BY EXPIRED_ON DESC) AS APPROVAL_N 
	// 	FROM ACCESS_APPROVAL WHERE EXPIRED_ON >= ? AND EXPIRED_ON < ? AND STATE = 'APPROVED' AND SUBMITTER_ID = ACCESSOR_ID
	// ) 
	// SELECT DISTINCT(A.ID) FROM EXPIRING_APPROVALS A LEFT JOIN DATA_ACCESS_NOTIFICATION N 
	// ON ( A.ID = N.ACCESS_APPROVAL_ID AND N.NOTIFICATION_TYPE = ? AND N.SENT_ON >= ? AND N.SENT_ON < ?) 
	// WHERE A.APPROVAL_N = 1 AND N.ID IS NULL LIMIT ?
	private static final String SQL_SELECT_APPROVALS_FOR_UNSENT_SUBMITTER = "WITH EXPIRING_APPROVALS AS ("
			+ " SELECT " + SqlConstants.COL_ACCESS_APPROVAL_ID + ","
			// Provide a ranking over requirement and submitter so that only the most relevant approval is taken into account
			+ " ROW_NUMBER() OVER (PARTITION BY " + SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID + ", " + SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID
			+ " ORDER BY " + SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON + " DESC) AS APPROVAL_N"
			+ " FROM " + SqlConstants.TABLE_ACCESS_APPROVAL
			// Filter by the approval expiration date
			+ " WHERE " + SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON + " >= ?"
			+ " AND " + SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON + " < ?"
			+ " AND " + SqlConstants.COL_ACCESS_APPROVAL_STATE + " = '" + ApprovalState.APPROVED + "'"
			// Only submitters
			+ " AND " + SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID + " = " + SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID
			+ ")"
			+ " SELECT DISTINCT(A." + SqlConstants.COL_ACCESS_APPROVAL_ID + ") FROM EXPIRING_APPROVALS A"
			// Left outer join on the approval id, plus the type and range of the notification (this allows to filter notifications from the join)
			+ " LEFT JOIN " + SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION + " N"
			+ " ON ("
			+ " A." + SqlConstants.COL_ACCESS_APPROVAL_ID + " = N." + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID
			+ " AND N." + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?"
			+ " AND N." + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON + " >= ?"
			+ " AND N." + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON + " < ?"
			+ ") WHERE"
			// Take only the last approval of the day for the same requirement and submitter
			+ " A.APPROVAL_N = 1"
			// Only include the approvals that do not match (do not have such a notification)
			+ " AND N." + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ID + " IS NULL"
			+ " LIMIT ?";

	private static final RowMapper<DBODataAccessNotification> ROW_MAPPER = new DBODataAccessNotification()
			.getTableMapping()::mapRow;

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;
	private DBOBasicDao basicDao;

	@Autowired
	public DataAccessNotificationDaoImpl(final IdGenerator idGenerator, final JdbcTemplate jdbcTemplate,
			final DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
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
	public List<Long> listSubmmiterApprovalsForUnSentReminder(DataAccessNotificationType notificationType, LocalDate sentOn,
			int limit) {		
		final LocalDate expirationDate = sentOn.plus(notificationType.getReminderPeriod());
		
		final Instant startOfExpiration = expirationDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant endOfExpiration = expirationDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant startOfSentOn = sentOn.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant endOfSentOn = sentOn.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		
		return jdbcTemplate.queryForList(SQL_SELECT_APPROVALS_FOR_UNSENT_SUBMITTER, Long.class, 
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
	public void clear() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_DATA_ACCESS_NOTIFICATION);
	}
}
