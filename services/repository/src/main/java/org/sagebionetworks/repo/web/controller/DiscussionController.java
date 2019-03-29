package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
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
public class DiscussionController {

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
	@RequestMapping(value = UrlHelpers.PROJECT_PROJECT_ID_FORUM, method = RequestMethod.GET)
	public @ResponseBody Forum getForumByProjectId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String projectId) throws DatastoreException, NotFoundException {
		return serviceProvider.getDiscussionService().getForumByProjectId(userId, projectId);
	}

	/**
	 * This API is used to get the Forum's metadata for a given its ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param forumId - The ID of the forum.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_FORUM_ID, method = RequestMethod.GET)
	public @ResponseBody Forum getForumByProject(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String forumId) throws DatastoreException, NotFoundException {
		return serviceProvider.getDiscussionService().getForum(userId, forumId);
	}

	/**
	 * This API is used to get N number of threads for a given forum ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 20.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param sort - The field to sort the resulting threads on. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder}">DiscussionThreadOrder</a>.
	 * @param ascending - The direction of sort: true for ascending, and false for descending
	 * @param filter - Filter deleted/ not deleted threads. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 * @param forumId - The forum ID to which the returning threads belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_FORUM_ID_THREADS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false) DiscussionThreadOrder sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false) Boolean ascending,
			@RequestParam(value = ServiceConstants.FILTER_PARAM) DiscussionFilter filter,
			@PathVariable String forumId) {
		return serviceProvider.getDiscussionService().getThreadsForForum(userId, forumId, limit, offset, sort, ascending, filter);
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

	/**
	 * This API is used to restore a deleted thread.
	 * <br/>
	 * Target users: only forum's moderator can restore a deleted thread.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param threadId - the ID of the thread that was marked as deleted
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_RESTORE, method = RequestMethod.PUT)
	public void restoreDeletedThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) {
		serviceProvider.getDiscussionService().markThreadAsNotDeleted(userId, threadId);
	}

	/**
	 * This API is used to mark a thread as pinned.
	 * <br/>
	 * Target users: only forum's moderator can mark a thread as pinned.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param threadId - the ID of the thread being marked as pinned
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_PIN, method = RequestMethod.PUT)
	public void pinThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) {
		serviceProvider.getDiscussionService().pinThread(userId, threadId);
	}

	/**
	 * This API is used to unpin a thread.
	 * <br/>
	 * Target users: only forum's moderator can unpin a thread.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param threadId - the ID of the thread being unpinned
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_UNPIN, method = RequestMethod.PUT)
	public void unpinThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) {
		serviceProvider.getDiscussionService().unpinThread(userId, threadId);
	}

	/**
	 * This API is used to get the message URL of a thread. The message URL is
	 * the URL to download the file which contains the thread message.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * <p>
 	 * The resulting URL will be signed with Content-Type ="text/plain; charset=utf-8";
 	 * therefore, this header must be included with the GET on the URL.
 	 * </p>
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param threadId - DiscussionThreadBundle.messageKey
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.THREAD_URL, method = RequestMethod.GET)
	public @ResponseBody MessageURL getThreadUrl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String messageKey) {
		return serviceProvider.getDiscussionService().getThreadUrl(userId, messageKey);
	}

	/**
	 * This API is used to create a new reply to a thread.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param toCreate - This object contains information needed to create a reply.
	 * @return
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.REPLY, method = RequestMethod.POST)
	public @ResponseBody DiscussionReplyBundle createReply(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateDiscussionReply toCreate) throws IOException {
		return serviceProvider.getDiscussionService().createReply(userId, toCreate);
	}

	/**
	 * This API is used to get a reply and its statistic given its ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param replyId - The ID of the reply being requested
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REPLY_REPLY_ID, method = RequestMethod.GET)
	public @ResponseBody DiscussionReplyBundle getReply(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String replyId) {
		return serviceProvider.getDiscussionService().getReply(userId, replyId);
	}

	/**
	 * This API is used to update the message of a reply.
	 * <br/>
	 * Target users: only the author of the reply can update its message.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param replyId - The ID of the reply being updated
	 * @param message - The new message
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.REPLY_REPLY_ID_MESSAGE, method = RequestMethod.PUT)
	public @ResponseBody DiscussionReplyBundle updateReplyMessage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String replyId,
			@RequestBody UpdateReplyMessage message) throws IOException {
		return serviceProvider.getDiscussionService().updateReplyMessage(userId, replyId, message);
	}

	/**
	 * This API is used to mark a reply as deleted.
	 * <br/>
	 * Target users: only forum's moderator can mark a reply as deleted.
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param replyId - the ID of the reply being marked as deleted
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.REPLY_REPLY_ID, method = RequestMethod.DELETE)
	public void markReplyAsDeleted(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String replyId) {
		serviceProvider.getDiscussionService().markReplyAsDeleted(userId, replyId);
	}

	/**
	 * This API is used to get N number of replies for a given thread ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 100.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param sort - The field to sort the resulting replies on. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder}">DiscussionReplyOrder</a>.
	 * @param ascending - The direction of sort: true for ascending, and false for descending
	 * @param filter - Filter deleted/ not deleted replies. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 * @param threadId - The thread ID to which the returning replies belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_REPLIES, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false) DiscussionReplyOrder sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false) Boolean ascending,
			@RequestParam(value = ServiceConstants.FILTER_PARAM) DiscussionFilter filter,
			@PathVariable String threadId) {
		return serviceProvider.getDiscussionService().getReplies(userId, threadId, limit, offset, sort, ascending, filter);
	}

	/**
	 * This API is used to get the message URL of a reply. The message URL is
	 * the URL to download the file which contains the reply message.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * <p>
 	 * The resulting URL will be signed with Content-Type ="text/plain; charset=utf-8";
 	 * therefore, this header must be included with the GET on the URL.
 	 * </p>
	 * 
	 * @param userId - the ID of the user who is making the request
	 * @param messageKey - DiscussionReplyBundle.messageKey
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REPLY_URL, method = RequestMethod.GET)
	public @ResponseBody MessageURL getReplyUrl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = true) String messageKey) {
		return serviceProvider.getDiscussionService().getReplyUrl(userId, messageKey);
	}

	/**
	 * This API is used to get the total number of threads for a given forum ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param filter - Filter deleted/ not deleted threads. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 * @param forumId - The forum ID to which the returning threads belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_FORUM_ID_THREAD_COUNT, method = RequestMethod.GET)
	public @ResponseBody ThreadCount getThreadCountForForum(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.FILTER_PARAM) DiscussionFilter filter,
			@PathVariable String forumId) {
		return serviceProvider.getDiscussionService().getThreadCount(userId, forumId, filter);
	}

	/**
	 * This API is used to get the total number of replies for a given thread ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param filter - Filter deleted/ not deleted replies. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 * @param threadId - The thread ID to which the returning replies belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.THREAD_THREAD_ID_REPLY_COUNT, method = RequestMethod.GET)
	public @ResponseBody ReplyCount getReplyCountForThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.FILTER_PARAM) DiscussionFilter filter,
			@PathVariable String threadId) {
		return serviceProvider.getDiscussionService().getReplyCount(userId, threadId, filter);
	}

	/**
	 * This API is used to get N number of threads that belongs to projects user
	 * can view and references the given entity.
	 * <br/>
	 * Target users: anyone who has READ permission to the entity.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 20.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param sort - The field to sort the resulting threads on. Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder}">DiscussionThreadOrder</a>.
	 * @param ascending - The direction of sort: true for ascending, and false for descending
	 * @param id - The request entityId
	 * @return the threads that user has read permission to.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_THREADS, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = true) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = true) Long offset,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false) DiscussionThreadOrder sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false) Boolean ascending,
			@PathVariable String id) {
		return serviceProvider.getDiscussionService().getThreadsForEntity(userId, id, limit, offset, sort, ascending);
	}

	/**
	 * This API is used to get list of entity and count pairs, with count is the
	 * number of threads that belongs to projects user can view and references
	 * the given entity.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param entityIds - The requested list. Limit size 20.
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_THREAD_COUNTS, method = RequestMethod.POST)
	public @ResponseBody EntityThreadCounts getThreadCounts(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody EntityIdList entityIds) {
		return serviceProvider.getDiscussionService().getThreadCounts(userId, entityIds);
	}

	/**
	 * Returns a page of moderators for a given forum ID.
	 * <br/>
	 * Target users: anyone who has READ permission to the project.
	 * 
	 * @param userId - The ID of the user who is making the request
	 * @param limit - Limits the size of the page returned. For example, a page size of 10 require limit = 10. The maximum Limit for this call is 100.
	 * @param offset - The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @param forumId - The forum ID to which the returning mederators belong
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_FORUM_ID_MODERATORS, method = RequestMethod.GET)
	public @ResponseBody PaginatedIds getForumModerators(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM) Long offset,
			@PathVariable String forumId) {
		return serviceProvider.getDiscussionService().getModerators(userId, forumId, limit, offset);
	}
}
