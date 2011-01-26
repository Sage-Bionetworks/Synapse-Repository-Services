package org.sagebionetworks.repo.web.controller;

import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Message;
import org.sagebionetworks.repo.server.EntityRepository;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for CRUD operations on Message objects
 * <p>
 * 
 * TODO factor this similar to CommentController if we like that pattern
 * 
 * @author deflaux
 */
@Controller
@RequestMapping(UrlHelpers.MESSAGE)
public class MessageController extends BaseController implements
		AbstractEntityController<Message> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(MessageController.class
			.getName());

	private EntityRepository<Message> messageRepository = new EntityRepository<Message>(
			Message.class);

	/**
	 * Get messages
	 * <p>
	 * <ul>
	 * <li>TODO filter by date
	 * <li>TODO more response bread crumb urls when we have proper DTOs
	 * </ul>
	 * 
	 * @param offset
	 *            1-based pagination offset
	 * @param limit
	 *            maximum number of results to return
	 * @param request
	 *            used to form return URLs in the body of the response
	 * @return list of all messages stored in the repository
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "", method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Message> getEntities(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, 
					required = false, 
					defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, 
					required = false, 
					defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, 
					required = false, 
					defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, 
					required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
					HttpServletRequest request) {
		ServiceConstants.validatePaginationParams(offset, limit);
		List<Message> messages = messageRepository.getRange(offset, limit);
		Integer totalNumberOfMessages = messageRepository.getCount();
		return new PaginatedResults<Message>(request.getServletPath()
				+ UrlHelpers.MESSAGE, messages, totalNumberOfMessages, offset,
				limit, sort, ascending);
	}

	/**
	 * Get a specific message
	 * <p>
	 * <ul>
	 * <li>TODO response bread crumb urls when we have proper DTOs
	 * </ul>
	 * 
	 * @param id
	 *            the unique identifier for the message to be returned
	 * @return the message or exception if not found
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	Message getEntity(@PathVariable String id, HttpServletRequest request)
			throws NotFoundException {
		Message message = messageRepository.getById(id);
		if (null == message) {
			throw new NotFoundException("no message with id " + id + " exists");
		}
		return message;
	}

	/**
	 * Create a new message
	 * <p>
	 * <ul>
	 * <li>TODO validate minimum requirements for new message object
	 * <li>TODO response bread crumb urls when we have proper DTOs
	 * </ul>
	 * 
	 * @param newMessage
	 * @return the newly created message
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody
	Message createEntity(@RequestBody Message newMessage,
			HttpServletRequest request) {
		// TODO check newMessage.isValid()
		// newMessage.getValidationErrorMessage()
		messageRepository.create(newMessage);
		return newMessage;
	}

	/**
	 * Update an existing message
	 * <p>
	 * <ul>
	 * <li>TODO validate updated message
	 * <li>TODO response bread crumb urls when we have proper DTOs
	 * </ul>
	 * 
	 * @param id
	 *            the unique identifier for the message to be updated
	 * @param etag
	 *            service-generated value used to detect conflicting updates
	 * @param updatedMessage
	 *            the object with which to overwrite the currently stored
	 *            message
	 * @return the updated message
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	Message updateEntity(@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Message updatedMessage, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException {
		Message message = messageRepository.getById(id);
		if (null == message) {
			throw new NotFoundException("no message with id " + id + " exists");
		}
		if (etag != message.hashCode()) {
			throw new ConflictingUpdateException(
					"message with id "
							+ id
							+ "was updated since you last fetched it, retrieve it again and reapply the update");
		}
		messageRepository.create(updatedMessage);
		return updatedMessage;
	}

	/**
	 * Delete a specific message
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the message to be deleted
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void deleteEntity(@PathVariable String id) throws NotFoundException {
		if (!messageRepository.deleteById(id)) {
			throw new NotFoundException("no message with id " + id + " exists");
		}
		return;
	}

	/**
	 * Simple sanity check test request, using the default view
	 * <p>
	 * 
	 * @param modelMap
	 *            the parameter into which output data is to be stored
	 * @return a dummy hard-coded response
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Messages rocks");
		return ""; // use the default view
	}

}
