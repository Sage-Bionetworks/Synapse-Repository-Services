package org.sagebionetworks.repo.model.dao.discussion;

import java.util.List;

import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;

public interface DiscussionReplyDAO {

	/**
	 * Create a new Reply
	 * 
	 * @param threadId
	 * @param messageKey
	 * @param userId
	 * @return
	 */
	public DiscussionReplyBundle createReply(String threadId, String replyId, String messageKey,
			Long userId);

	/**
	 * Get a reply given its ID
	 * 
	 * @param replyId
	 * @param filter
	 * @return
	 */
	public DiscussionReplyBundle getReply(long replyId, DiscussionFilter filter);

	/**
	 * Get replies for a given thread.
	 * If includeDeleted is true, returns all replies found;
	 * otherwise, only returns non-deleted replies.
	 * 
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public List<DiscussionReplyBundle> getRepliesForThread(Long threadId,
			Long limit, Long offset, DiscussionReplyOrder order, Boolean ascending,
			DiscussionFilter filter);

	/**
	 * Get the number of replies for a given thread.
	 * If includeDeleted is true, returns count of all replies found;
	 * otherwise, only returns count of non-deleted replies.
	 * 
	 * @param threadId
	 * @param filter
	 * @return
	 */
	public long getReplyCount(long threadId, DiscussionFilter filter);

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

	/**
	 * Get the thread statistic about its replies
	 * 
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadReplyStat getThreadReplyStat(long threadId);

	/**
	 * Get the projectID which this reply belongs to
	 * 
	 * @param replyId
	 * @return
	 */
	public String getProjectId(String replyId);

	/**
	 * Get the top 5 contributors for this thread
	 * 
	 * @param threadId
	 * @return
	 */
	public List<String> getActiveAuthors(long threadId);
}
