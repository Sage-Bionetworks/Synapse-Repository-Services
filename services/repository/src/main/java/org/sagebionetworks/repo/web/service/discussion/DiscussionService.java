package org.sagebionetworks.repo.web.service.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UserInfo;
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

public interface DiscussionService {

	/**
	 * get forum metadata for the given project
	 * 
	 * @param projectId
	 * @return
	 */
	public Forum getForumByProjectId(UserInfo userInfo, String projectId);

	/**
	 * get forum metadata for the given ID
	 * 
	 * @param forumId
	 * @return
	 */
	public Forum getForum(UserInfo userInfo, String forumId);

	/**
	 * Create a new thread
	 * 
	 * @param toCreate
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread toCreate) throws IOException;

	/**
	 * Get a thread given its ID
	 *
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId);

	/**
	 * Update a thread title
	 * 
	 * @param threadId
	 * @param newTitle
	 * @return
	 */
	public DiscussionThreadBundle updateThreadTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle);

	/**
	 * Update a thread message
	 * 
	 * @param threadId
	 * @param message
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle updateThreadMessage(UserInfo userInfo, String threadId, UpdateThreadMessage message) throws IOException;

	/**
	 * Mark a thread as deleted
	 * 
	 * @param threadId
	 */
	public void markThreadAsDeleted(UserInfo userInfo, String threadId);

	/**
	 * Get limit number of threads starting at offset for a given forum
	 * 
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo,
			String forumId, Long limit, Long offset, DiscussionThreadOrder order,
			Boolean ascending, DiscussionFilter filter);

	/**
	 * Create a new reply
	 * 
	 * @param toCreate
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle createReply(UserInfo userInfo, CreateDiscussionReply toCreate) throws IOException;

	/**
	 * Get a reply given its ID
	 * 
	 * @param replyId
	 * @return
	 */
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId);

	/**
	 * Update a reply's message
	 * 
	 * @param replyId
	 * @param message
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo,
			String replyId, UpdateReplyMessage message) throws IOException;

	/**
	 * Mark a reply as deleted
	 * 
	 * @param replyId
	 */
	public void markReplyAsDeleted(UserInfo userInfo, String replyId);

	/**
	 * Get replies for a given thread ID
	 * 
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionReplyBundle> getReplies(UserInfo userInfo,
			String threadId, Long limit, Long offset, DiscussionReplyOrder order,
			Boolean ascending, DiscussionFilter filter);

	/**
	 * Get the message Url of a thread
	 * 
	 * @param messageKey
	 * @return
	 */
	public MessageURL getThreadUrl(UserInfo userInfo, String messageKey);

	/**
	 * Get the message Url of a reply
	 *
	 * @param messageKey
	 * @return
	 */
	public MessageURL getReplyUrl(UserInfo userInfo, String messageKey);

	/**
	 * Get the total number of threads for a given fourmId
	 * 
	 * @param forumId
	 * @param filter
	 * @return
	 */
	public ThreadCount getThreadCount(UserInfo userInfo, String forumId, DiscussionFilter filter);

	/**
	 * Get the total number of replies for a given threadId
	 * 
	 * @param threadId
	 * @param filter
	 * @return
	 */
	public ReplyCount getReplyCount(UserInfo userInfo, String threadId, DiscussionFilter filter);

	/**
	 * Pin a thread 

	 * @param threadId
	 */
	public void pinThread(UserInfo userInfo, String threadId);

	/**
	 * Unpin a thread
	 * 
	 * @param threadId
	 */
	public void unpinThread(UserInfo userInfo, String threadId);

	/**
	 * Get limit number of threads starting at offset for a given entityId
	 * 
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(UserInfo userInfo, String entityId, Long limit,
			Long offset, DiscussionThreadOrder order, Boolean ascending);

	/**
	 * Get EntityThreadCounts for a list of entityIds.
	 * 
	 * @param userInfo
	 * @param entityIds
	 * @return
	 */
	public EntityThreadCounts getThreadCounts(UserInfo userInfo, EntityIdList entityIds);

	/**
	 * Mark a reply as not deleted
	 * 
	 * @param replyId
	 */
	public void markThreadAsNotDeleted(UserInfo userInfo, String threadId);

	/**
	 * Retrieve a paginated list of moderator Ids
	 * 
	 * param forumId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedIds getModerators(UserInfo userInfo, String forumId, Long limit, Long offset);
}
