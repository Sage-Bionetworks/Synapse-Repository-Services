package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Provides REST APIs for sending messages to other Synapse users and for commenting on Synapse entities.
 * </p>
 */
@ControllerInfo(displayName = "Message Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class MessageController {

	@Autowired
	private ServiceProvider serviceProvider;
	
	private static final String defaultInboxFilter = "UNREAD";
	private static final String defaultSortOrder = "SEND_DATE";
	private static final String defaultSortDescending = "true";

	/**
	 * <p>
	 * Sends a message.
	 * </p>
	 * <p>
	 * The "fileHandleId" field should point to a plain text file containing the body of the message.  
	 * The file should be uploaded prior to this call.
	 * </p>
	 * <p>
	 * The "recipients" field should hold a set of IDs corresponding to the recipients of the message.  
	 * </p>
	 * <p>
	 * All other fields are optional, including the "subject" field.  
	 * To chain messages together into a conversation, specify the message you are replying to via the "inReplyTo" field.  
	 * </p>
	 * <p>
	 * See the <a href="${org.sagebionetworks.repo.model.message.MessageToUser}">message schema</a>,  
	 * <a href="${org.sagebionetworks.repo.model.message.MessageContent}">message content schema</a>, 
	 * and <a href="${org.sagebionetworks.repo.model.message.MessageRecipientSet}">message recipient schema</a>
	 * for more information.
	 * </p>
	 * <p>
	 * In most cases, message delivery is asynchronous to message creation.  
	 * i.e. It may take several seconds for a message to appear in a user's inbox.
	 * </p>
	 * <p>
	 * Notes: 
	 * <br/>
	 * Unauthorized delivery, such as messaging a team you are not affiliated with, 
	 * will result in a bounce message being sent to your email.
	 * <br/>
	 * There are limits on the number of message recipients you can specify (50) and
	 * the rate at which you can send messages (10 per minute).  Neither these restrictions,
	 * nor the restriction that you can't message a Team with which you are unaffiliated,
	 * apply if you are a member of the Trusted Message Senders Team.
	 * </p>
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MESSAGE, method = RequestMethod.POST)
	public @ResponseBody
	MessageToUser createMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody MessageToUser toCreate) throws NotFoundException {
		return serviceProvider.getMessageService().create(userId, toCreate);
	}

	/**
	 * <b>Note:  This service is designed to be used by CloudMailIn, not by clients in general.</b>
	 * <p>Calling the service requires Basic Authentication credentials owned by the 
	 * the Synapse CloudMailIn account.</p>
	 * 
	 * @param toCreate the CloudMailIn message in JSON format
	 * @param notificationUnsubscribeEndpoint
	 * @throws Exception 
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.CLOUDMAILIN_MESSAGE, method = RequestMethod.POST)
	public void createCloudMailInMessage(
			@RequestBody Message toCreate,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, 
			required = false) String notificationUnsubscribeEndpoint
			) throws Exception {
		serviceProvider.getMessageService().create(toCreate, notificationUnsubscribeEndpoint);
	}

	/**
	 * <b>Note:  This service is designed to be used by CloudMailIn, not by clients in general.</b>
	 * 
	 * @param toAuthorize the header of the CloudMailIn message in JSON format
	 * @throws IllegalArgumentException if not valid
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.CLOUDMAILIN_AUTHORIZATION, method = RequestMethod.POST)
	public void authorizeCloudMailInMessage(
			@RequestBody AuthorizationCheckHeader toAuthorize) throws NotFoundException {
		serviceProvider.getMessageService().authorize(toAuthorize);
	}
	
	/**
	 * Retrieves the current authenticated user's inbox.  
	 * It may take several seconds for a message to appear in the inbox after creation.  
	 * </br>
	 * By default, the most recent unread messages are returned first.  
	 * To flip the ordering, set the "descending" parameter to "false".
	 * To change the way the messages are ordered, set the "orderBy" parameter to 
	 *   either "SEND_DATE" or "SUBJECT".
	 * To retrieve messages that have been read or archived, set the "inboxFilter" parameter to 
	 *   a comma-separated list of values defined in the <a href="${org.sagebionetworks.repo.model.message.MessageStatusType}">MessageStatusType enumeration</a>.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_INBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageBundle> getInbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.MESSAGE_INBOX_FILTER_PARAM, defaultValue = defaultInboxFilter) String inboxFilter, 
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset, 
			HttpServletRequest request) 
					throws NotFoundException {
		// Convert inbox filter param into a list
		List<MessageStatusType> filter = new ArrayList<MessageStatusType>();
		String[] splits = inboxFilter.split(ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR);
		for (String split : splits) {
			MessageStatusType d = MessageStatusType.valueOf(split.toUpperCase());
			filter.add(d);
		}
		
		MessageSortBy sortBy = MessageSortBy.valueOf(orderBy.toUpperCase());
		return serviceProvider.getMessageService().getInbox(userId, filter, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_INBOX);
	}
	
	/**
	 * Retrieves the current authenticated user's outbox.  
	 * </br>
	 * By default, the most recent messages are returned first.  
	 * To flip the ordering, set the "descending" parameter to "false".
	 * To change the way the messages are ordered, set the "orderBy" parameter to 
	 *   either "SEND_DATE" or "SUBJECT".
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_OUTBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getOutbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset, 
			HttpServletRequest request) 
					throws NotFoundException {
		MessageSortBy sortBy = MessageSortBy.valueOf(orderBy.toUpperCase());
		return serviceProvider.getMessageService().getOutbox(userId, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_OUTBOX);
	}
	
	/**
	 * Fetches the specified message.  
	 * The authenticated user must be either the sender or receiver of the message.  
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID, method = RequestMethod.GET)
	public @ResponseBody
	MessageToUser getMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String messageId) throws NotFoundException {
		return serviceProvider.getMessageService().getMessage(userId, messageId);
	}
	
	/**
	 * Forwards a message to the specified set of recipients.
	 * The authenticated user must be either the sender or receiver of the forwarded message.  
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_FORWARD, method = RequestMethod.POST)
	public @ResponseBody
	MessageToUser forwardMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String messageId, 
			@RequestBody MessageRecipientSet recipients) throws NotFoundException {
		return serviceProvider.getMessageService().forwardMessage(userId, messageId, recipients);
	}
	
	/**
	 * Retrieves messages belonging to the same thread as the given message.
	 * The current authenticated user will be either the sender or receiver of all returned messages.
	 * </br>
	 * By default, the most recent messages are returned first.  
	 * To flip the ordering, set the "descending" parameter to "false".
	 * To change the way the messages are ordered, set the "orderBy" parameter to 
	 *   either "SEND_DATE" or "SUBJECT".
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_CONVERSATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getConversation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String messageId, 
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset, 
			HttpServletRequest request) 
					throws NotFoundException {
		MessageSortBy sortBy = MessageSortBy.valueOf(orderBy.toUpperCase());
		return serviceProvider.getMessageService().getConversation(userId, messageId, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_ID_CONVERSATION);
	}
	
	/**
	 * Updates the current status of a message relative to the current authenticated user.
	 * Note: the "recipientId" field of the request body will be ignored.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_STATUS, method = RequestMethod.PUT)
	public void updateMessageStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody MessageStatus status) throws NotFoundException {
		serviceProvider.getMessageService().updateMessageStatus(userId, status);
	}

	@Deprecated
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID, method = RequestMethod.DELETE)
	public @ResponseBody String deleteMessage() throws NotFoundException {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.MESSAGE_ID + " and is only accessible to Synapse administrators";
	}

	/**
	 * Deletes a message.  Only accessible to administrators.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.MESSAGE_ID, method = RequestMethod.DELETE)
	public void deleteMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String messageId) throws NotFoundException {
		serviceProvider.getMessageService().deleteMessage(userId, messageId);
	}

	/**
	 * Get the actual URL of the file associated with the message
	 * </br>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * 
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 */
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_FILE, method = RequestMethod.GET)
	public void fileRedirectForMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable("messageId") String messageId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws NotFoundException, IOException {
		String redirectUrl = serviceProvider.getMessageService()
				.getMessageFileRedirectURL(userId, messageId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Adds the owner of the given entity as an additional recipient of the message.
	 * 
	 * Afterwards, behavior is identical to <a href="${POST.message}">POST /message</a>
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_MESSAGE, method = RequestMethod.POST)
	public @ResponseBody
	MessageToUser sendMessageToEntityOwner(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, 
			@RequestBody MessageToUser toCreate) throws NotFoundException, ACLInheritanceException {
		return serviceProvider.getMessageService().createMessageToEntityOwner(userId, id, toCreate);
	}
}
