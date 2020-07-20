package org.sagebionetworks.repo.manager.dataaccess;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.notifications.DataAccessNotificationBuilder;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationDao;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
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

	// TODO: Notifications configuration should be stored in the DB, including the resend timeout and
	// eventually the reminder period so that it can be changed later if needed in the DB
	
	// Do not re-send another notification if one was sent already within the last 7 days
	private static final long REVOKED_RESEND_TIMEOUT_DAYS = 7;
	
	// Do not process a change message if it's older than 24 hours
	private static final long CHANGE_TIMEOUT_HOURS = 24;
	
	// Fake id for messages to that are not actually created
	private static final long NO_MESSAGE_TO_USER = -1;

	private BroadcastMessageDao broadcastMessageDao;
	private UserManager userManager;
	private DataAccessNotificationDao notificationDao;
	private AccessApprovalDAO accessApprovalDao;
	private AccessRequirementDAO accessRequirementDao;
	private FileHandleManager fileHandleManager;
	private MessageManager messageManager;
	private FeatureManager featureManager;
	private ProdDetector prodDetector;

	private Map<DataAccessNotificationType, DataAccessNotificationBuilder> notificationBuilders;

	@Autowired
	public AccessApprovalNotificationManagerImpl(
			final UserManager userManager, 
			final BroadcastMessageDao broadcastMessageDao,
			final DataAccessNotificationDao notificationDao, 
			final AccessApprovalDAO accessApprovalDao,
			final AccessRequirementDAO accessRequirementDao, 
			final FileHandleManager fileHandleManager,
			final MessageManager messageManager, 
			final FeatureManager featureTesting,
			final ProdDetector prodDetector) {
		this.broadcastMessageDao = broadcastMessageDao;
		this.userManager = userManager;
		this.notificationDao = notificationDao;
		this.accessApprovalDao = accessApprovalDao;
		this.accessRequirementDao = accessRequirementDao;
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
		this.featureManager = featureTesting;
		this.prodDetector = prodDetector;
	}

	@Autowired(required = false)
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
		
		// Register the change as processed so that is not processed again (changes are sent multiple times)
		broadcastMessageDao.setBroadcast(message.getChangeNumber());
		
		AccessApproval approval = accessApprovalDao.get(message.getObjectId());

		UserInfo recipient = getRecipientForRevocation(approval);

		// Should we process this approval change?
		if (discardAccessApproval(approval, recipient)) {
			return;
		}

		// Send the notification
		sendNotification(DataAccessNotificationType.REVOCATION, approval, recipient);
	}

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

		// Check if the message was already processed
		if (broadcastMessageDao.wasBroadcast(change.getChangeNumber())) {
			return true;
		}
		
		return false;
	}
	
	boolean discardAccessApproval(AccessApproval approval, UserInfo recipient) {
		// Do not process approvals that are not revoked
		if (!ApprovalState.REVOKED.equals(approval.getState())) {
			return true;
		}
		
		final Long requirementId = approval.getRequirementId();
		
		String accessRequirementType = accessRequirementDao.getConcreteType(requirementId.toString());

		// Does not process notifications for non-managed access requirements
		if (!ManagedACTAccessRequirement.class.getName().equals(accessRequirementType)) {
			return true;
		}

		// Check if a revocation was already sent to the user for this requirement
		Optional<Instant> sent = notificationDao.getSentOn(DataAccessNotificationType.REVOCATION, requirementId, recipient.getId());

		if (sent.isPresent()) {

			Instant sentOn = sent.get();
			Instant approvalModifiedOn = approval.getModifiedOn().toInstant();

			// If it was sent after the approval modification then it was already processed
			if (sentOn.isAfter(approvalModifiedOn)) {
				return true;
			}
			
			// The approval was modified after the notification was sent (e.g. the user was added back and revoked again)
			// We do not want to re-send another notification if the last one for the same approval was within the last week
			if (sentOn.isAfter(approvalModifiedOn.minus(REVOKED_RESEND_TIMEOUT_DAYS, ChronoUnit.DAYS))) {
				return true;
			}

		}

		// We need to check if an APPROVED access approval exists already for the same access requirement, in such a case
		// there is no need to send a notification as the user is still considered APPROVED
		if (!accessApprovalDao.listApprovalsByAccessor(requirementId.toString(), recipient.getId().toString()).isEmpty()) {
			return true;
		}
		
		return false;
	}

	void sendNotification(DataAccessNotificationType notificationType, AccessApproval approval, UserInfo recipient) throws RecoverableMessageException {
		Long messageId;
		Instant sentOn;
		
		if (deliverMessage(approval, recipient)) {
			MessageToUser messageToUser = sendMessageToUser(notificationType, approval, recipient);
			messageId = Long.valueOf(messageToUser.getId());
			sentOn = messageToUser.getCreatedOn().toInstant();
		} else {
			// We do not deliver, but we still want to record the notification to avoid re-processing it
			LOG.warn("{} notification (AR: {}, Recipient: {}, AP: {}) will not be delivered", notificationType, approval.getRequirementId(), recipient.getId(), approval.getId());
			messageId = NO_MESSAGE_TO_USER;
			sentOn = Instant.now();
		}
		
		notificationDao.registerNotification(notificationType, approval.getRequirementId(), recipient.getId(), approval.getId(), messageId, sentOn);
	
	}
	
	MessageToUser sendMessageToUser(DataAccessNotificationType notificationType, AccessApproval approval, UserInfo recipient) {

		DataAccessNotificationBuilder notificationBuidler = getNotificationBuilder(notificationType);
		
		AccessRequirement accessRequirement = accessRequirementDao.get(approval.getRequirementId().toString());
		
		if (!(accessRequirement instanceof ManagedACTAccessRequirement)) {
			throw new IllegalStateException("Cannot sent a notification for a non managed access requirement");
		}
		
		ManagedACTAccessRequirement managedAccessRequriement = (ManagedACTAccessRequirement) accessRequirement;

		UserInfo notificationsSender = getNotificationsSender();
		
		String sender = notificationsSender.getId().toString();
		String messageBody = notificationBuidler.buildMessageBody(managedAccessRequriement, approval);
		String mimeType = notificationBuidler.getMimeType();
		String subject = notificationBuidler.buildSubject(managedAccessRequriement, approval);
		String fileHandleId = storeNotificationBody(sender, messageBody, mimeType);

		MessageToUser message = new MessageToUser();

		message.setSubject(subject);
		message.setCreatedBy(sender);
		message.setIsNotificationMessage(false);
		message.setWithUnsubscribeLink(false);
		message.setWithProfileSettingLink(false);
		message.setFileHandleId(fileHandleId);
		message.setRecipients(Collections.singleton(recipient.getId().toString()));

		boolean overrideNotificationSettings = true;

		return messageManager.createMessage(notificationsSender, message, overrideNotificationSettings);
	}
	
	UserInfo getNotificationsSender() {
		return userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());
	}

	boolean deliverMessage(AccessApproval approval, UserInfo recipient) throws RecoverableMessageException {
		// Checks if the feature is enabled first for the given recipient
		if (featureManager.isFeatureEnabledForUser(Feature.DATA_ACCESS_RENEWALS, recipient)) {
			return true;
		}
		
		// Once the feature is enabled we can process the notification only on production as we do not want
		// to send duplicated messages from prod/staging
		return prodDetector.isProductionStack().orElseThrow(() -> 
			new RecoverableMessageException("Cannot detect current stack")
		);
	}

	String storeNotificationBody(String sender, String messageBody, String mimeType) {
		try {
			return fileHandleManager
					.createCompressedFileFromString(sender, Date.from(Instant.now()), messageBody, mimeType).getId();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	UserInfo getRecipientForRevocation(AccessApproval approval) {
		return userManager.getUserInfo(Long.valueOf(approval.getAccessorId()));
	}
	
	private DataAccessNotificationBuilder getNotificationBuilder(DataAccessNotificationType notificationType) {
		DataAccessNotificationBuilder messageBuilder = notificationBuilders.get(notificationType);

		if (messageBuilder == null) {
			throw new IllegalStateException(
					"Could not find a message builder for " + notificationType + " notification type.");
		}
		return messageBuilder;
	}

}
