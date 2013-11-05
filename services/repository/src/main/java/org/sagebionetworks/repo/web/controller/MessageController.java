package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
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
	 * To chain messages together into a message thread, specify the message you are replying to via the "replyTo" parameter.  
	 * </br>
	 * In most cases, message delivery is asynchronous to message creation.  
	 * i.e. It may take several seconds for a sent message to appear in a user's inbox.
	 * </br>
	 * Note: Unauthorized delivery, such as commenting on a restricted entity, 
	 * may result in a silent failure or a bounce message, depending on your notification settings.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MESSAGE, method = RequestMethod.POST)
	public @ResponseBody
	Message createMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_REPLY_TO_PARAM, required = false) String replyTo, 
			@RequestBody Message toCreate) throws NotFoundException {
		return serviceProvider.getMessageService().create(username, replyTo, toCreate);
	}
	
	/**
	 * Retrieves the current authenticated user's inbox.  
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
	PaginatedResults<Message> getOutbox(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String username,
			@RequestParam(value = UrlHelpers.MESSAGE_ORDER_BY_PARAM, required = false, defaultValue = defaultSortOrder) String orderBy, 
			@RequestParam(value = UrlHelpers.MESSAGE_DESCENDING_PARAM, required = false, defaultValue = defaultSortDescending) boolean descending, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit, 
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset) 
					throws NotFoundException {
		return serviceProvider.getMessageService().getOutbox(username, orderBy, descending, limit, offset);
	}
}
