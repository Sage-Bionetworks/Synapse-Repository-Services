package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
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
// No javadocs since the implementation is not complete yet
// @ControllerInfo(displayName = "Message Services", path = "repo/v1")
@Controller
public class MessageController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;
	
	private static final String defaultInboxFilter = "UNREAD";
	private static final String defaultSortOrder = "SEND_DATE";
	private static final String defaultSortDescending = "true";

	/**
	 * Sends a message.  
	 * To chain messages together into a conversation, specify the message you are replying to via the "inReplyTo" field.  
	 * See the <a href="${org.sagebionetworks.repo.model.message.MessageToUser}">message schema</a>.
	 * </br>
	 * In most cases, message delivery is asynchronous to message creation.  
	 * i.e. It may take several seconds for a message to appear in a user's inbox.
	 * </br>
	 * Note: Unauthorized delivery, such as messaging a team you are not affiliated with, 
	 * may result in a silent failure or a bounce message, depending on your notification settings.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MESSAGE, method = RequestMethod.POST)
	public @ResponseBody
	MessageToUser createMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestBody MessageToUser toCreate) throws NotFoundException {
		return serviceProvider.getMessageService().create(username, toCreate);
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_INBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageBundle> getInbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_INBOX_FILTER_PARAM, defaultValue = defaultInboxFilter) String inboxFilter, 
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset, 
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
		return serviceProvider.getMessageService().getInbox(username, filter, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_INBOX);
	}
	
	/**
	 * Retrieves the current authenticated user's outbox.  
	 * </br>
	 * By default, the most recent messages are returned first.  
	 * To flip the ordering, set the "descending" parameter to "false".
	 * To change the way the messages are ordered, set the "orderBy" parameter to 
	 *   either "SEND_DATE" or "SUBJECT".
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_OUTBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getOutbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset, 
			HttpServletRequest request) 
					throws NotFoundException {
		MessageSortBy sortBy = MessageSortBy.valueOf(orderBy.toUpperCase());
		return serviceProvider.getMessageService().getOutbox(username, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_OUTBOX);
	}
	
	/**
	 * Fetches the specified message.  
	 * The authenticated user must be either the sender or receiver of the message.  
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID, method = RequestMethod.GET)
	public @ResponseBody
	MessageToUser getMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@PathVariable String messageId) throws NotFoundException {
		return serviceProvider.getMessageService().getMessage(username, messageId);
	}
	
	/**
	 * Forwards a message to the specified set of recipients.
	 * The authenticated user must be either the sender or receiver of the forwarded message.  
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_FORWARD, method = RequestMethod.POST)
	public @ResponseBody
	MessageToUser forwardMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@PathVariable String messageId, 
			@RequestBody MessageRecipientSet recipients) throws NotFoundException {
		return serviceProvider.getMessageService().forwardMessage(username, messageId, recipients);
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_CONVERSATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getConversation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@PathVariable String messageId, 
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset, 
			HttpServletRequest request) 
					throws NotFoundException {
		MessageSortBy sortBy = MessageSortBy.valueOf(orderBy.toUpperCase());
		return serviceProvider.getMessageService().getConversation(username, messageId, sortBy, descending, limit, offset, request.getServletPath() + UrlHelpers.MESSAGE_ID_CONVERSATION);
	}
	
	/**
	 * Updates the current status of a message relative to the current authenticated user.
	 * Note: the "recipientId" field of the request body will be ignored.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_STATUS, method = RequestMethod.PUT)
	public void updateMessageStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestBody MessageStatus status) throws NotFoundException {
		serviceProvider.getMessageService().updateMessageStatus(username, status);
	}
	
	/**
	 * Deletes a message.  Only accessible to administrators.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID, method = RequestMethod.DELETE)
	public void deleteMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@PathVariable String messageId) throws NotFoundException {
		serviceProvider.getMessageService().deleteMessage(username, messageId);
	}
}
