package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;


public class MessageManagerImpl implements MessageManager, InitializingBean {
	
	static private Logger log = LogManager.getLogger(MessageManagerImpl.class);
	
	/**
	 * The maximum number of messages a user can create within a given interval
	 * The interval is defined by {@link #MESSAGE_CREATION_INTERVAL}
	 */
	private static final long MAX_NUMBER_OF_NEW_MESSAGES = 10L;
	
	/**
	 * The span of the interval, in milliseconds, in which created messages are counted
	 * See {@link #MAX_NUMBER_OF_NEW_MESSAGES}  
	 */
	private static final long MESSAGE_CREATION_INTERVAL_MILLISECONDS = 60000L;
	
	/**
	 * The maximum number of targets of a message
	 */
	protected static final long MAX_NUMBER_OF_RECIPIENTS = 50L;
	
	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private AmazonSimpleEmailService amazonSESClient;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	/**
	 * The ID of the default group AUTHENTICATED_USERS
	 */
	private String authenticatedUsersId;
	
	/**
	 * The ID of the user from which Synapse sends emails
	 */
	private String emailerId;
	
	public MessageManagerImpl() { };
	
	/**
	 * Used for testing
	 */
	public MessageManagerImpl(MessageDAO messageDAO, UserGroupDAO userGroupDAO,
			GroupMembersDAO groupMembersDAO, UserManager userManager,
			UserProfileDAO userProfileDAO,
			AuthorizationManager authorizationManager,
			AmazonSimpleEmailService amazonSESClient,
			FileHandleManager fileHandleManager, NodeDAO nodeDAO,
			EntityPermissionsManager entityPermissionsManager) {
		this.messageDAO = messageDAO;
		this.userGroupDAO = userGroupDAO;
		this.groupMembersDAO = groupMembersDAO;
		this.userManager = userManager;
		this.userProfileDAO = userProfileDAO;
		this.authorizationManager = authorizationManager;
		this.amazonSESClient = amazonSESClient;
		this.fileHandleManager = fileHandleManager;
		this.nodeDAO = nodeDAO;
		this.entityPermissionsManager = entityPermissionsManager;
	}
	
	@Override
	public void setFileHandleManager(FileHandleManager fileHandleManager) {
		this.fileHandleManager = fileHandleManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		UserGroup authUsers = userGroupDAO.findGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false);
		if (authUsers == null) {
			throw new DatastoreException("Could not find the default group for all authenticated users");
		}
		authenticatedUsersId = authUsers.getId();
		
		UserGroup emailer = userGroupDAO.findGroup(StackConfiguration.getNotificationEmailAddress(), true);
		if (emailer == null) {
			throw new DatastoreException("Could not find the user that sends Synapse emails");
		}
		emailerId = emailer.getId();
	}
	
	@Override
	public MessageToUser getMessage(UserInfo userInfo, String messageId) throws NotFoundException {
		MessageToUser message = messageDAO.getMessage(messageId);
		
		// Get the user's ID and the user's groups' IDs
		String userId = userInfo.getIndividualGroup().getId();
		Set<String> userGroups = new HashSet<String>();
		for (UserGroup ug : userInfo.getGroups()) {
			userGroups.add(ug.getId());
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
	public URL getMessageFileRedirectURL(UserInfo userInfo, String messageId) throws NotFoundException {
		// If the user can get the message metadata (permission checking by the manager)
		// then the user can download the file
		MessageToUser dto = getMessage(userInfo, messageId);
		return fileHandleManager.getRedirectURLForFileHandle(dto.getFileHandleId());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto) throws NotFoundException {
		// Make sure the sender is correct
		dto.setCreatedBy(userInfo.getIndividualGroup().getId());
		
		if (!userInfo.isAdmin()) {
			// Throttle message creation
			if (!messageDAO.canCreateMessage(userInfo.getIndividualGroup().getId(), 
						MAX_NUMBER_OF_NEW_MESSAGES,
						MESSAGE_CREATION_INTERVAL_MILLISECONDS)) {
				throw new TooManyRequestsException(
						"Please slow down.  You may send a maximum of "
								+ MAX_NUMBER_OF_NEW_MESSAGES + " message(s) every "
								+ (MESSAGE_CREATION_INTERVAL_MILLISECONDS / 1000) + " second(s)");
			}
			
			// Limit the number of recipients
			if (dto.getRecipients() != null && dto.getRecipients().size() > MAX_NUMBER_OF_RECIPIENTS) {
				throw new IllegalArgumentException(
						"May not message more than "
								+ MAX_NUMBER_OF_RECIPIENTS
								+ " at once.  Consider grouping the recipients in a Team if possible.");
			}
		}
		
		if (!authorizationManager.canAccessRawFileHandleById(userInfo, dto.getFileHandleId())
				&& !messageDAO.canSeeMessagesUsingFileHandle(userInfo.getGroups(), dto.getFileHandleId())) {
			throw new UnauthorizedException("Invalid file handle given");
		}
		
		dto = messageDAO.createMessage(dto);
		
		// If the recipient list is only one element long, 
		// process and send the message in this transaction 
		if (dto.getRecipients().size() == 1) {
			UserGroup ug;
			try {
				ug = userGroupDAO.get(dto.getRecipients().iterator().next());
			} catch (NotFoundException e) {
				throw new DatastoreException("Could not get a user group that satisfied message creation constraints");
			}
			
			// Defer the sending of messages to non-individuals 
			// since there could be more than one actual recipient after finding the members
			if (ug.getIsIndividual()) {
				List<String> errors;
				try {
					errors = processMessage(dto.getId(), true);
				} catch (NotFoundException e) {
					throw new DatastoreException("Could not find a message that was created in the same transaction");
				}
				if (errors.size() > 0) {
					throw new IllegalArgumentException(StringUtils.join(errors, "\n"));
				}
			}
		}
		return dto;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser createMessageToEntityOwner(UserInfo userInfo,
			String entityId, MessageToUser toCreate) throws NotFoundException, ACLInheritanceException {
		// No permission checks since we only need to find the IDs of the creator of the node
		//   (or anyone with CHANGE_PERMISSIONS access)
		Node entity = nodeDAO.getNode(entityId);
		AccessControlList acl = entityPermissionsManager.getACL(entityId, userInfo);
		
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser forwardMessage(UserInfo userInfo, String messageId,
			MessageRecipientSet recipients) throws NotFoundException {
		MessageToUser message = getMessage(userInfo, messageId);
		message.setRecipients(recipients.getRecipients());
		message.setInReplyTo(messageId);
		return createMessage(userInfo, message);
	}

	@Override
	public QueryResults<MessageToUser> getConversation(UserInfo userInfo, String associatedMessageId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException {
		MessageToUser dto = messageDAO.getMessage(associatedMessageId);
		String rootMessageId = dto.getInReplyToRoot();
		
		List<MessageToUser> dtos = messageDAO.getConversation(rootMessageId, userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getConversationSize(rootMessageId, userInfo.getIndividualGroup().getId());
		return new QueryResults<MessageToUser>(dtos, totalMessages);
	}

	@Override
	public QueryResults<MessageBundle> getInbox(UserInfo userInfo, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageBundle> dtos = messageDAO.getReceivedMessages(userInfo.getIndividualGroup().getId(), 
				included, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumReceivedMessages(userInfo.getIndividualGroup().getId(), included);
		return new QueryResults<MessageBundle>(dtos, totalMessages);
	}

	@Override
	public QueryResults<MessageToUser> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageToUser> dtos = messageDAO.getSentMessages(userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumSentMessages(userInfo.getIndividualGroup().getId());
		return new QueryResults<MessageToUser>(dtos, totalMessages);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void markMessageStatus(UserInfo userInfo, MessageStatus status) throws NotFoundException {
		// Check to see if the user can see the message being updated
		getMessage(userInfo, status.getMessageId());
		
		// Update the message
		status.setRecipientId(userInfo.getIndividualGroup().getId());
		boolean succeeded = messageDAO.updateMessageStatus(status);
		
		if (!succeeded) {
			throw new UnauthorizedException("Cannot change status of message (" + status.getMessageId() + ")");
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public List<String> processMessage(String messageId) throws NotFoundException {
		return processMessage(messageId, false);
	}
	
	/**
	 * See {@link #processMessage(String)}
	 * Also used by {@link #createMessage(UserInfo, MessageToUser)}
	 * 
	 * @param singleTransaction Should the sending be done in one transaction or one transaction per recipient?
	 *    This allows the sending of messages during creation to complete without deadlock. 
	 *    Note: It is crucial to pass in "true" when creating *and* sending a message in the same operation together. 
	 *      Otherwise the 'sending step' waits forever for the lock obtained by the 'creation step' to be released.
	 * General usage of this method sets this parameter to false.
	 */
	private List<String> processMessage(String messageId, boolean singleTransaction) throws NotFoundException {
		MessageToUser dto = messageDAO.getMessage(messageId);
		String messageBody = downloadEmailContent(dto.getFileHandleId());
		return processMessage(dto, singleTransaction, messageBody);
	}
	
	/**
	 * See {@link #processMessage(String, boolean)}
	 * Also used by {@link #sendTemplateEmail(String, String, String, boolean)}
	 * 
	 * @param messageBody The body of any email(s) that get sent as a result of processing this message
	 *    Note: This parameter is provided so that templated messages do not need to be uploaded then downloaded before sending
	 */
	private List<String> processMessage(MessageToUser dto, boolean singleTransaction, String messageBody) throws NotFoundException {
		List<String> errors = new ArrayList<String>();
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		
		// Check to see if the message has already been sent
		// If so, nothing else needs to be done
		if (messageDAO.hasMessageBeenSent(dto.getId())) {
			return errors;
		}
		
		// Get the individual recipients
		Set<String> recipients = expandRecipientSet(userInfo, dto.getRecipients(), errors);
		
		// Make sure the caller set the boolean correctly
		if (recipients.size() > 1 && singleTransaction) {
			throw new IllegalArgumentException("A message sent to multiple recipients must be done in separate transactions");
		}
		
		// Now that the recipients list has been expanded, begin processing the message
		for (String user : recipients) {
			// Try to send messages to each user individually
			try {
				// Get the user's settings
				Settings settings = null;
				try {
					UserProfile profile = userProfileDAO.get(user);
					settings = profile.getNotificationSettings();
				} catch (NotFoundException e) { }
				if (settings == null) {
					settings = new Settings();
				}
				
				MessageStatusType defaultStatus = null;
				
				// Should emails be sent?
				if (settings.getSendEmailNotifications() == null || settings.getSendEmailNotifications()) {
					sendEmail(userGroupDAO.get(user).getName(), 
							dto.getSubject(),
							messageBody, 
							//TODO change this to an alias
							//TODO bootstrap a better name for the notification user
							userInfo.getUser().getDisplayName());
					
					// Should the message be marked as READ?
					if (settings.getMarkEmailedMessagesAsRead() != null && settings.getMarkEmailedMessagesAsRead()) {
						defaultStatus = MessageStatusType.READ;
					}
				}
				
				// This marks a user as a recipient of the message
				// which is equivalent to marking the message as sent
				if (singleTransaction) {
					messageDAO.createMessageStatus_SameTransaction(dto.getId(), user, defaultStatus);
				} else {
					messageDAO.createMessageStatus_NewTransaction(dto.getId(), user, defaultStatus);
				}
			} catch (Exception e) {
				log.info("Error caught while processing message", e);
				errors.add("Failed while processing message for recipient (" + user + "): " + e.getMessage());
			}
		}
		
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
			UserGroup ug;
			try {
				ug = userGroupDAO.get(principalId);
			} catch (NotFoundException e) {
				errors.add(e.getMessage());
				continue;
			}
			
			// Check permissions to send to non-individuals
			if (!ug.getIsIndividual()
					&& !authorizationManager.canAccess(userInfo, principalId, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)) {
				errors.add(userInfo.getIndividualGroup().getName()
						+ " may not send messages to the group (" + principalId + ")");
				continue;
			}
			
			// The principal is a valid recipient
			if (ug.getIsIndividual()) {
				recipients.add(principalId);
			} else {
				// Handle the implicit group that contains all users
				// Note: only admins can pass the authorization check to reach this
				if (authenticatedUsersId.equals(principalId)) {
					for (UserGroup member : userGroupDAO.getAll()) {
						if (member.getIsIndividual()) {
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
	
	/**
	 * Helper for {@link #processMessage(String, boolean, String)}
	 * 
	 * Returns a string containing the body of a message
	 */
	private String downloadEmailContent(String fileHandleId) throws NotFoundException {
		URL url = fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
		
		// Read the file
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = null;
			try {
				byte[] buffer = new byte[1024];
				in = url.openStream();
				int length = 0;
				while ((length = in.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}
				return new String(out.toByteArray());
			} finally {
				if (in != null) {
					in.close();
				}
				out.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper for {@link #processMessage(String, boolean, String)}
	 * and for {@link #sendTemplateEmail(String, String, String, boolean)}
	 * 
	 * Constructs a email to send via Amazon SES
	 */
	private SendEmailResult sendEmail(String recipient, String subject, String body) {
		return sendEmail(recipient, subject, body, null);
	}
	
	/**
	 * See {@link #sendEmail(String, String, String)}
	 * 
	 * @param sender The username of the sender (null tolerant)
	 */
	private SendEmailResult sendEmail(String recipient, String subject, String body, String sender) {
		// Construct whom the email is from 
		String source = StackConfiguration.getNotificationEmailAddress();
		if (sender != null) {
			source = sender + " <" + source + ">";
		}
		
		// Construct an object to contain the recipient address
        Destination destination = new Destination().withToAddresses(recipient);
        
        // Create the subject and body of the message
        if (subject == null) {
        	subject = "";
        }
        Content textSubject = new Content().withData(subject);
        Body messageBody = new Body().withText(new Content().withData(body));
        
        // Create a message with the specified subject and body
        Message message = new Message().withSubject(textSubject).withBody(messageBody);
        
        // Assemble the email
		SendEmailRequest request = new SendEmailRequest()
				.withSource(source)
				.withDestination(destination)
				.withMessage(message);
        
        // Send the email
        return amazonSESClient.sendEmail(request);  
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteMessage(UserInfo userInfo, String messageId) {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only admins may delete messages");
		}
		
		messageDAO.deleteMessage(messageId);
	}
	
	
	//////////////////////////////////////////
	// Email template constants and methods //
	//////////////////////////////////////////
	
	private static final String TEMPLATE_KEY_ORIGIN_CLIENT = "#originclient#";
	private static final String TEMPLATE_KEY_DISPLAY_NAME = "#displayname#";
	private static final String TEMPLATE_KEY_USERNAME = "#username#";
	private static final String TEMPLATE_KEY_WEB_LINK = "#link#";
	private static final String TEMPLATE_KEY_MESSAGE_ID = "#messageid#";
	private static final String TEMPLATE_KEY_DETAILS = "#details#";

	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendPasswordResetEmail(String recipientId, OriginatingClient originClient, String sessionToken) throws NotFoundException {
		// Build the subject and body of the message
		UserInfo recipient = userManager.getUserInfo(Long.parseLong(recipientId));
		String domain = WordUtils.capitalizeFully(originClient.name());
		String subject = "Set " + domain + " Password";
		String messageBody = readMailTemplate("message/PasswordResetTemplate.txt");
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_ORIGIN_CLIENT, domain);
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_DISPLAY_NAME, recipient.getUser().getDisplayName());
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_USERNAME, recipient.getIndividualGroup().getName());
		String webLink;
		switch (originClient) {
		case BRIDGE:
			webLink = "https://bridge.synapse.org/webapp/resetPassword.html?token=" + sessionToken;
			break;
		case SYNAPSE:
			webLink = "https://www.synapse.org/Portal.html#!PasswordReset:" + sessionToken;
			break;
		default:
			throw new IllegalArgumentException("Unknown origin client type: " + originClient);
		}
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_WEB_LINK, webLink);
		
		sendTemplateEmail(recipientId, subject, messageBody, false);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendWelcomeEmail(String recipientId, OriginatingClient originClient) throws NotFoundException {
		// Build the subject and body of the message
		UserInfo recipient = userManager.getUserInfo(Long.parseLong(recipientId));
		String domain = WordUtils.capitalizeFully(originClient.name());
		String subject = "Welcome to " + domain + "!";
		String messageBody = readMailTemplate("message/WelcomeTemplate.txt");
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_ORIGIN_CLIENT, domain);
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_DISPLAY_NAME, recipient.getUser().getDisplayName());
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_USERNAME, recipient.getIndividualGroup().getName());
		
		sendTemplateEmail(recipientId, subject, messageBody, false);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendDeliveryFailureEmail(String messageId, List<String> errors) throws NotFoundException {
		// Build the subject and body of the message
		MessageToUser dto = messageDAO.getMessage(messageId);
		UserInfo sender = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		String subject = "Message " + messageId + " Delivery Failure(s)";
		String messageBody = readMailTemplate("message/DeliveryFailureTemplate.txt");
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_DISPLAY_NAME, sender.getUser().getDisplayName());
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_MESSAGE_ID, messageId);
		messageBody = messageBody.replaceAll(TEMPLATE_KEY_DETAILS, "- " + StringUtils.join(errors, "\n- "));
		
		sendTemplateEmail(sender.getIndividualGroup().getId(), subject, messageBody, false);
	}
	
	/**
	 * Helper for sending templated emails
	 * 
	 * Reads a resource into a string
	 */
	private String readMailTemplate(String filename) {
		try {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			try {
				String s = br.readLine();
				while (s != null) {
					sb.append(s + "\r\n");
					s = br.readLine();
				}
				return sb.toString();
			} finally {
				br.close();
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper for sending templated emails
	 * 
	 * @param createRecord Should the message be saved within the messaging system?
	 *   i.e. Transient emails like password resets do not need to be saved and should not be saved
	 *   Note: If set to false, an email is sent regardless of the user's preferences for notifications 
	 *   (because there would be no other way of retrieving the message).
	 */
	private void sendTemplateEmail(String recipientId, String subject, String messageBody, boolean createRecord) throws NotFoundException {
		// Send out the message
		if (createRecord) {
			// Upload the message body to S3
			final String content = messageBody;
			FileItemStream fis = new FileItemStream() {
				@Override
				public InputStream openStream() throws IOException {
					return new ByteArrayInputStream(content.getBytes());
				}

				@Override
				public boolean isFormField() {
					return false;
				}

				@Override
				public String getName() {
					return UUID.randomUUID() + ".txt";
				}

				@Override
				public String getFieldName() {
					return "none";
				}

				@Override
				public String getContentType() {
					return "application/text";
				}
			};
			S3FileHandle handle;
			try {
				handle = fileHandleManager.uploadFile(emailerId, fis);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ServiceUnavailableException e) {
				throw new RuntimeException(e);
			}
			
			// Add an entry to the message tables
			MessageToUser dto = new MessageToUser();
			dto.setCreatedBy(emailerId);
			dto.setFileHandleId(handle.getId());
			dto.setSubject(subject);
			dto.setRecipients(new HashSet<String>());
			dto.getRecipients().add(recipientId);
			
			// Skip the throttling and auto-sending logic that is part of the MessageManager's createMessage method
			dto = messageDAO.createMessage(dto);
			
			// Now process the message like any other message
			List<String> errors = processMessage(dto, true, messageBody);
			if (errors.size() > 0) {
				throw new IllegalArgumentException(StringUtils.join(errors, "\n"));
			}
		} else {
			UserGroup recipient = userGroupDAO.get(recipientId);
			sendEmail(recipient.getName(), subject, messageBody);
		}
	}
}
