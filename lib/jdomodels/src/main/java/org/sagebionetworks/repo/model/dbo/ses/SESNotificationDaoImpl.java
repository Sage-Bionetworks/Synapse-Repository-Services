package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_SES_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SES_NOTIFICATIONS;

import java.sql.Timestamp;
import java.time.Instant;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.ses.SESNotificationRecord;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SESNotificationDaoImpl implements SESNotificationDao {

	private IdGenerator idGenerator;
	private DBOBasicDao basicDao;
	private JdbcTemplate jdbcTemplate;
	private int instanceNumber;

	@Autowired
	public SESNotificationDaoImpl(IdGenerator idGenerator, DBOBasicDao basicDao, JdbcTemplate jdbcTemplate, StackConfiguration config) {
		this.idGenerator = idGenerator;
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
		this.instanceNumber = config.getStackInstanceNumber();
	}

	@Override
	@WriteTransaction
	public SESNotificationRecord saveNotification(SESNotificationRecord notification) {
		ValidateArgument.required(notification, "The notification");

		DBOSESNotification dbo = map(notification);

		dbo = basicDao.createNew(dbo);

		return map(dbo);
	}

	@Override
	public Long countBySesMessageId(String sesMessageId) {
		ValidateArgument.required(sesMessageId, "The SES Message Id");

		String sql = "SELECT COUNT(" + COL_SES_NOTIFICATIONS_ID + ") FROM " + TABLE_SES_NOTIFICATIONS + " WHERE "
				+ COL_SES_NOTIFICATIONS_SES_MESSAGE_ID + " = ?";

		return jdbcTemplate.queryForObject(sql, Long.class, sesMessageId);
	}

	private DBOSESNotification map(SESNotificationRecord dto) {
		DBOSESNotification dbo = new DBOSESNotification();

		dbo.setInstanceNumber(instanceNumber);
		dbo.setCreatedOn(Timestamp.from(Instant.now()));
		dbo.setSesMessageId(dto.getSesMessageId());
		dbo.setSesFeedbackId(dto.getSesFeedbackId());
		dbo.setNotificationType(dto.getNotificationType().toString());
		dbo.setNotificationSubType(dto.getNotificationSubType());
		dbo.setNotificationReason(dto.getNotificationReason());
		dbo.setNotificationBody(dto.getNotificationBody());
		dbo.setId(idGenerator.generateNewId(IdType.SES_NOTIFICATION_ID));

		return dbo;
	}

	private SESNotificationRecord map(DBOSESNotification dbo) {
		SESNotificationType notificationType = SESNotificationType.valueOf(dbo.getNotificationType());
		
		return new SESNotificationRecord(notificationType, dbo.getNotificationBody())
				.withId(dbo.getId())
				.withInstanceNumber(dbo.getInstanceNumber())
				.withCreatedOn(dbo.getCreatedOn().toInstant())
				.withSesMessageId(dbo.getSesMessageId())
				.withSesFeedbackId(dbo.getSesFeedbackId())
				.withNotificationSubType(dbo.getNotificationSubType())
				.withNotificationReason(dbo.getNotificationReason());
	}

	@Override
	public void clearAll() {
		String sql = "DELETE FROM " + TABLE_SES_NOTIFICATIONS;
		jdbcTemplate.update(sql);
	}

}
