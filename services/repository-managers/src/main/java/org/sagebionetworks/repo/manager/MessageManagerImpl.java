package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
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


public class MessageManagerImpl implements MessageManager {
	
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
	
	public MessageManagerImpl() { };
	
	/**
	 * Used for testing
	 */
	public MessageManagerImpl(MessageDAO messageDAO, UserGroupDAO userGroupDAO,
			GroupMembersDAO groupMembersDAO, UserManager userManager, UserProfileDAO userProfileDAO,
			AuthorizationManager authorizationManager,
			AmazonSimpleEmailService amazonSESClient, FileHandleManager fileHandleManager) {
		this.messageDAO = messageDAO;
		this.userGroupDAO = userGroupDAO;
		this.groupMembersDAO = groupMembersDAO;
		this.userManager = userManager;
		this.userProfileDAO = userProfileDAO;
		this.authorizationManager = authorizationManager;
		this.amazonSESClient = amazonSESClient;
		this.fileHandleManager = fileHandleManager;
	}
	
	@Override
	public void setFileHandleManager(FileHandleManager fileHandleManager) {
		this.fileHandleManager = fileHandleManager;
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto) {
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
					errors = sendMessage(dto.getId(), true);
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
		return sendMessage(messageId, false);
	}
	
	/**
	 * See {@link #processMessage(String)}
	 * 
	 * @param singleTransaction Should the sending be done in one transaction or one transaction per recipient?
	 *    This allows the sending of messages during creation to complete without deadlock. 
	 *    Note: It is crucial to pass in "true" when creating *and* sending a message in the same operation together. 
	 *      Otherwise the 'sending step' waits forever for the lock obtained by the 'creation step' to be released.
	 * General usage of this method sets this parameter to false.
	 */
	private List<String> sendMessage(String messageId, boolean singleTransaction) throws NotFoundException {
		List<String> errors = new ArrayList<String>();
		
		MessageToUser dto = messageDAO.getMessage(messageId);
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		UserGroup authUsers = userGroupDAO.findGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false);
		if (authUsers == null) {
			throw new DatastoreException("Could not find the default group for all authenticated users");
		}
		
		// Check to see if the message has already been sent
		// If so, nothing else needs to be done
		if (messageDAO.hasMessageBeenSent(messageId)) {
			return errors;
		}
		
		// From the list of intended recipients, filter out the un-permitted recipients
		Set<String> recipients = new HashSet<String>();
		for (String principalId : dto.getRecipients()) {
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
				if (authUsers.getId().equals(principalId)) {
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
							userInfo.getIndividualGroup().getName(), 
							dto.getSubject(),
							downloadEmailContent(dto.getFileHandleId()));
					
					// Should the message be marked as READ?
					if (settings.getMarkEmailedMessagesAsRead() != null && settings.getMarkEmailedMessagesAsRead()) {
						defaultStatus = MessageStatusType.READ;
					}
				}
				
				// This marks a user as a recipient of the message
				// which is equivalent to marking the message as sent
				if (singleTransaction) {
					messageDAO.createMessageStatus_SameTransaction(messageId, user, defaultStatus);
				} else {
					messageDAO.createMessageStatus_NewTransaction(messageId, user, defaultStatus);
				}
			} catch (Exception e) {
				log.info("Error caught while processing message", e);
				errors.add(e.getMessage());
			}
		}
		
		return errors;
	}

	@Override
	public void deleteMessage(UserInfo userInfo, String messageId) {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only admins may delete messages");
		}
		
		messageDAO.deleteMessage(messageId);
	}

	@Override
	public void sendEmail(EMAIL_TEMPLATE template, String recipient,
			Map<String, String> replacements, boolean createRecord) {
		//TODO 
	}
	
	/**
	 * Returns a string containing the body of a message
	 */
	private String downloadEmailContent(String fileHandleId) throws NotFoundException, IOException {
		URL url = fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
		
		// Read the file
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
	}
	
	/**
	 * Constructs a email to send via Amazon SES
	 */
	private SendEmailResult sendEmail(String recipient, String from, String subject, String body) {
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
        SendEmailRequest request = new SendEmailRequest().withSource(from).withDestination(destination).withMessage(message);
        
        // Send the email
        System.out.println(request);
        return amazonSESClient.sendEmail(request);  
	}
	
}
