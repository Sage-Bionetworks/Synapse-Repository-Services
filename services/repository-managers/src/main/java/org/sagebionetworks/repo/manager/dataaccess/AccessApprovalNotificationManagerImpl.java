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

import javax.annotation.PostConstruct;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.notifications.DataAccessNotificationBuilder;
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

	// Do not re-send another notification if one was sent already within the last 7 days
	private static final long REVOKED_RESEND_TIMEOUT_DAYS = 7;
	private static final long CHANGE_TIMEOUT_HOURS = 24;

	private StackConfiguration stackConfiguration;
	private BroadcastMessageDao broadcastMessageDao;
	private UserManager userManager;
	private DataAccessNotificationDao notificationDao;
	private AccessApprovalDAO accessApprovalDao;
	private AccessRequirementDAO accessRequirementDao;
	private FileHandleManager fileHandleManager;
	private MessageManager messageManager;
	private ProdDetector prodDetector;

	private Map<DataAccessNotificationType, DataAccessNotificationBuilder> notificationBuilders;
	private UserInfo notificationSender;

	@Autowired
	public AccessApprovalNotificationManagerImpl(
			final StackConfiguration stackConfiguration,
			final UserManager userManager, 
			final BroadcastMessageDao broadcastMessageDao,
			final DataAccessNotificationDao notificationDao, 
			final AccessApprovalDAO accessApprovalDao,
			final AccessRequirementDAO accessRequirementDao, 
			final FileHandleManager fileHandleManager,
			final MessageManager messageManager, 
			final ProdDetector prodDetector) {
		this.stackConfiguration = stackConfiguration;
		this.broadcastMessageDao = broadcastMessageDao;
		this.userManager = userManager;
		this.notificationDao = notificationDao;
		this.accessApprovalDao = accessApprovalDao;
		this.accessRequirementDao = accessRequirementDao;
		this.fileHandleManager = fileHandleManager;
		this.messageManager = messageManager;
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

	@PostConstruct
	public void init() {
		Long senderId = stackConfiguration.getDataAccessNotificationsSender();
		notificationSender = userManager.getUserInfo(senderId);
	}

	@Override
	@WriteTransaction
	public void processAccessApprovalChangeMessage(UserInfo user, ChangeMessage message)
			throws RecoverableMessageException {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(message, "The change message");

		// Should we process this change?
		if (discardChangeMessage(message)) {
			return;
		}
		
		// Register the change as processed so that is not processed again (changes are sent multiple times)
		broadcastMessageDao.setBroadcast(message.getChangeNumber());
		
		AccessApproval approval = accessApprovalDao.get(message.getObjectId());

		// Should we process this approval change?
		if (discardAccessApproval(approval)) {
			return;
		}

		// Now we can build and send the notification
		if (deliverMessage(approval)) {
			sendMessageToUser(DataAccessNotificationType.REVOCATION, approval);
		}
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
	
	boolean discardAccessApproval(AccessApproval approval) {
		// Do not process approvals that are not revoked
		if (!ApprovalState.REVOKED.equals(approval.getState())) {
			return true;
		}
		
		// Do not process approvals that do not expire
		if (approval.getExpiredOn() == null) {
			return true;
		}
		
		final Long requirementId = approval.getRequirementId();
		
		String accessRequirementType = accessRequirementDao.getConcreteType(requirementId.toString());

		// Does not process notifications for non-managed access requirements
		if (!ManagedACTAccessRequirement.class.getName().equals(accessRequirementType)) {
			return true;
		}
		
		Long recipientId = getRecipientForApproval(approval);

		// Check if a revocation was already sent to the user for this requirement
		Optional<Instant> sent = notificationDao.getSentOn(DataAccessNotificationType.REVOCATION, requirementId, recipientId);

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
		if (!accessApprovalDao.listApprovalsByAccessor(requirementId.toString(), recipientId.toString()).isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	void sendMessageToUser(DataAccessNotificationType notificationType, AccessApproval approval) throws RecoverableMessageException {

		DataAccessNotificationBuilder notificationBuidler = getNotificationBuilder(notificationType);
		
		AccessRequirement accessRequirement = accessRequirementDao.get(approval.getRequirementId().toString());
		
		if (!(accessRequirement instanceof ManagedACTAccessRequirement)) {
			throw new IllegalStateException("Cannot sent a notification for a non managed access requirement");
		}
		
		ManagedACTAccessRequirement managedAccessRequriement = (ManagedACTAccessRequirement) accessRequirement;

		String sender = notificationSender.getId().toString();
		String messageBody = notificationBuidler.buildMessageBody(managedAccessRequriement, approval);
		String mimeType = notificationBuidler.getMimeType();
		String subject = notificationBuidler.buildSubject(managedAccessRequriement, approval);
		String fileHandleId = storeNotificationBody(sender, messageBody, mimeType);
		Long recipientId = getRecipientForApproval(approval);

		MessageToUser message = new MessageToUser();

		message.setSubject(subject);
		message.setCreatedBy(sender);
		message.setIsNotificationMessage(false);
		message.setWithUnsubscribeLink(false);
		message.setWithProfileSettingLink(false);
		message.setFileHandleId(fileHandleId);
		message.setRecipients(Collections.singleton(recipientId.toString()));

		boolean overrideNotificationSettings = true;

		message = messageManager.createMessage(notificationSender, message, overrideNotificationSettings);

		Long messageId = Long.valueOf(message.getId());
		Instant sentOn = message.getCreatedOn().toInstant();

		notificationDao.registerSent(notificationType, accessRequirement.getId(), recipientId, approval.getId(), messageId, sentOn);
	}

	boolean deliverMessage(AccessApproval approval) throws RecoverableMessageException {
		boolean isProductionStack = prodDetector.isProductionStack()
				.orElseThrow(() -> new RecoverableMessageException("Cannot detect current stack"));

		if (isProductionStack) {
			return true;
		}
		
		Long recipientId = getRecipientForApproval(approval);

		// On a non production stack (e.g. staging, testing) we deliver the message only
		// if the accessor belongs to the testing group
		UserInfo accessor = userManager.getUserInfo(recipientId);

		return accessor.getGroups().contains(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId());
	}

	String storeNotificationBody(String sender, String messageBody, String mimeType) {
		try {
			return fileHandleManager
					.createCompressedFileFromString(sender, Date.from(Instant.now()), messageBody, mimeType).getId();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private Long getRecipientForApproval(AccessApproval approval) {
		return Long.valueOf(approval.getAccessorId());
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
