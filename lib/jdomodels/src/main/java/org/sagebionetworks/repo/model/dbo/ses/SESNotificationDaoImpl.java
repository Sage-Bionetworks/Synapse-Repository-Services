package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

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
		dbo.setSesMessageId(notification.getSesMessageId());
		dbo.setSesFeedbackId(notification.getSesFeedbackId());
		dbo.setNotificationType(notification.getNotificationType().toString());
		dbo.setNotificationBody(SESNotificationUtils.encodeBody(notification.getNotificationBody()));
		dbo.setId(idGenerator.generateNewId(IdType.SES_NOTIFICATION_ID));

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

	private void validateDTO(SESNotification notification) {
		ValidateArgument.required(notification, "notification");
		ValidateArgument.required(notification.getNotificationType(), "The notification type");
		ValidateArgument.required(notification.getNotificationBody(), "The notification body");
	}

	private SESNotification map(DBOSESNotification dbo) {
		SESNotification dto = new SESNotification();

		dto.setId(dbo.getId());
		dto.setCreatedOn(dbo.getCreatedOn().toInstant());
		dto.setSesMessageId(dbo.getSesMessageId());
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
