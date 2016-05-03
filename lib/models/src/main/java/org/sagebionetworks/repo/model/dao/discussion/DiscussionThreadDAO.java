package org.sagebionetworks.repo.model.dao.discussion;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;

public interface DiscussionThreadDAO {

	/**
	 * Create a new discussion thread
	 * 
	 * @param forumId
	 * @param threadId
	 * @param title
	 * @param messageKey
	 * @param userId
	 * @return
	 */
	public DiscussionThreadBundle createThread(String forumId, String threadId,
			String title, String messageKey, long userId);

	/**
	 * Get a discussion thread
	 * 
	 * @param threadId
	 * @param filter
	 * @return
	 */
	public DiscussionThreadBundle getThread(long threadId, DiscussionFilter filter);

	/**
	 * Get the number of discussion thread in a given forum
	 * 
	 * @param forumId
	 * @param filter
	 * @return
	 */
	public long getThreadCount(long forumId, DiscussionFilter filter);

	/**
	 * Get a paginated list of discussion thread for a forum given forumId,
	 * the order of the discussion thread, limit and offset
	 * 
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending,
			DiscussionFilter filter);

	/**
	 * Mark a discussion thread as deleted
	 * 
	 * @param threadId
	 */
	public void markThreadAsDeleted(long threadId);

	/**
	 * Update a discussion thread message
	 * 
	 * @param threadId
	 * @param newMessageKey
	 */
	public DiscussionThreadBundle updateMessageKey(long threadId, String newMessageKey);

	/**
	 * Update a discussion thread title
	 * 
	 * @param threadId
	 * @param title
	 */
	public DiscussionThreadBundle updateTitle(long threadId, String title);

	/**
	 * update number of views for the given thread
	 * 
	 * @param stats
	 */
	public void updateThreadViewStat(List<DiscussionThreadViewStat> stats);

	/**
	 * update number of replies and last activity for the given thread
	 * 
	 * @param stats
	 */
	public void updateThreadReplyStat(List<DiscussionThreadReplyStat> stats);

	/**
	 * update active authors for the given thread
	 * 
	 * @param stats
	 */
	public void updateThreadAuthorStat(List<DiscussionThreadAuthorStat> stats);

	/**
	 * insert ignore a record into THREAD_VIEW table
	 * 
	 * @param threadId
	 * @param userId
	 */
	public void updateThreadView(long threadId, long userId);

	/**
	 * count the number of users who viewed this thread
	 * 
	 * @param threadId
	 */
	public long countThreadView(long threadId);

	/**
	 * Get the etag before attempt to update
	 * 
	 * @param threadId
	 * @return
	 */
	public String getEtagForUpdate(long threadId);

	/**
	 * Get the thread view statistic
	 * 
	 * @Param limit
	 * @param offset
	 * @return
	 */
	public List<DiscussionThreadViewStat> getThreadViewStat(Long limit, Long offset);

	/**
	 * Get all thread Id
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<Long> getAllThreadId(Long limit, Long offset);

	/**
	 * Get all thread Ids for a forum
	 * 
	 * @param forumId
	 * @return
	 */
	public List<String> getAllThreadIdForForum(String forumId);

	/**
	 * Pin a thread
	 * 
	 * @param threadId
	 */
	public void pinThread(long threadId);

	/**
	 * Unpin a thread
	 * @param threadId
	 */
	public void unpinThread(long threadId);
}
