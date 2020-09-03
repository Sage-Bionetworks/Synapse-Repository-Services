package org.sagebionetworks.repo.manager.dataaccess;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.notifications.DataAccessNotificationBuilder;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotification;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.NotificationType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBODataAccessNotification;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessApprovalNotificationManagerImpl implements AccessApprovalNotificationManager {

	private static final Logger LOG = LogManager.getLogger(AccessApprovalNotificationManagerImpl.class);

	private static final String MSG_NOT_DELIVERED = "{} notification (AR: {}, Recipient: {}, AP: {}) will not be delivered.";
	
	// Simple inline DBO to DTO mapper
	private static final Function<DBODataAccessNotification, AccessApprovalNotification> DTO_MAPPER = (dbo) -> {
		AccessApprovalNotification dto = new AccessApprovalNotification();
		dto.setRequirementId(dbo.getRequirementId());
		dto.setNotificationType(NotificationType.valueOf(dbo.getNotificationType()));
		dto.setRecipientId(dbo.getRecipientId());
		dto.setSentOn(Date.from(dbo.getSentOn().toInstant()));
		return dto;
	};

	private UserManager userManager;
	private DataAccessNotificationDao notificationDao;
	private AccessApprovalDAO accessApprovalDao;
	private AccessRequirementDAO accessRequirementDao;
	private FileHandleManager fileHandleManager;
	private MessageManager messageManager;
	private FeatureManager featureManager;
	private ProdDetector prodDetector;
	private AuthorizationManager authManager;

	/**
	 * The map is initialized by {@link #configureDataAccessNotificationBuilders(List)} on bean creation
	 */
	private Map<DataAccessNotificationType, DataAccessNotificationBuilder> notificationBuilders;

	@Autowired
	public AccessApprovalNotificationManagerImpl(
			final UserManager userManager,
			final DataAccessNotificationDao notificationDao, 
			final AccessApprovalDAO accessApprovalDao,
			final AccessRequirementDAO accessRequirementDao, 
			final FileHandleManager fileHandleManager,
			final MessageManager messageManager, 
			final FeatureManager featureTesting, 
			final ProdDetector prodDetector,
			final AuthorizationManager authManager) {
		this.userManager = userManager;
		this.notificationDao = notificationDao;
		this.accessApprovalDao = accessApprovalDao;
		this.accessRequirementDao = accessRequirementDao;
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
		this.featureManager = featureTesting;
		this.prodDetector = prodDetector;
		this.authManager = authManager;
	}

	@Autowired
	public void configureDataAccessNotificationBuilders(List<DataAccessNotificationBuilder> builders) {
		notificationBuilders = new HashMap<>(DataAccessNotificationType.values().length);

		for (DataAccessNotificationBuilder builder : builders) {
			for (DataAccessNotificationType supportedType : builder.supportedTypes()) {
				if (notificationBuilders.containsKey(supportedType)) {
					throw new IllegalStateException(
							"A notification builder for type " + supportedType + " is already registred.");
				}
				notificationBuilders.put(supportedType, builder);
			}
		}

	}

	@Override
	@WriteTransaction
	public void processAccessApprovalChange(ChangeMessage message) throws RecoverableMessageException {
		ValidateArgument.required(message, "The change message");

		// Should we process this change?
		if (discardChangeMessage(message)) {
			return;
		}

		processAccessApproval(DataAccessNotificationType.REVOCATION, Long.valueOf(message.getObjectId()));
	}
	
	@Override
	@WriteTransaction
	public void processAccessApproval(DataAccessNotificationType notificationType, Long approvalId) throws RecoverableMessageException {
		
		// Check if the feature is enabled
		if (!featureManager.isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS)) {
			return;
		}
		
		AccessApproval approval = accessApprovalDao.get(approvalId.toString());

		// Check that the type expected by the notification type matches
		if (!notificationType.getExpectedState().equals(approval.getState())) {
			return;
		}
		
		sendMessageIfNeeded(notificationType, approval);
	}
	
	@Override
	public List<Long> listSubmitterApprovalsForUnsentReminder(DataAccessNotificationType notificationType, int limit) {
		ValidateArgument.required(notificationType, "The notification type");
		ValidateArgument.requirement(notificationType.isReminder(), "The notification type must be a reminder.");
		ValidateArgument.requirement(limit > 0, "The limit must be greater than zero.");
		
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		
		return notificationDao.listSubmmiterApprovalsForUnSentReminder(notificationType, today, limit);
	}
	
	@Override
	public AccessApprovalNotificationResponse listNotificationsRequest(UserInfo user, AccessApprovalNotificationRequest request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getRequirementId(), "The request.requirementId");
		ValidateArgument.required(request.getRecipientIds(), "The request.recipientIds");
		ValidateArgument.requirement(request.getRecipientIds().size() <= MAX_NOTIFICATION_REQUEST_RECIPIENTS,
				"The maximum number of allowed recipient ids in the request is " + MAX_NOTIFICATION_REQUEST_RECIPIENTS
						+ ".");
		
		if (!authManager.isACTTeamMemberOrAdmin(user)) {
			throw new UnauthorizedException("You must be a member of the ACT to perform this operation.");
		}

		final Long requirementId = request.getRequirementId();
		final List<Long> recipientIds = request.getRecipientIds();
		
		final AccessApprovalNotificationResponse response = new AccessApprovalNotificationResponse();
		
		List<AccessApprovalNotification> notifications = notificationDao.listForRecipients(requirementId, recipientIds)
				.stream()
				.map(DTO_MAPPER)
				.collect(Collectors.toList());
		
		response.setRequirementId(requirementId);
		response.setResults(notifications);
		
		return response;
	}

	boolean isSendRevocation(DataAccessNotificationType notificationType, AccessApproval approval, DBODataAccessNotification notification) {
		if (!DataAccessNotificationType.REVOCATION.equals(notificationType)) {
			throw new UnsupportedOperationException("Unsupported notification type " + notificationType);
		}
		
		// We need to check if an APPROVED access approval exists already for the same access requirement, in such a
		// case there is no need to send a notification as the user is still considered APPROVED
		if (accessApprovalDao.hasAccessorApproval(approval.getRequirementId().toString(), approval.getAccessorId())) {
			return false;
		}
		
		// A notification does not exist, we can send a new one
		if (notification == null) {
			return true;
		}
		
		final Instant sentOn = notification.getSentOn().toInstant();
		final Instant approvalModifiedOn = approval.getModifiedOn().toInstant();

		// If it was sent after the approval modification then it was already processed
		if (sentOn.isAfter(approvalModifiedOn)) {
			return false;
		}

		// The approval was modified after the notification was sent (e.g. the user was
		// added back and revoked again). We do not want to re-send another notification if the last one for the same
		// approval was within the last week
		if (sentOn.isAfter(approvalModifiedOn.minus(RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS))) {
			return false;
		}

		return true;
	}
	
	boolean isSendReminder(DataAccessNotificationType notificationType, AccessApproval approval, DBODataAccessNotification notification) {
		if (!notificationType.isReminder()) {
			throw new UnsupportedOperationException("Unsupported notification type " + notificationType);
		}
		
		// Reminders are not sent for approvals that do not expire
		if (approval.getExpiredOn() == null) {
			return false;
		}
		
		final Instant expiredOn = approval.getExpiredOn().toInstant();
		final LocalDate expiredDate = LocalDateTime.ofInstant(expiredOn, ZoneOffset.UTC).toLocalDate();
		
		// Double check the period
		if (!LocalDate.now(ZoneOffset.UTC).plus(notificationType.getReminderPeriod()).equals(expiredDate)) {
			return false;
		}
		
		// We need to check if the submitter has another approval after the expiration of the approval (including approvals that do not expire)
		if (accessApprovalDao.hasSubmitterApproval(approval.getRequirementId().toString(), approval.getSubmitterId(), expiredOn)) {
			return false;
		}
		
		// A notification does not exist, we can send a new one
		if (notification == null) {
			return true;
		}
		
		final Instant sentOn = notification.getSentOn().toInstant();
		
		// A reminder was already sent, check if it was processed recently
		if (sentOn.isAfter(Instant.now().minus(RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS))) {
			return false;
		}
		
		return true;
	}

	/**
	 * Checks if the given change message is valid and can be processed.
	 * 
	 * @param change The change message
	 * @return True if the the change is an update for an access approval and the change is not expired
	 */
	boolean discardChangeMessage(ChangeMessage change) {

		// Check if it's an ACCESS_APPROVAL message
		if (!ObjectType.ACCESS_APPROVAL.equals(change.getObjectType())) {
			return true;
		}

		// Process only UPDATES
		if (!ChangeType.UPDATE.equals(change.getChangeType())) {
			return true;
		}

		// Discard old changes
		if (change.getTimestamp().toInstant().isBefore(Instant.now().minus(CHANGE_TIMEOUT_HOURS, ChronoUnit.HOURS))) {
			return true;
		}

		return false;
	}

	void sendMessageIfNeeded(DataAccessNotificationType notificationType, AccessApproval approval) throws RecoverableMessageException {
		
		final Long approvalId = approval.getId();
		final Long requirementId = approval.getRequirementId();
		
		// Checks that we are processing a managed access requirement
		ManagedACTAccessRequirement requirement = getManagedAccessRequirement(requirementId).orElse(null);
		
		if (requirement == null) {
			return;
		}
		
		// Fetch the intended recipient for the notification type
		final RecipientProvider recipientProvider = getRecipientProvider(notificationType);
		
		final UserInfo recipient = userManager.getUserInfo(recipientProvider.getRecipient(approval));
		final Long recipientId = recipient.getId();
		
		// We check if a notification was sent out already for the given requirement and recipient, we acquire a lock on
		// the row if it exists
		Optional<DBODataAccessNotification> notification = notificationDao.findForUpdate(notificationType, requirementId, recipientId);

		final SendConditionProvider sendCondition = getSendConditionProvider(notificationType);
		
		// Validate that we can send the notification
		if (!sendCondition.canSend(notificationType, approval, notification.orElse(null))) {
			return;
		}

		Long messageId = NO_MESSAGE_TO_USER;
		Instant sentOn = Instant.now();

		// Check if the message can be delivered to the given recipient (e.g. on staging we usually do not want to send
		// notifications)
		if (deliverMessage(recipient)) {
			MessageToUser messageToUser = createMessageToUser(notificationType, approval, requirement, recipient);
			messageId = Long.valueOf(messageToUser.getId());
			sentOn = messageToUser.getCreatedOn().toInstant();
		} else {
			LOG.warn(MSG_NOT_DELIVERED, notificationType, requirementId, recipient.getId(), approvalId);
		}

		DBODataAccessNotification toStore = notification.orElse(new DBODataAccessNotification());

		// Align the data for creation/update
		toStore.setNotificationType(notificationType.name());
		toStore.setRequirementId(requirementId);
		toStore.setRecipientId(recipientId);
		toStore.setAccessApprovalId(approvalId);
		toStore.setMessageId(messageId);
		toStore.setSentOn(Timestamp.from(sentOn));

		// If two messages come at the same time, one or the other will fail on creation due to the unique constraint
		// One transaction will roll back and the message to user won't be sent
		if (toStore.getId() == null) {
			notificationDao.create(toStore);
		} else {
			notificationDao.update(toStore.getId(), toStore);
		}
	}

	MessageToUser createMessageToUser(DataAccessNotificationType notificationType, AccessApproval approval, ManagedACTAccessRequirement accessRequriement,
			UserInfo recipient) {

		DataAccessNotificationBuilder notificationBuilder = getNotificationBuilder(notificationType);

		UserInfo notificationsSender = getNotificationsSender();

		String sender = notificationsSender.getId().toString();
		String messageBody = notificationBuilder.buildMessageBody(accessRequriement, approval, recipient);
		String mimeType = notificationBuilder.getMimeType();
		String subject = notificationBuilder.buildSubject(accessRequriement, approval, recipient);

		// The message to user requires a file handle where the body is stored
		String fileHandleId = storeMessageBody(sender, messageBody, mimeType);

		MessageToUser message = new MessageToUser();

		message.setSubject(subject);
		message.setCreatedBy(sender);
		message.setIsNotificationMessage(false);
		message.setWithUnsubscribeLink(false);
		message.setWithProfileSettingLink(false);
		message.setFileHandleId(fileHandleId);
		message.setRecipients(Collections.singleton(recipient.getId().toString()));

		// We override the user notification setting as we want to send this kind of notifications anyway
		boolean overrideNotificationSettings = true;

		return messageManager.createMessage(notificationsSender, message, overrideNotificationSettings);
	}

	boolean deliverMessage(UserInfo recipient) throws RecoverableMessageException {

		// We always deliver the message if the user is in the synapse testing group
		if (featureManager.isUserInTestingGroup(recipient)) {
			return true;
		}

		// We deliver the actual email only in production to avoid duplicate messages
		// sent out both from prod and/or staging
		return prodDetector.isProductionStack()
				.orElseThrow(() -> new RecoverableMessageException("Could not detect current stack version."));
	}

	String storeMessageBody(String sender, String messageBody, String mimeType) {
		try {
			return fileHandleManager
					.createCompressedFileFromString(sender, Date.from(Instant.now()), messageBody, mimeType).getId();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	UserInfo getNotificationsSender() {
		return userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());
	}
	
	SendConditionProvider getSendConditionProvider(DataAccessNotificationType notificationType) {
		switch (notificationType) {
		case REVOCATION:
			return this::isSendRevocation;
		case FIRST_RENEWAL_REMINDER:
		case SECOND_RENEWAL_REMINDER:
			return this::isSendReminder;
		default:
			throw new UnsupportedOperationException("Unsupported notification type " + notificationType.name());
		}
	}
		
	RecipientProvider getRecipientProvider(DataAccessNotificationType notificationType) {
		switch (notificationType) {
		case REVOCATION:
			return (approval) -> Long.valueOf(approval.getAccessorId());
		case FIRST_RENEWAL_REMINDER:
		case SECOND_RENEWAL_REMINDER:
			return (approval) -> Long.valueOf(approval.getSubmitterId());
		default:
			throw new UnsupportedOperationException("Unsupported notification type " + notificationType.name());
		}
	}

	DataAccessNotificationBuilder getNotificationBuilder(DataAccessNotificationType notificationType) {
		if (notificationBuilders == null) {
			throw new IllegalStateException("The message builders were not initialized.");
		}

		DataAccessNotificationBuilder messageBuilder = notificationBuilders.get(notificationType);

		if (messageBuilder == null) {
			throw new IllegalStateException(
					"Could not find a message builder for " + notificationType + " notification type.");
		}
		return messageBuilder;
	}

	Optional<ManagedACTAccessRequirement> getManagedAccessRequirement(Long requirementId) {
		final AccessRequirement accessRequirement = accessRequirementDao.get(requirementId.toString());

		if (!(accessRequirement instanceof ManagedACTAccessRequirement)) {
			return Optional.empty();
		}

		final ManagedACTAccessRequirement managedAccessRequirement = (ManagedACTAccessRequirement) accessRequirement;

		if (!ACCESS_TYPE.DOWNLOAD.equals(managedAccessRequirement.getAccessType())) {
			return Optional.empty();
		}

		return Optional.of(managedAccessRequirement);
	}

	/**
	 * Internal functional interface to check if a notification for a given access approval should be sent
	 */
	@FunctionalInterface
	public static interface SendConditionProvider {

		/**
		 * @param notificationType The type of notification
		 * @param approval         The approval to send the notification for
		 * @param notification     An optional notification, can be null if no notification exist for the approval
		 * @return True iff a new notification should be sent out at this time for the given approval, false otherwise
		 */
		boolean canSend(DataAccessNotificationType notificationType, AccessApproval approval,
				DBODataAccessNotification notification);

	}

	/**
	 * Internal functional interface to fetch the recipient of a given approval
	 */
	@FunctionalInterface
	public static interface RecipientProvider {
		/**
		 * @param approval
		 * @return The id of the notification recipient for the given approval
		 */
		Long getRecipient(AccessApproval approval);
	}

}
