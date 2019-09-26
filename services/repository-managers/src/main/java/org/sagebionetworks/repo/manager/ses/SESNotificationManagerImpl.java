package org.sagebionetworks.repo.manager.ses;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESJsonWithFeedbackId;
import org.sagebionetworks.repo.model.ses.SESNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SESNotificationManagerImpl implements SESNotificationManager {

	private static final Logger LOG = LogManager.getLogger(SESNotificationManagerImpl.class);

	private SESNotificationDao notificationDao;

	@Autowired
	public SESNotificationManagerImpl(SESNotificationDao notificationDao) {
		this.notificationDao = notificationDao;
	}

	@Override
	@WriteTransaction
	public void processNotification(SESJsonNotification notification) {
		ValidateArgument.required(notification, "The notification");
		ValidateArgument.required(notification.getNotificationBody(), "The notificaiton body");

		SESNotification dto = map(notification);
		
		notificationDao.create(dto);

	}

	SESNotification map(SESJsonNotification json) {
		SESNotification dto = new SESNotification();
		SESNotificationType type = SESNotificationType.UNKNOWN;

		String notificationTypeString = json.getNotificationType();
		
		if (!StringUtils.isBlank(notificationTypeString)) {
			try {
				notificationTypeString = notificationTypeString.toUpperCase();
				type = SESNotificationType.valueOf(notificationTypeString);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		dto.setNotificationType(type);
		dto.setNotificationBody(json.getNotificationBody());

		if (json.getMail() != null) {
			dto.setSesMessageId(json.getMail().getMessageId());
		}

		Optional<SESJsonWithFeedbackId> feedback = Optional.empty();

		switch (type) {
		case BOUNCE:
			feedback = Optional.ofNullable(json.getBounce());
			break;
		case COMPLAINT:
			feedback = Optional.ofNullable(json.getComplaint());
		default:
			break;
		}

		feedback.ifPresent(feedbackId -> {
			dto.setSesFeedbackId(feedbackId.getFeedbackId());
		});

		return dto;
	}

}
