package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
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
 * <p>Discussions in Synapse are captured in the Project's Forum. Each 
 * Project has a Forum. Each Forum has a set of Moderators. The Moderators manage 
 * the content of the Forum.</p>
 * <br>
 * <p>A Forum has multiple Threads. A Thread is created by an authorized user. 
 * Other authorized users can view and reply to an existing Thread.</p>
 * <br>
 * <p>These services provide the APIs for Moderators and authorized users to 
 * create, edit, and manage the conversations that happen in Synapse.</p>
 * <br>
 */
@ControllerInfo(displayName = "Discussion Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DiscussionController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * This API is used to get the Forum's metadata for a given project ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param projectId - The ID of the project to which the forum belongs.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_PROJECT_ID, method = RequestMethod.GET)
	public @ResponseBody Forum getForumMetadata(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String projectId) throws DatastoreException, NotFoundException {
		return serviceProvider.getDiscussionService().getForumMetadata(userId, projectId);
	}

	/**
	 * This API is used to get N number of threads for a given forum ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 100.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param sort - The field to sort the resulting threads on
	 * @param ascending - The direction of sort: true for ascending, and false for descending
	 * @param forumId - The forum ID to which the returning threads belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_FORUM_ID_THREADS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<DiscussionThreadBundle> getThreads(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false) DiscussionThreadOrder order,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false) Boolean ascending,
			@PathVariable String forumId) {
		return serviceProvider.getDiscussionService().getThreads(userId, forumId, limit, offset, order, ascending);
	}

	/**
	 * This API is used to create a new thread in a forum.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param toCreate - This object contains information needed to create a thread
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.THREAD, method = RequestMethod.POST)
	public @ResponseBody DiscussionThreadBundle createThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateDiscussionThread toCreate) throws IOException {
		return serviceProvider.getDiscussionService().createThread(userId, toCreate);
	}

	/**
	 * This API is used to get a thread and its statistic given its ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param threadId - The ID of the thread being requested
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID, method = RequestMethod.GET)
	public @ResponseBody DiscussionThreadBundle getThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) {
		return serviceProvider.getDiscussionService().getThread(userId, threadId);
	}

	/**
	 * This API is used to update the title of a thread.
	 * <br/>
	 * Target users: only the author of the thread can update its title.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param threadId - The ID of the thread being updated
	 * @param title - The new title
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_TITLE, method = RequestMethod.PUT)
	public @ResponseBody DiscussionThreadBundle updateThreadTitle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId,
			@RequestBody UpdateThreadTitle title) {
		return serviceProvider.getDiscussionService().updateThreadTitle(userId, threadId, title);
	}

	/**
	 * This API is used to update the message of a thread.
	 * <br/>
	 * Target users: only the author of the thread can update its message.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param threadId - The ID of the thread being updated
	 * @param message - The new message
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_MESSAGE, method = RequestMethod.PUT)
	public @ResponseBody DiscussionThreadBundle updateThreadMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId,
			@RequestBody UpdateThreadMessage message) throws IOException {
		return serviceProvider.getDiscussionService().updateThreadMessage(userId, threadId, message);
	}

	/**
	 * This API is used to mark a thread as deleted.
	 * <br/>
	 * Target users: only forum's moderator can mark a thread as deleted.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param threadId - the ID of the thread being marked as deleted
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID, method = RequestMethod.DELETE)
	public void markThreadAsDeleted(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) {
		serviceProvider.getDiscussionService().markThreadAsDeleted(userId, threadId);
	}
}
