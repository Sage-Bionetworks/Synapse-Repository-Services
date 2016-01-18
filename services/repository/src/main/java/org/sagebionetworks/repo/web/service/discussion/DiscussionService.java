package org.sagebionetworks.repo.web.service.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
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
	public Forum getForumMetadata(Long userId, String projectId);

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
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreads(Long userId,
			String forumId, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending);

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
	 * @return
	 */
	public PaginatedResults<DiscussionReplyBundle> getReplies(Long userId,
			String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending);

	/**
	 * Get the message Url of a thread
	 * 
	 * @param userId
	 * @param threadId
	 * @return
	 */
	public MessageURL getThreadUrl(Long userId, String threadId);

	/**
	 * Get the message Url of a reply
	 * 
	 * @param userId
	 * @param replyId
	 * @return
	 */
	public MessageURL getReplyUrl(Long userId, String replyId);
}
