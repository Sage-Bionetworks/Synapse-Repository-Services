package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION;

import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
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
	
	private static final String SQL_FIND = "SELECT * FROM " + SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION
			+ " WHERE " + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?"
			+ " AND " + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = ?"
			+ " AND " + SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " = ?"
			+ " FOR UPDATE";
	
	private static final RowMapper<DBODataAccessNotification> ROW_MAPPER = new DBODataAccessNotification().getTableMapping()::mapRow;

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;
	private DBOBasicDao basicDao;
	
	@Autowired
	public DataAccessNotificationDaoImpl(final IdGenerator idGenerator, final JdbcTemplate jdbcTemplate, final DBOBasicDao basicDao) {
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
	public Optional<DBODataAccessNotification> findForUpdate(DataAccessNotificationType type, Long requirementId,
			Long recipientId) {
		try {
			return Optional.of(jdbcTemplate.queryForObject(SQL_FIND, ROW_MAPPER, type.name(), requirementId, recipientId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void clear() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_DATA_ACCESS_NOTIFICATION);
	}
}
