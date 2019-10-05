package org.sagebionetworks.repo.manager.ses;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationRecord;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class SESNotificationManagerImpl implements SESNotificationManager {

	private static final Logger LOG = LogManager.getLogger(SESNotificationManagerImpl.class);

	// Set of types of notifications for which no quarantine is possible
	private static final Set<SESNotificationType> NO_QUARANTINE_SET = ImmutableSet.of(SESNotificationType.DELIVERY,
			SESNotificationType.UNKNOWN);

	private SESNotificationDao notificationDao;
	private EmailQuarantineDao quarantineDao;
	private Map<SESNotificationType, EmailQuarantineProvider> quarantineProviderMap;

	@Autowired
	public SESNotificationManagerImpl(SESNotificationDao notificationDao, EmailQuarantineDao quarantineDao,
			List<EmailQuarantineProvider> quarantineProviders) {
		this.notificationDao = notificationDao;
		this.quarantineDao = quarantineDao;
		this.quarantineProviderMap = initProviders(quarantineProviders);
	}

	private Map<SESNotificationType, EmailQuarantineProvider> initProviders(List<EmailQuarantineProvider> quarantineProviders) {
		Map<SESNotificationType, EmailQuarantineProvider> providerMap = new HashMap<>(quarantineProviders.size());

		quarantineProviders.forEach(provider -> {
			providerMap.put(provider.getSupportedType(), provider);
		});

		return providerMap;
	}

	@Override
	@WriteTransaction
	public void processMessage(String messageBody) {
		ValidateArgument.requiredNotBlank(messageBody, "The message body");

		SESJsonNotification notification;

		try {
			notification = SESNotificationUtils.parseSQSMessage(messageBody);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		SESNotificationType notificationType = parseNotificationType(notification.getNotificationType());

		// Makes sure we pass in the original message body
		SESNotificationRecord dto = new SESNotificationRecord(notificationType, messageBody);

		if (notification.getMail() != null) {
			dto.withSesMessageId(notification.getMail().getMessageId());
		}

		Optional<SESJsonNotificationDetails> details = getNotificationDetails(dto.getNotificationType(), notification);

		// Fill in additional details about the notification
		details.ifPresent(info -> {
			dto.withSesFeedbackId(info.getFeedbackId());
			dto.withNotificationSubType(info.getSubType().orElse(null));
			dto.withNotificationReason(info.getReason().orElse(null));
		});

		// Save the notification to the database
		notificationDao.saveNotification(dto);

		details.ifPresent(info -> {
			// Get the list of emails that needs to be put in quarantine and saves them to the database
			QuarantinedEmailBatch batch = getEmailQuarantineBatch(dto.getNotificationType(), info, dto.getSesMessageId());

			if (!batch.isEmpty()) {
				quarantineDao.addToQuarantine(batch);
			}
		});
	}

	QuarantinedEmailBatch getEmailQuarantineBatch(SESNotificationType notificationType, SESJsonNotificationDetails details,
			String sesMessageId) {

		if (NO_QUARANTINE_SET.contains(notificationType)) {
			return QuarantinedEmailBatch.EMPTY_BATCH;
		}

		List<SESJsonRecipient> recipients = details.getRecipients();

		if (recipients == null || recipients.isEmpty()) {
			return QuarantinedEmailBatch.EMPTY_BATCH;
		}

		EmailQuarantineProvider provider = quarantineProviderMap.get(notificationType);

		if (provider == null) {
			throw new IllegalStateException("Quarantine provider not found for notification type " + notificationType);
		}

		return provider.getQuarantinedEmails(details, sesMessageId);
	}

	SESNotificationType parseNotificationType(String notificationType) {
		SESNotificationType type = SESNotificationType.UNKNOWN;

		if (!StringUtils.isBlank(notificationType)) {
			try {
				type = SESNotificationType.valueOf(notificationType.toUpperCase());
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		return type;
	}

	Optional<SESJsonNotificationDetails> getNotificationDetails(SESNotificationType notificationType, SESJsonNotification notification) {
		switch (notificationType) {
		case BOUNCE:
			return Optional.ofNullable(notification.getBounce());
		case COMPLAINT:
			return Optional.ofNullable(notification.getComplaint());
		default:
			return Optional.empty();
		}
	}

}
