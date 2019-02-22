package org.sagebionetworks.repo.web.service.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
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
	 * @param userId
	 * @param projectId
	 * @return
	 */
	public Forum getForumByProjectId(Long userId, String projectId);

	/**
	 * get forum metadata for the given ID
	 * 
	 * @param userId
	 * @param forumId
	 * @return
	 */
	public Forum getForum(Long userId, String forumId);

	/**
	 * Create a new thread
	 * 
	 * @param userId
	 * @param toCreate
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle createThread(Long userId, CreateDiscussionThread toCreate) throws IOException;

	/**
	 * Get a thread given its ID
	 * 
	 * @param userId
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadBundle getThread(Long userId, String threadId);

	/**
	 * Update a thread title
	 * 
	 * @param userId
	 * @param threadId
	 * @param newTitle
	 * @return
	 */
	public DiscussionThreadBundle updateThreadTitle(Long userId, String threadId, UpdateThreadTitle newTitle);

	/**
	 * Update a thread message
	 * 
	 * @param userId
	 * @param threadId
	 * @param message
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle updateThreadMessage(Long userId, String threadId, UpdateThreadMessage message) throws IOException;

	/**
	 * Mark a thread as deleted
	 * 
	 * @param userId
	 * @param threadId
	 */
	public void markThreadAsDeleted(Long userId, String threadId);

	/**
	 * Get limit number of threads starting at offset for a given forum
	 * 
	 * @param userId
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(Long userId,
			String forumId, Long limit, Long offset, DiscussionThreadOrder order,
			Boolean ascending, DiscussionFilter filter);

	/**
	 * Create a new reply
	 * 
	 * @param userId
	 * @param toCreate
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle createReply(Long userId, CreateDiscussionReply toCreate) throws IOException;

	/**
	 * Get a reply given its ID
	 * 
	 * @param userId
	 * @param replyId
	 * @return
	 */
	public DiscussionReplyBundle getReply(Long userId, String replyId);

	/**
	 * Update a reply's message
	 * 
	 * @param userId
	 * @param replyId
	 * @param message
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle updateReplyMessage(Long userId,
			String replyId, UpdateReplyMessage message) throws IOException;

	/**
	 * Mark a reply as deleted
	 * 
	 * @param userId
	 * @param replyId
	 */
	public void markReplyAsDeleted(Long userId, String replyId);

	/**
	 * Get replies for a given thread ID
	 * 
	 * @param userId
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionReplyBundle> getReplies(Long userId,
			String threadId, Long limit, Long offset, DiscussionReplyOrder order,
			Boolean ascending, DiscussionFilter filter);

	/**
	 * Get the message Url of a thread
	 * 
	 * @param userId
	 * @param messageKey
	 * @return
	 */
	public MessageURL getThreadUrl(Long userId, String messageKey);

	/**
	 * Get the message Url of a reply
	 * 
	 * @param userId
	 * @param messageKey
	 * @return
	 */
	public MessageURL getReplyUrl(Long userId, String messageKey);

	/**
	 * Get the total number of threads for a given fourmId
	 * 
	 * @param userId
	 * @param forumId
	 * @param filter
	 * @return
	 */
	public ThreadCount getThreadCount(Long userId, String forumId, DiscussionFilter filter);

	/**
	 * Get the total number of replies for a given threadId
	 * 
	 * @param userId
	 * @param threadId
	 * @param filter
	 * @return
	 */
	public ReplyCount getReplyCount(Long userId, String threadId, DiscussionFilter filter);

	/**
	 * Pin a thread 
	 * @param userId
	 * @param threadId
	 */
	public void pinThread(Long userId, String threadId);

	/**
	 * Unpin a thread
	 * @param userId
	 * @param threadId
	 */
	public void unpinThread(Long userId, String threadId);

	/**
	 * Get limit number of threads starting at offset for a given entityId
	 * 
	 * @param userId
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(Long userId, String entityId, Long limit,
			Long offset, DiscussionThreadOrder order, Boolean ascending);

	/**
	 * Get EntityThreadCounts for a list of entityIds.
	 * 
	 * @param userId
	 * @param entityIds
	 * @return
	 */
	public EntityThreadCounts getThreadCounts(Long userId, EntityIdList entityIds);

	/**
	 * Mark a reply as not deleted
	 * 
	 * @param userId
	 * @param replyId
	 */
	public void markThreadAsNotDeleted(Long userId, String threadId);

	/**
	 * Retrieve a paginated list of moderator Ids
	 * 
	 * @param userId
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedIds getModerators(Long userId, String forumId, Long limit, Long offset);
}
