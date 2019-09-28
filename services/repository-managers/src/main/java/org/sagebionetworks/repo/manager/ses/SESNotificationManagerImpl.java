package org.sagebionetworks.repo.manager.ses;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
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
	public void processNotification(String notificationBody) {
		ValidateArgument.requiredNotBlank(notificationBody, "The notification body");
		
		SESJsonNotification notification;
		
		try {
			notification = SESNotificationUtils.parseNotification(notificationBody);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		SESNotification dto = map(notification);
		
		// Makes sure we pass in the original notification body
		dto.setNotificationBody(notificationBody);
		
		notificationDao.saveNotification(dto);

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

		if (json.getMail() != null) {
			dto.setSesMessageId(json.getMail().getMessageId());
		}

		Optional<SESJsonNotificationDetails> details = Optional.empty();

		switch (type) {
		case BOUNCE:
			details = Optional.ofNullable(json.getBounce());
			break;
		case COMPLAINT:
			details = Optional.ofNullable(json.getComplaint());
		default:
			break;
		}

		details.ifPresent(info -> {
			dto.setSesFeedbackId(info.getFeedbackId());
			dto.setNotificationSubType(info.getSubType().orElse(null));
			dto.setNotificationReason(info.getReason().orElse(null));
		});

		return dto;
	}

}
