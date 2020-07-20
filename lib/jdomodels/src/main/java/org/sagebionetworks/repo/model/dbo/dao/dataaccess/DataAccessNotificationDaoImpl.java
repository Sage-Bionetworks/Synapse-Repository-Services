package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessNotificationDaoImpl implements DataAccessNotificationDao {
	
	private static final String SQL_SELECT_SENT_ON = "SELECT " + COL_DATA_ACCESS_NOTIFICATION_SENT_ON 
			+ " FROM " + TABLE_DATA_ACCESS_NOTIFICATION
			+ " WHERE " + COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?"
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " = ?";
	
	private static final String SQL_INSERT_UPDATE = "INSERT INTO " + TABLE_DATA_ACCESS_NOTIFICATION
			+ " ("
			+ COL_DATA_ACCESS_NOTIFICATION_ID + ","
			+ COL_DATA_ACCESS_NOTIFICATION_ETAG + ","
			+ COL_DATA_ACCESS_NOTIFICATION_TYPE + ","
			+ COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + ","
			+ COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + ","
			+ COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID + "," 
			+ COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID + ","
			+ COL_DATA_ACCESS_NOTIFICATION_SENT_ON
			+ " ) VALUES (?, UUID(), ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
			+ COL_DATA_ACCESS_NOTIFICATION_ETAG + " = UUID(),"
			+ COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID + " = ?,"
			+ COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID + " = ?,"
			+ COL_DATA_ACCESS_NOTIFICATION_SENT_ON + " = ?";

	private IdGenerator idGenerator;
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	public DataAccessNotificationDaoImpl(final IdGenerator idGenerator, final JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public void registerNotification(DataAccessNotificationType type, Long requirementId, Long recipientId,
			Long accessApprovalId, Long messageId, Instant sentOn) {
		
		Long newId = idGenerator.generateNewId(IdType.DATA_ACCESS_NOTIFICATION_ID);
		
		jdbcTemplate.update(SQL_INSERT_UPDATE, (ps) -> {
			int index = 0;
			
			// On create
			ps.setLong(++index, newId);
			ps.setString(++index, type.name());
			ps.setLong(++index, requirementId);
			ps.setLong(++index, recipientId);
			ps.setLong(++index, accessApprovalId);
			ps.setLong(++index, messageId);
			ps.setObject(++index, sentOn);

			// On update
			ps.setLong(++index, accessApprovalId);
			ps.setLong(++index, messageId);
			ps.setObject(++index, sentOn);
		});
		
	}

	@Override
	public Optional<Instant> getSentOn(DataAccessNotificationType type, Long requirementId, Long recipientId) {
		try {
			Long sentOn = jdbcTemplate.queryForObject(SQL_SELECT_SENT_ON, Long.class, type.name(), requirementId, recipientId);
			return Optional.of(new Date(sentOn).toInstant());
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

}
