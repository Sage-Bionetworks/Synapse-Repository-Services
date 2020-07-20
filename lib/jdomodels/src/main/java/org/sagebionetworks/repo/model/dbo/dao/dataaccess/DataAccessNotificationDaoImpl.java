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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessNotificationDaoImpl implements DataAccessNotificationDao {
	
	private static final String SQL_SELECT_TEMPLATE = "SELECT %s "
			+ " FROM " + TABLE_DATA_ACCESS_NOTIFICATION
			+ " WHERE " + COL_DATA_ACCESS_NOTIFICATION_TYPE + " = ?"
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID + " = ?";
	
	private static final String SQL_SELECT_SENT_ON = String.format(SQL_SELECT_TEMPLATE, COL_DATA_ACCESS_NOTIFICATION_SENT_ON);
	
	private static final String SQL_SELECT_ETAG = String.format(SQL_SELECT_TEMPLATE, COL_DATA_ACCESS_NOTIFICATION_ETAG);
	
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
	private DBOBasicDao basicDao;
	
	@Autowired
	public DataAccessNotificationDaoImpl(final IdGenerator idGenerator, final JdbcTemplate jdbcTemplate, final DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.basicDao = basicDao;
	}

	@Override
	@WriteTransaction
	public void registerNotification(DataAccessNotificationType type, Long requirementId, Long recipientId,
			Long accessApprovalId, Long messageId, Instant sentOn) {
		
		Long id = idGenerator.generateNewId(IdType.DATA_ACCESS_NOTIFICATION_ID);
		Timestamp sentTimestamp = Timestamp.from(sentOn);
		
		jdbcTemplate.update(SQL_INSERT_UPDATE, (ps) -> {
			int index = 0;
			
			// On create
			ps.setLong(++index, id);
			ps.setString(++index, type.name());
			ps.setLong(++index, requirementId);
			ps.setLong(++index, recipientId);
			ps.setLong(++index, accessApprovalId);
			ps.setLong(++index, messageId);
			ps.setTimestamp(++index, sentTimestamp);

			// On update
			ps.setLong(++index, accessApprovalId);
			ps.setLong(++index, messageId);
			ps.setTimestamp(++index, sentTimestamp);
		});
	}

	@Override
	public Optional<Instant> getSentOn(DataAccessNotificationType type, Long requirementId, Long recipientId) {
		try {
			Timestamp sentOnTimestamp = jdbcTemplate.queryForObject(SQL_SELECT_SENT_ON, Timestamp.class, type.name(), requirementId, recipientId);
			return Optional.of(sentOnTimestamp.toInstant());
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> getEtag(DataAccessNotificationType type, Long requirementId, Long recipientId) {
		try {
			String etag = jdbcTemplate.queryForObject(SQL_SELECT_ETAG, String.class, type.name(), requirementId, recipientId);
			return Optional.of(etag);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void clear() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_DATA_ACCESS_NOTIFICATION);
	}
}
