package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageManager {
	
	/**
	 * Retrieves a single message by ID.  
	 * The user must be either the sender or *intended* recipient of the message.  
	 * Otherwise, an UnauthorizedException is thrown.  
	 */
	public MessageToUser getMessage(UserInfo userInfo, String messageId) throws NotFoundException;
	
	/**
	 * Returns the redirect URL used to download the file containing the body of the message
	 */
	public String getMessageFileRedirectURL(UserInfo userInfo, String messageId) throws NotFoundException;
	
	/**
	 * Saves the message so that it can be processed by other queries.
	 * If the message is going to exactly one recipient, then the message will be sent in this transaction  
	 * and any failures will be propagated immediately.
	 * </br> 
	 * If the message is going to more than one recipient, a worker will asynchronously process the message.
	 * In case of failure, the user will be notified via bounce message.  
	 * </br>
	 * This method also checks to see if file handles (message body) are accessible.  
	 */
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto) throws NotFoundException;

	/**
	 * Adds the creator of the given entity to the recipient list of the
	 * message. If the creator is unable to share the entity, then users that
	 * can share the entity will be messaged instead.
	 * 
	 * Afterwards, calls {@link #createMessage(UserInfo, MessageToUser)}
	 */
	public MessageToUser createMessageToEntityOwner(UserInfo userInfo,
			String entityId, MessageToUser toCreate) throws NotFoundException,
			ACLInheritanceException;

	/**
	 * Saves an existing message so that it can be delivered to the given set of recipients
	 */
	public MessageToUser forwardMessage(UserInfo userInfo, String messageId,
			MessageRecipientSet recipients) throws NotFoundException;
	
	/**
	 * Retrieves all messages within the same conversation as the associated message.
	 * All returned messages will be visible to the user
	 * (i.e. the user is either the sender or receiver of the messages)
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public List<MessageToUser> getConversation(UserInfo userInfo, String associatedMessageId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException;
	
	/**
	 * Retrieves all messages received by the user
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public List<MessageBundle> getInbox(UserInfo userInfo, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Retrieves all messages sent by the user
	 */
	public List<MessageToUser> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Changes the status of the user's message 
	 */
	public void markMessageStatus(UserInfo userInfo, MessageStatus status) throws NotFoundException;
	
	/**
	 * Takes an existing message and processes it, 
	 * updating tables and sending emails where necessary and permitted.
	 * </br>
	 * Non-fatal errors will be caught and their error messages will be returned in a list.
	 * It is the caller's responsibility to send a bounce message to the user.
	 * </br>
	 * Note: This method is to be used by the MessageToUserWorker and should not be exposed via the REST API.
	 */
	public List<String> processMessage(String messageId, ProgressCallback progressCallback) throws NotFoundException;
	
	/**
	 * Deletes a message, only accessible to admins
	 */
	public void deleteMessage(UserInfo userInfo, String messageId);
	
	/**
	 * Sends a password reset email based on a template via Amazon SES, using as potential destination
	 * the email defined by the given alias if of type {@link AliasType#USER_EMAIL}, otherwise falling
	 * back to the default notification email for the user identified by the given alias.
	 * 
	 * @param passwordResetPrefix The url prefix for the verification callback in the portal
	 * @param passwordResetToken  The reset token generated for the user that needs the password reset
	 * @param alias               The alias to use as the destination, if the alias is not of type
	 *                            {@link AliasType#USER_EMAIL} falls back to the default notification
	 *                            email of the user that owns the given alias. The principal id in the
	 *                            alias must match the user id of the signed token
	 */
	public void sendNewPasswordResetEmail(String passwordResetPrefix, PasswordResetSignedToken passwordResetToken,
			PrincipalAlias alias) throws NotFoundException;

	/**
	 * Send an email confirming to user that their password has been changed
	 * @param userId
	 */
	public void sendPasswordChangeConfirmationEmail(long userId);

	/**
	 * Sends a welcome email based on a template via Amazon SES
	 */
	public void sendWelcomeEmail(Long recipientId, String notificationUnsubscribeEndpoint) throws NotFoundException;
	
	/**
	 * Sends a delivery failure notification based on a template
	 */
	public void sendDeliveryFailureEmail(String messageId, List<String> errors) throws NotFoundException;

	/**
	 * Saves the message so that it can be processed by other queries.
	 * If the message is going to exactly one recipient, then the message will be sent in this transaction  
	 * and any failures will be propagated immediately.
	 * </br> 
	 * If the message is going to more than one recipient, a worker will asynchronously process the message.
	 * In case of failure, the user will be notified via bounce message.  
	 * </br>
	 * This method also handles throttling of message creation 
	 * and checks to see if file handles (message body) are accessible.  
	 */
	public MessageToUser createMessageWithThrottle(UserInfo userInfo, MessageToUser dto);

}
