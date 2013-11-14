package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
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
public class MessageController extends BaseController {

	@Autowired
	private ServiceProvider serviceProvider;
	
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_INBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageBundle> getInbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, required = false, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, required = false, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset) 
					throws NotFoundException {
		return serviceProvider.getMessageService().getInbox(username, orderBy, descending, limit, offset);
	}
	
	/**
	 * Retrieves the current authenticated user's outbox.  
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_OUTBOX, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getOutbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, required = false, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, required = false, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset) 
					throws NotFoundException {
		return serviceProvider.getMessageService().getOutbox(username, orderBy, descending, limit, offset);
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_ID_CONVERSATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MessageToUser> getConversation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@PathVariable String messageId, 
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, required = false, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, required = false, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset) 
					throws NotFoundException {
		return serviceProvider.getMessageService().getConversation(username, messageId, orderBy, descending, limit, offset);
	}
	
	/**
	 * Updates the current status of a message relative to the current authenticated user.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE_STATUS, method = RequestMethod.PUT)
	public void updateMessageStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestBody MessageStatus status) throws NotFoundException {
		serviceProvider.getMessageService().updateMessageStatus(username, status);
	}
}
