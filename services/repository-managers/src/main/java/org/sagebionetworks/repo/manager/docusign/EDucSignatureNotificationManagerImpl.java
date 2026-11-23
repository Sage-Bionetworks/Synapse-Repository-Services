package org.sagebionetworks.repo.manager.docusign;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.NotificationDeliveryPolicy;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOEDucEnvelopeNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucNotificationState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucPendingNotification;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class EDucSignatureNotificationManagerImpl implements EDucSignatureNotificationManager {

	private static final Logger LOG = LogManager.getLogger(EDucSignatureNotificationManagerImpl.class);

	/**
	 * The states an envelope can no longer move out of, which are the ones recorded.
	 */
	static final Set<EDucStatusEnum> TERMINAL_STATUSES = Collections.unmodifiableSet(
			EnumSet.of(EDucStatusEnum.completed, EDucStatusEnum.declined, EDucStatusEnum.voided));

	/*
	 * The states the requester is told about: their request is either ready to submit, or stuck until they
	 * act. 'voided' is deliberately absent: an envelope the requester cancelled themselves is no longer
	 * referenced by the request and so is never recorded, leaving only expiry and administrative voids,
	 * which are recorded silently.
	 */
	private static final Map<EDucStatusEnum, NotificationContent> NOTIFIED_STATUSES = Map.of(
			EDucStatusEnum.completed, new NotificationContent(
					"message/EDucSignatureCompletedTemplate.html.vtl",
					"Your data use certificate has been signed"),
			EDucStatusEnum.declined, new NotificationContent(
					"message/EDucSignatureDeclinedTemplate.html.vtl",
					"Action needed: a signer declined your data use certificate"));

	/**
	 * How far back a stack that has not read the signing service's status changes yet starts, so that
	 * envelopes finished shortly before it began are still notified.
	 */
	static final Duration STATUS_CHECKPOINT_LOOKBACK = Duration.ofDays(30);

	private record NotificationContent(String templateFile, String subject) {}

	private final EDucEnvelopeNotificationDao notificationDao;
	private final TemplatedMessageSender templatedMessageSender;
	private final UserManager userManager;
	private final NotificationDeliveryPolicy deliveryPolicy;
	private final StackConfiguration stackConfiguration;
	private final Clock clock;

	public EDucSignatureNotificationManagerImpl(EDucEnvelopeNotificationDao notificationDao,
			TemplatedMessageSender templatedMessageSender, UserManager userManager,
			NotificationDeliveryPolicy deliveryPolicy, StackConfiguration stackConfiguration, Clock clock) {
		this.notificationDao = notificationDao;
		this.templatedMessageSender = templatedMessageSender;
		this.userManager = userManager;
		this.deliveryPolicy = deliveryPolicy;
		this.stackConfiguration = stackConfiguration;
		this.clock = clock;
	}

	@Override
	public Instant getStatusCheckpoint() {
		return notificationDao.getStatusCheckpoint()
				.orElseGet(() -> clock.now().toInstant().minus(STATUS_CHECKPOINT_LOOKBACK));
	}

	@WriteTransaction
	@Override
	public void recordTerminalStatuses(Map<String, EDucStatusEnum> statusByEnvelopeId, Instant queriedOn) {
		ValidateArgument.required(statusByEnvelopeId, "statusByEnvelopeId");
		ValidateArgument.required(queriedOn, "queriedOn");

		Map<String, EDucStatusEnum> terminal = statusByEnvelopeId.entrySet().stream()
				.filter(entry -> entry.getValue() != null && TERMINAL_STATUSES.contains(entry.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// The signing account is shared with other stacks, whose envelopes no request here holds
		Map<String, Long> requestIdsByEnvelopeId = notificationDao.findRequestIdsByEnvelopeId(terminal.keySet());

		requestIdsByEnvelopeId.forEach((envelopeId, requestId) -> {
			// A window is read again after a failure, and windows overlap, so an envelope may be seen twice
			if (notificationDao.find(envelopeId).isEmpty()) {
				notificationDao.createPending(requestId, envelopeId, terminal.get(envelopeId));
			}
		});

		notificationDao.setStatusCheckpoint(queriedOn);
	}

	@Override
	public List<EDucPendingNotification> listPendingNotifications(long limit) {
		return notificationDao.listPendingNotifications(limit);
	}

	@WriteTransaction
	@Override
	public void processPendingNotification(EDucPendingNotification pending) {
		ValidateArgument.required(pending, "pending");

		// The lock, and the state read under it, make sure the requester is dealt with once
		Optional<DBOEDucEnvelopeNotification> recorded = notificationDao.findForUpdate(pending.envelopeId());
		if (recorded.isEmpty()
				|| !EDucNotificationState.PENDING.name().equals(recorded.get().getNotificationState())) {
			return;
		}

		NotificationContent content = NOTIFIED_STATUSES.get(pending.terminalStatus());
		if (content != null && deliveryPolicy.shouldDeliver(userManager.getUserInfo(pending.createdBy()))) {
			long messageId = Long.parseLong(sendNotification(pending, content));
			notificationDao.updateNotificationState(pending.envelopeId(), EDucNotificationState.SENT, messageId);
		} else {
			notificationDao.updateNotificationState(pending.envelopeId(), EDucNotificationState.NOT_SENT, null);
		}
	}

	private String sendNotification(EDucPendingNotification pending, NotificationContent content) {
		UserInfo sender = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());

		Map<String, Object> context = Map.of("signatureUrl", signatureUrl(pending.requestId()));

		String messageId = templatedMessageSender.sendMessage(MessageTemplate.builder()
				.withSender(sender)
				.withRecipients(Collections.singleton(pending.createdBy().toString()))
				.withTemplateFile(content.templateFile())
				.withSubject(content.subject())
				.withContext(context)
				.build()).getId();

		LOG.info("Sent message {} about envelope {} to the creator of request {}.", messageId,
				pending.envelopeId(), pending.requestId());

		return messageId;
	}

	/**
	 * The page where the requester reviews signature status and submits the request. Composed here rather
	 * than in the template so that the portal's base URL stays configurable.
	 */
	String signatureUrl(Long requestId) {
		// The base URL carries a trailing slash, which is not relied on so that a stack configured without one
		// still produces a well formed link
		String baseUrl = StringUtils.removeEnd(stackConfiguration.getSynapseBaseUrl(), "/");
		String url = baseUrl + "/RequestHistory:request/" + requestId + "/signature";
		// Guards against a misconfigured base sending requesters somewhere outside Synapse.
		EmailUtils.validateSynapsePortalHost(url);
		return url;
	}
}
