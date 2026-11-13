package org.sagebionetworks.repo.manager.docusign;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOEDucEnvelopeNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucEnvelopeToExamine;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class EDucSignatureNotificationManagerImpl implements EDucSignatureNotificationManager {

	private static final Logger LOG = LogManager.getLogger(EDucSignatureNotificationManagerImpl.class);

	/**
	 * The states an envelope can no longer move out of. Reaching any of them is recorded, which is what stops
	 * the envelope being examined again.
	 */
	static final Set<EDucStatusEnum> TERMINAL_STATUSES = Collections.unmodifiableSet(
			EnumSet.of(EDucStatusEnum.completed, EDucStatusEnum.declined, EDucStatusEnum.voided));

	/*
	 * The states the requester is told about: their request is either ready to submit, or stuck until they
	 * act. 'voided' is deliberately absent — an envelope the requester cancelled themselves is no longer
	 * referenced by the request and so is never examined, leaving only expiry and administrative voids,
	 * which are recorded silently.
	 */
	private static final Map<EDucStatusEnum, NotificationContent> NOTIFIED_STATUSES = Map.of(
			EDucStatusEnum.completed, new NotificationContent(
					"message/EDucSignatureCompletedTemplate.html.vtl",
					"Your data use certificate has been signed"),
			EDucStatusEnum.declined, new NotificationContent(
					"message/EDucSignatureDeclinedTemplate.html.vtl",
					"Action needed: a signer declined your data use certificate"));

	private record NotificationContent(String templateFile, String subject) {}

	private final EDucEnvelopeNotificationDao notificationDao;
	private final TemplatedMessageSender templatedMessageSender;
	private final UserManager userManager;
	private final FeatureManager featureManager;
	private final ProdDetector prodDetector;
	private final StackConfiguration stackConfiguration;

	public EDucSignatureNotificationManagerImpl(EDucEnvelopeNotificationDao notificationDao,
			TemplatedMessageSender templatedMessageSender, UserManager userManager, FeatureManager featureManager,
			ProdDetector prodDetector, StackConfiguration stackConfiguration) {
		this.notificationDao = notificationDao;
		this.templatedMessageSender = templatedMessageSender;
		this.userManager = userManager;
		this.featureManager = featureManager;
		this.prodDetector = prodDetector;
		this.stackConfiguration = stackConfiguration;
	}

	@Override
	public boolean isEnabled() {
		return featureManager.isFeatureEnabled(Feature.EDUC_SIGNATURE_NOTIFICATIONS);
	}

	@Override
	public List<EDucEnvelopeToExamine> listEnvelopesToExamine(long limit) {
		return notificationDao.listEnvelopesToExamine(limit);
	}

	@WriteTransaction
	@Override
	public void processObservedStatus(EDucEnvelopeToExamine envelope, EDucStatusEnum status) {
		ValidateArgument.required(envelope, "envelope");

		// A status that could not be read, or one the envelope can still move out of, is left for the next
		// examination rather than recorded.
		if (status == null || !TERMINAL_STATUSES.contains(status)) {
			return;
		}

		// Two instances can examine the same envelope at once. Whichever takes the lock first records the
		// state; the other finds the row and stops, so the requester is notified once.
		Optional<DBOEDucEnvelopeNotification> alreadyRecorded = notificationDao
				.findForUpdate(envelope.envelopeId());
		if (alreadyRecorded.isPresent()) {
			return;
		}

		NotificationContent content = NOTIFIED_STATUSES.get(status);
		long messageId = DBOEDucEnvelopeNotification.NO_MESSAGE_SENT;

		if (content != null && deliverMessage(envelope.createdBy())) {
			messageId = Long.parseLong(sendNotification(envelope, content));
		}

		notificationDao.create(envelope.requestId(), envelope.envelopeId(), status.name(), messageId);
	}

	private String sendNotification(EDucEnvelopeToExamine envelope, NotificationContent content) {
		UserInfo sender = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());

		Map<String, Object> context = Map.of("signatureUrl", signatureUrl(envelope.requestId()));

		String messageId = templatedMessageSender.sendMessage(MessageTemplate.builder()
				.withSender(sender)
				.withRecipients(Collections.singleton(envelope.createdBy().toString()))
				.withTemplateFile(content.templateFile())
				.withSubject(content.subject())
				.withContext(context)
				.build()).getId();

		LOG.info("Sent message {} about envelope {} to the creator of request {}.", messageId,
				envelope.envelopeId(), envelope.requestId());

		return messageId;
	}

	/**
	 * The page where the requester reviews signature status and submits the request. Composed here rather
	 * than in the template so that the portal's base URL stays configurable.
	 */
	String signatureUrl(Long requestId) {
		String url = stackConfiguration.getPortalBaseEndpoint() + "/RequestHistory:request/" + requestId
				+ "/signature";
		// Guards against a misconfigured base sending requesters somewhere outside Synapse.
		EmailUtils.validateSynapsePortalHost(url);
		return url;
	}

	/**
	 * Whether the email should actually go out. Staging runs the same workers against the same data, so
	 * delivering from both would notify every requester twice.
	 */
	boolean deliverMessage(Long recipientId) {
		UserInfo recipient = userManager.getUserInfo(recipientId);
		if (featureManager.isUserInTestingGroup(recipient)) {
			return true;
		}
		return prodDetector.isProductionStack()
				.orElseThrow(() -> new RecoverableMessageException("Could not detect current stack version."));
	}
}
