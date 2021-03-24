package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.SendRawEmailRequestBuilder.BodyType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.MessageToUserUtils;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Lists;

public class MessageManagerImpl implements MessageManager {

	private static final Logger LOG = LogManager.getLogger(MessageManagerImpl.class);
	
	/**
	 * The maximum number of messages a user can create within a given interval
	 * The interval is defined by {@link #MESSAGE_CREATION_INTERVAL}
	 */
	private static final long MAX_NUMBER_OF_NEW_MESSAGES = 10L;
	
	/**
	 * The maximum number of targets of a message
	 */
	protected static final int MAX_NUMBER_OF_RECIPIENTS = 50;
	
	/**
	 * The span of the interval, in milliseconds, in which created messages are counted
	 * See {@link #MAX_NUMBER_OF_NEW_MESSAGES}  
	 */
	private static final long MESSAGE_CREATION_INTERVAL_MILLISECONDS = 60000L;
	
	// Message templates
	private static final String MESSAGE_TEMPLATE_PASSWORD_CHANGE_CONFIRMATION = "message/PasswordChangeConfirmationTemplate.txt";

	private static final String MESSAGE_TEMPLATE_DELIVERY_FAILURE = "message/DeliveryFailureTemplate.txt";

	private static final String MESSAGE_TEMPLATE_PASSWORD_RESET = "message/PasswordResetTemplate.txt";

	private static final String MESSAGE_VALUE_ORIGIN_CLIENT = "Synapse";
	
	private MessageDAO messageDAO;
	
	private UserGroupDAO userGroupDAO;
	
	private GroupMembersDAO groupMembersDAO;
	
	private UserManager userManager;
	
	private UserProfileManager userProfileManager;
	
	private NotificationEmailDAO notificationEmailDao;
	
	private PrincipalAliasDAO principalAliasDAO;
	
	private AuthorizationManager authorizationManager;
	
	private SynapseEmailService sesClient;
	
	private FileHandleManager fileHandleManager;
	
	private FileHandleDao fileHandleDao;
	
	private NodeDAO nodeDAO;
	
	private EntityAclManager entityAclManager;
	
	private EmailQuarantineDao emailQuarantineDao;
	
	// Note: This class has 14 dependencies, needs refactoring
	@Autowired
	public MessageManagerImpl(MessageDAO messageDAO, UserGroupDAO userGroupDAO,
			GroupMembersDAO groupMembersDAO, UserManager userManager,
			UserProfileManager userProfileManager,
			NotificationEmailDAO notificationEmailDao,
			PrincipalAliasDAO principalAliasDAO,
			AuthorizationManager authorizationManager,
			SynapseEmailService sesClient,
			FileHandleManager fileHandleManager, NodeDAO nodeDAO,
			EntityAclManager entityAclManager,
			FileHandleDao fileHandleDao,
			EmailQuarantineDao emailQuarantineDao) {
		this.messageDAO = messageDAO;
		this.userGroupDAO = userGroupDAO;
		this.groupMembersDAO = groupMembersDAO;
		this.userManager = userManager;
		this.userProfileManager = userProfileManager;
		this.notificationEmailDao = notificationEmailDao;
		this.principalAliasDAO = principalAliasDAO;
		this.authorizationManager = authorizationManager;
		this.sesClient = sesClient;
		this.fileHandleManager = fileHandleManager;
		this.nodeDAO = nodeDAO;
		this.entityAclManager = entityAclManager;
		this.fileHandleDao = fileHandleDao;
		this.emailQuarantineDao = emailQuarantineDao;
	}
	
	@Override
	public MessageToUser getMessage(UserInfo userInfo, String messageId) throws NotFoundException {
		MessageToUser message = messageDAO.getMessage(messageId);
		
		// Get the user's ID and the user's groups' IDs
		String userId = userInfo.getId().toString();
		Set<String> userGroups = new HashSet<String>();
		for (Long ug : userInfo.getGroups()) {
			userGroups.add(ug.toString());
		}
		userGroups.add(userId);
		
		// Is the user the sender?
		if (!message.getCreatedBy().equals(userId)) {
			// Is the user an intended recipient?
			boolean isRecipient = false;
			for (String recipient : message.getRecipients()) {
				if (userGroups.contains(recipient)) {
					isRecipient = true;
					break;
				}
			}
			
			// Not allowed to get the message
			if (!isRecipient) {
				throw new UnauthorizedException("You are not the sender or receiver of this message (" + messageId + ")");
			}
		}
		return message;
	}
	
	@Override
	public String getMessageFileRedirectURL(UserInfo userInfo, String messageId) throws NotFoundException {
		// If the user can get the message metadata (permission checking by the manager)
		// then the user can download the file
		MessageToUser dto = getMessage(userInfo, messageId);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, dto.getFileHandleId())
				.withAssociation(FileHandleAssociateType.MessageAttachment, messageId);
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	@WriteTransaction
	public MessageToUser createMessageWithThrottle(UserInfo userInfo, MessageToUser dto) throws NotFoundException {
		boolean userIsTrustedMessageSender = userInfo.getGroups().contains(TeamConstants.TRUSTED_MESSAGE_SENDER_TEAM_ID);

		// Throttle message creation
		if (!userInfo.isAdmin() && !userIsTrustedMessageSender && !messageDAO.canCreateMessage(userInfo.getId().toString(), 
					MAX_NUMBER_OF_NEW_MESSAGES,
					MESSAGE_CREATION_INTERVAL_MILLISECONDS)) {
			throw new TooManyRequestsException(
					"Please slow down.  You may send a maximum of "
							+ MAX_NUMBER_OF_NEW_MESSAGES + " message(s) every "
							+ (MESSAGE_CREATION_INTERVAL_MILLISECONDS / 1000) + " second(s)");
		}
		
		return createMessage(userInfo, dto);
	}

	@Override
	@WriteTransaction
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto) throws NotFoundException {
		return createMessage(userInfo, dto, false);
	}
	
	@Override
	@WriteTransaction
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto, boolean overrideNotificationSettings) throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(dto, "dto");
		ValidateArgument.requirement(dto.getRecipients() != null && !dto.getRecipients().isEmpty(),
				"recipients must be set.");
		// Make sure the sender is correct
		dto.setCreatedBy(userInfo.getId().toString());
		if (!userInfo.isAdmin()) {
			// Can't be anonymous
			if (AuthorizationUtils.isUserAnonymous(userInfo)) {
				throw new UnauthorizedException("Anonymous user may not send messages.");
			}

			boolean userIsTrustedMessageSender = userInfo.getGroups().contains(TeamConstants.TRUSTED_MESSAGE_SENDER_TEAM_ID);

			// Limit the number of recipients
			if (!userIsTrustedMessageSender && dto.getRecipients() != null && dto.getRecipients().size() > MAX_NUMBER_OF_RECIPIENTS) {
				throw new IllegalArgumentException(
						"May not message more than "
								+ MAX_NUMBER_OF_RECIPIENTS
								+ " at once.  Consider grouping the recipients in a Team if possible.");
			}
		}
		
		if (!authorizationManager.canAccessRawFileHandleById(userInfo, dto.getFileHandleId()).isAuthorized()
				&& !messageDAO.canSeeMessagesUsingFileHandle(userInfo.getGroups(), dto.getFileHandleId())) {
			throw new UnauthorizedException("Invalid file handle given");
		}
		
		// Make sure the recipients all exist
		List<UserGroup> ugs = userGroupDAO.get(Lists.newArrayList(dto.getRecipients()));
		if (ugs.size() != dto.getRecipients().size()) {
			throw new IllegalArgumentException("One or more of the following IDs are not recognized: " + dto.getRecipients());
		}
		
		return messageDAO.createMessage(dto, overrideNotificationSettings);
	}
	
	@Override
	@WriteTransaction
	public MessageToUser createMessageToEntityOwner(UserInfo userInfo,
			String entityId, MessageToUser toCreate) throws NotFoundException, ACLInheritanceException {
		// No permission checks since we only need to find the IDs of the creator of the node
		//   (or anyone with CHANGE_PERMISSIONS access)
		Node entity = nodeDAO.getNode(entityId);
		String benefactor = nodeDAO.getBenefactor(entityId);		
		AccessControlList acl = entityAclManager.getACL(benefactor, userInfo);
		
		// Find all users with permission to change permissions
		Set<Long> sharers = new HashSet<Long>();
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS)) {
				sharers.add(ra.getPrincipalId());
			}
		}

		if (toCreate.getRecipients() == null) {
			toCreate.setRecipients(new HashSet<String>());
		}
		
		// If the creator has permission, just message the creator
		if (sharers.contains(entity.getCreatedByPrincipalId())) {
			toCreate.getRecipients().add(entity.getCreatedByPrincipalId().toString());
			
		// Otherwise message everyone else
		} else {
			if (sharers.size() <= 0) {
				throw new UnauthorizedException("Unable to find a user with access to this entity.  Please contact a Synapse Administrator at synapseInfo@sagebase.org");
			}
			for (Long sharer : sharers) {
				toCreate.getRecipients().add(sharer.toString());
			}
		}
		
		// Create the message like normal
		return createMessage(userInfo, toCreate);
	}
	
	@Override
	@WriteTransaction
	public MessageToUser forwardMessage(UserInfo userInfo, String messageId,
			MessageRecipientSet recipients) throws NotFoundException {
		MessageToUser message = getMessage(userInfo, messageId);
		message.setRecipients(recipients.getRecipients());
		message.setInReplyTo(messageId);
		message = MessageToUserUtils.setUserGeneratedMessageFooter(message);
		return createMessageWithThrottle(userInfo, message);
	}

	@Override
	public List<MessageToUser> getConversation(UserInfo userInfo, String associatedMessageId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException {
		MessageToUser dto = messageDAO.getMessage(associatedMessageId);
		String rootMessageId = dto.getInReplyToRoot();
		
		List<MessageToUser> dtos = messageDAO.getConversation(rootMessageId, userInfo.getId().toString(), 
				sortBy, descending, limit, offset);
		return dtos;
	}

	@Override
	public List<MessageBundle> getInbox(UserInfo userInfo, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageBundle> dtos = messageDAO.getReceivedMessages(userInfo.getId().toString(), 
				included, sortBy, descending, limit, offset);
		return dtos;
	}

	@Override
	public List<MessageToUser> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageToUser> dtos = messageDAO.getSentMessages(userInfo.getId().toString(), 
				sortBy, descending, limit, offset);
		return dtos;
	}

	@Override
	public void markMessageStatus(UserInfo userInfo, MessageStatus status) throws NotFoundException {
		// Check to see if the user can see the message being updated
		getMessage(userInfo, status.getMessageId());
		
		// Update the message
		status.setRecipientId(userInfo.getId().toString());
		boolean succeeded = messageDAO.updateMessageStatus(status);
		
		if (!succeeded) {
			throw new UnauthorizedException("Cannot change status of message (" + status.getMessageId() + ")");
		}
	}

	@Override
	@WriteTransaction
	public List<String> processMessage(String messageId, ProgressCallback progressCallback) throws NotFoundException {
		MessageToUser dto = messageDAO.getMessage(messageId);
		boolean overrideNotificationSettings = messageDAO.overrideNotificationSettings(messageId);
		FileHandle fileHandle = fileHandleDao.get(dto.getFileHandleId());
		ContentType contentType = ContentType.parse(fileHandle.getContentType());
		String mimeType = contentType.getMimeType().trim().toLowerCase();

		try {
			String messageBody = fileHandleManager.downloadFileToString(dto.getFileHandleId());
			return processMessage(dto, messageBody, mimeType, overrideNotificationSettings, progressCallback);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * See {@link #processMessage(String, ProgressCallback)}
	 * 
	 * @param messageBody The body of any email(s) that get sent as a result of processing this message
	 *    Note: This parameter is provided so that templated messages do not need to be uploaded then downloaded before sending
	 */
	private List<String> processMessage(MessageToUser dto, String messageBody, String mimeType, boolean overrideNotificationSettings, ProgressCallback progressCallback) throws NotFoundException {
		List<String> errors = new ArrayList<String>();
		
		// Check to see if the message has already been sent
		// If so, nothing else needs to be done
		if (messageDAO.getMessageSent(dto.getId())) {
			return errors;
		}
		String senderUserName = null;

		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		
		// in practice there is always a user name, but strictly speaking it is allowed to be missing
		
		try {
			senderUserName = principalAliasDAO.getUserName(userInfo.getId());
		} catch (NotFoundException e) {
			senderUserName = null;
		}
		String senderDisplayName = null;
		try {
			UserProfile senderUserProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
			senderDisplayName = EmailUtils.getDisplayName(senderUserProfile);
		} catch (NotFoundException e) {
			senderDisplayName = null;
		}
		
		// Get the individual recipients
		Set<String> recipients = expandRecipientSet(userInfo, dto.getRecipients(), errors);
		
		// Now that the recipients list has been expanded, begin processing the message
		for (String userId : recipients) {
			// Try to send messages to each user individually
			try {
				
				// Get the user's settings
				Settings settings = new Settings();
				
				if (!overrideNotificationSettings) {
					UserProfile profile = userProfileManager.getUserProfile(userId);
					if (profile.getNotificationSettings() != null) {
						settings = profile.getNotificationSettings();
					}
				}
				
				MessageStatusType userMessageStatus = null; // setting to null tells the DAO to use the default value
				
				messageDAO.createMessageStatus(dto.getId(), userId, userMessageStatus);	
				
				// Should emails be sent?
				if (settings.getSendEmailNotifications() == null || settings.getSendEmailNotifications()) {
					String email = getEmailForUser(Long.parseLong(userId));
					
					if (emailQuarantineDao.isQuarantined(email)) {
						errors.add("Cannot deliver message to recipient (" + userId + "). The recipient does not have a valid notification email.");
						continue;
					}
					
					SendRawEmailRequestBuilder.BodyType bodyType;
					if (ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
						bodyType = SendRawEmailRequestBuilder.BodyType.JSON;
					} else {
						boolean isHtml= ContentType.TEXT_HTML.getMimeType().equals(mimeType);
						bodyType = isHtml ? SendRawEmailRequestBuilder.BodyType.HTML : SendRawEmailRequestBuilder.BodyType.PLAIN_TEXT;
					}

					SendRawEmailRequest sendRawEmailRequest = new SendRawEmailRequestBuilder()
						.withRecipientEmail(email)
						.withSubject(dto.getSubject())
						.withTo(dto.getTo())
						.withCc(dto.getCc())
						.withBcc(dto.getBcc())
						.withBody(messageBody, bodyType)
						.withSenderUserName(senderUserName)
						.withSenderDisplayName(senderDisplayName)
						.withUserId(userId)
						.withNotificationUnsubscribeEndpoint(dto.getNotificationUnsubscribeEndpoint())
						.withUnsubscribeLink(dto.getWithUnsubscribeLink())
						.withUserProfileSettingEndpoint(dto.getUserProfileSettingEndpoint())
						.withProfileSettingLink(dto.getWithProfileSettingLink())
						.withIsNotificationMessage(dto.getIsNotificationMessage())
						.build();
					sesClient.sendRawEmail(sendRawEmailRequest);
					// Should the message be marked as READ?
					if (settings.getMarkEmailedMessagesAsRead() != null && settings.getMarkEmailedMessagesAsRead()) {
						userMessageStatus = MessageStatusType.READ;
					}
				}
				
				if (userMessageStatus!=null) {
					// the status has been changed, so we have to update it again
					MessageStatus messageStatus = new MessageStatus();
					messageStatus.setMessageId(dto.getId());
					messageStatus.setRecipientId(userId);
					messageStatus.setStatus(userMessageStatus);
					messageDAO.updateMessageStatus(messageStatus);
				}
			} catch (Exception e) {
				LOG.info("Error caught while processing message", e);
				errors.add("Failed while processing message for recipient (" + userId + "): " + e.getMessage());
			}
		}
		
		messageDAO.updateMessageTransmissionAsComplete(dto.getId());
		
		return errors;
	}
	
	/**
	 * Helper for {@link #processMessage(String, boolean, String)}
	 * 
	 * Takes a set of user IDs and expands it into a set of individuals that the user is permitted to message
	 */
	private Set<String> expandRecipientSet(UserInfo userInfo, Set<String> intendedRecipients, List<String> errors) throws NotFoundException {
		// From the list of intended recipients, filter out the un-permitted recipients
		Set<String> recipients = new HashSet<String>();
		for (String principalId : intendedRecipients) {
			Long principalIdLong = Long.parseLong(principalId);
			UserGroup ug;
			try {
				ug = userGroupDAO.get(principalIdLong);
			} catch (NotFoundException e) {
				errors.add(e.getMessage());
				continue;
			}
			
			boolean userIsTrustedMessageSender = userInfo.getGroups().contains(TeamConstants.TRUSTED_MESSAGE_SENDER_TEAM_ID);
			// Check permissions to send to non-individuals
			if (!userIsTrustedMessageSender &&
					!ug.getIsIndividual() &&
					!authorizationManager.canAccess(userInfo, principalId, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE).isAuthorized()) {
				String sender = null;
				String team = null;
				try {
					sender = principalAliasDAO.getUserName(userInfo.getId());
				} catch (NotFoundException e) {
					sender = userInfo.getId().toString();
				}
				try {
					team = principalAliasDAO.getTeamName(Long.parseLong(principalId));
				} catch (NotFoundException e) {
					team = principalId;
				}
				errors.add(sender + " may not send messages to the group (" + team + ")");
				continue;
			}
			
			// The principal is a valid recipient
			if (ug.getIsIndividual()) {
				recipients.add(principalId);
			} else {
				// Handle the implicit group that contains all users
				// Note: only admins can pass the authorization check to reach this
				if (BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString().equals(principalId)) {
					for (UserGroup member : userGroupDAO.getAllPrincipals()) {
						if (member.getIsIndividual() && !isNotEmailRecipient(member.getId())) {
							recipients.add(member.getId());
						}
					}
				}
				
				// Expand non-individuals into individuals
				for (UserGroup member : groupMembersDAO.getMembers(principalId)) {
					recipients.add(member.getId());
				}
			}
		}
		
		return recipients;
	}
	
	// this filters out bootstrapped principals, who are NOT email recipients
	private static boolean isNotEmailRecipient(String principalId) {
		for (BOOTSTRAP_PRINCIPAL pb : BOOTSTRAP_PRINCIPAL.values()) {
			if (pb.getPrincipalId().toString().equals(principalId)) return true;
		}
		return false;
	}
	
	@Override
	@WriteTransaction
	public void deleteMessage(UserInfo userInfo, String messageId) {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only admins may delete messages");
		}
		
		messageDAO.deleteMessage(messageId);
	}
	
	@Override
	@WriteTransaction
	public void sendNewPasswordResetEmail(String passwordResetUrlPrefix, PasswordResetSignedToken passwordResetToken,
			PrincipalAlias alias) {
		ValidateArgument.required(passwordResetToken, "passwordResetToken");
		ValidateArgument.required(passwordResetUrlPrefix, "passwordResetPrefix");
		ValidateArgument.required(alias, "alias");

		long userId = Long.parseLong(passwordResetToken.getUserId());

		ValidateArgument.requirement(alias.getPrincipalId().equals(userId),
				String.format("The id of the alias principal must match the token user id: %d (Expected: %d)",
						alias.getPrincipalId(), userId));

		String resetUrl = getPasswordResetUrl(passwordResetUrlPrefix, passwordResetToken);
		String email = getEmailForAlias(alias);
		
		if (emailQuarantineDao.isQuarantined(email)) {
			logQuarantinedAddress("password reset email", email);
			throw new QuarantinedEmailException("There was a problem with the provided account, please contact support.");
		}

		String subject = "Reset Synapse Password";
		Map<String, String> fieldValues = new HashMap<String, String>();

		String username = principalAliasDAO.getUserName(userId);
		UserProfile userProfile = userProfileManager.getUserProfile(Long.toString(userId));
		String displayName = EmailUtils.getDisplayName(userProfile);

		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, MESSAGE_VALUE_ORIGIN_CLIENT);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, displayName);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_USERNAME, username);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_WEB_LINK, resetUrl);

		String messageBody = EmailUtils.readMailTemplate(MESSAGE_TEMPLATE_PASSWORD_RESET, fieldValues);

		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(email)
				.withSubject(subject)
				.withBody(messageBody, BodyType.PLAIN_TEXT)
				.withSenderUserName(username)
				.withSenderDisplayName(displayName)
				.withUserId(Long.toString(userId))
				.withIsNotificationMessage(true)
				.build();

		sesClient.sendRawEmail(sendEmailRequest);
	}

	@Override
	public void sendPasswordChangeConfirmationEmail(long userId){
		String subject = "Your Synapse Password Has Been Changed";
		Map<String,String> fieldValues = new HashMap<String,String>();

		String alias = principalAliasDAO.getUserName(userId);
		UserProfile userProfile = userProfileManager.getUserProfile(Long.toString(userId));
		String displayName = EmailUtils.getDisplayName(userProfile);

		fieldValues.put(EmailUtils.TEMPLATE_KEY_ORIGIN_CLIENT, MESSAGE_VALUE_ORIGIN_CLIENT);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, displayName);
		fieldValues.put(EmailUtils.TEMPLATE_KEY_USERNAME, alias);

		String messageBody = EmailUtils.readMailTemplate(MESSAGE_TEMPLATE_PASSWORD_CHANGE_CONFIRMATION, fieldValues);
		String email = getEmailForUser(userId);
		
		if (emailQuarantineDao.isQuarantined(email)) {
			logQuarantinedAddress("password change confirmation", email);
			throw new QuarantinedEmailException("There was a problem with your account, please contact support.");
		}
		
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(email)
				.withSubject(subject)
				.withBody(messageBody, BodyType.PLAIN_TEXT)
				.withSenderUserName(alias)
				.withSenderDisplayName(displayName)
				.withUserId(Long.toString(userId))
				.withIsNotificationMessage(true)
				.build();
		
		sesClient.sendRawEmail(sendEmailRequest);
	}
	
	@Override
	@WriteTransaction
	public void sendDeliveryFailureEmail(String messageId, List<String> errors) throws NotFoundException {
		// Build the subject and body of the message
		MessageToUser dto = messageDAO.getMessage(messageId);
		
		// Notification messages are special system generated messages that might be sent on behalf of the user
		// but that the user might not even be aware of, we avoid bouncing back to the user if the notification didn't go
		// through
		if (Boolean.TRUE.equals(dto.getIsNotificationMessage())) {
			LOG.warn("Skipping delivery failure notification for notification message (Message: {}, Errors: {})", messageId, StringUtils.join(errors));
			return;
		}
		
		Long senderId = Long.parseLong(dto.getCreatedBy());
		String subject = "Message " + messageId + " Delivery Failure(s)";
		
		String alias = principalAliasDAO.getUserName(senderId);

		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DISPLAY_NAME, alias);
		
		fieldValues.put(EmailUtils.TEMPLATE_KEY_MESSAGE_SUBJECT, dto.getSubject());
		fieldValues.put(EmailUtils.TEMPLATE_KEY_DETAILS, "- " + StringUtils.join(errors, "\n- "));
		String email = getEmailForUser(senderId);
		
		if (emailQuarantineDao.isQuarantined(email)) {
			logQuarantinedAddress("delivery failure email", email);
			throw new QuarantinedEmailException("Cannot send delivery failure email to quarantined address");
		}
		
		String messageBody = EmailUtils.readMailTemplate(MESSAGE_TEMPLATE_DELIVERY_FAILURE, fieldValues);
		
		SendRawEmailRequest sendEmailRequest = new SendRawEmailRequestBuilder()
				.withRecipientEmail(email)
				.withSubject(subject)
				.withBody(messageBody, BodyType.PLAIN_TEXT)
				.withNotificationUnsubscribeEndpoint(dto.getNotificationUnsubscribeEndpoint())
				.withUserId(senderId.toString())
				.withIsNotificationMessage(true)
				.build();
		sesClient.sendRawEmail(sendEmailRequest);
	}
	
	String getEmailForAlias(PrincipalAlias alias) throws NotFoundException {
		if (AliasType.USER_EMAIL.equals(alias.getType())) {
			return alias.getAlias();
		}
		return getEmailForUser(alias.getPrincipalId());
	}
	
	String getEmailForUser(Long principalId) throws NotFoundException {
		return notificationEmailDao.getNotificationEmailForPrincipal(principalId);
	}
	
	String getPasswordResetUrl(String passwordResetUrlPrefix, PasswordResetSignedToken passwordResetToken) {
		String resetUrl = passwordResetUrlPrefix + SerializationUtils.serializeAndHexEncode(passwordResetToken);
		EmailUtils.validateSynapsePortalHost(resetUrl);
		return resetUrl;
	}
	
	private void logQuarantinedAddress(String messageType, String email) {
		LOG.warn("Cannot send {} to quarantined address: {}", messageType, email);
	}

}
