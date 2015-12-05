package org.sagebionetworks.repo.model.dao.discussion;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;

public interface DiscussionReplyDAO {

	/**
	 * Create a new Reply
	 * 
	 * @param threadId
	 * @param messageKey
	 * @param userId
	 * @return
	 */
	public DiscussionReplyBundle createReply(String threadId, String messageKey,
			Long userId);

	/**
	 * Get a reply given its ID
	 * 
	 * @param replyId
	 * @return
	 */
	public DiscussionReplyBundle getReply(long replyId);

	/**
	 * Get replies for a given thread
	 * 
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(long threadId,
			Long limit, Long offset, DiscussionReplyOrder order, Boolean ascending);

	/**
	 * Get the number of replies for a given thread
	 * 
	 * @param threadId
	 * @return
	 */
	public long getReplyCount(long threadId);

	/**
	 * Mark a given reply as deleted
	 * 
	 * @param replyId
	 */
	public void markReplyAsDeleted(long replyId);

	/**
	 * Update a reply message
	 * 
	 * @param replyId
	 * @param newKey
	 * @return
	 */
	public DiscussionReplyBundle updateMessageKey(long replyId, String newKey);

	/**
	 * Get the Etag before updating a reply
	 * 
	 * @param replyId
	 * @return
	 */
	public String getEtagForUpdate(long replyId);
}
