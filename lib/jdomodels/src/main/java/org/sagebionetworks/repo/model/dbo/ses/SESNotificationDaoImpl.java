package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SES_NOTIFICATIONS;

import java.sql.Timestamp;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.ses.SESNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
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

	@Autowired
	public SESNotificationDaoImpl(IdGenerator idGenerator, DBOBasicDao basicDao, JdbcTemplate jdbcTemplate) {
		this.idGenerator = idGenerator;
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public SESNotification create(SESNotification notification) {
		validateDTO(notification);

		DBOSESNotification dbo = new DBOSESNotification();

		dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setSesEmailId(notification.getSesEmailId());
		dbo.setSesFeedbackId(notification.getSesFeedbackId());
		dbo.setMessageTimestamp(Timestamp.from(notification.getMessageTimestamp()));
		dbo.setIspTimestamp(Timestamp.from(notification.getIspTimestamp()));
		dbo.setNotificationType(notification.getNotificationType().toString());
		dbo.setNotificationBody(SESNotificationUtils.encodeBody(notification.getNotificationBody()));
		dbo.setId(idGenerator.generateNewId(IdType.SES_NOTIFICATION_ID));

		dbo = basicDao.createNew(dbo);

		return map(dbo);
	}

	private void validateDTO(SESNotification notification) {
		ValidateArgument.required(notification, "notification");
		ValidateArgument.required(notification.getSesEmailId(), "The SES Email Id");
		ValidateArgument.required(notification.getSesFeedbackId(), "The SES Feedback Id");
		ValidateArgument.required(notification.getMessageTimestamp(), "The message timestamp");
		ValidateArgument.required(notification.getIspTimestamp(), "The isp timestamp");
		ValidateArgument.required(notification.getNotificationType(), "The notification type");
		ValidateArgument.required(notification.getNotificationBody(), "The notification body");
	}

	private SESNotification map(DBOSESNotification dbo) {
		SESNotification dto = new SESNotification();

		dto.setId(dbo.getId());
		dto.setCreatedOn(dbo.getCreatedOn().toInstant());
		dto.setIspTimestamp(dbo.getIspTimestamp().toInstant());
		dto.setMessageTimestamp(dbo.getMessageTimestamp().toInstant());
		dto.setSesEmailId(dbo.getSesEmailId());
		dto.setSesFeedbackId(dbo.getSesFeedbackId());
		dto.setNotificationType(SESNotificationType.valueOf(dbo.getNotificationType()));
		dto.setNotificationBody(SESNotificationUtils.decodeBody(dbo.getNotificationBody()));

		return dto;
	}

	@Override
	public void clearAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_SES_NOTIFICATIONS);
	}

}
