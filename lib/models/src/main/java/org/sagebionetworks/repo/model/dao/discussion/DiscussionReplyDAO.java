package org.sagebionetworks.repo.model.dao.discussion;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;

public interface DiscussionReplyDAO {

	/**
	 * Create a new Reply
	 * 
	 * @param threadId
	 * @param replyId
	 * @param messageKey
	 * @param userId
	 * @return
	 */
	public DiscussionReplyBundle createReply(String threadId, String replyId,
			String messageKey, Long userId);

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
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(Long threadId,
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

	/**
	 * Get the thread statistic about reply
	 * 
	 * @param limit - the maximum number of results return in one call. The default and maximum limit is 100.
	 * @param offset
	 * @return
	 */
	public List<DiscussionThreadReplyStat> getThreadReplyStat(Long limit, Long offset);

	/**
	 * Get the top 5 contributors for this thread
	 * 
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadAuthorStat getDiscussionThreadAuthorStat(long threadId);
}
