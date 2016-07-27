package org.sagebionetworks.repo.model.dao.discussion;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadAuthorStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadReplyStat;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadViewStat;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;

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
	public long getThreadCountForForum(long forumId, DiscussionFilter filter);

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

	/**
	 * Return the projectID that this thread belong to
	 * 
	 * @param threadId
	 * @return
	 */
	public String getProjectId(String threadId);

	/**
	 * Return the author of the thread
	 * 
	 * @param threadId
	 * @return
	 */
	public String getAuthorForUpdate(String threadId);

	/**
	 * 
	 * @param threadId
	 * @return true is the thread has been marked as deleted,
	 *         false otherwise.
	 */
	public boolean isThreadDeleted(String threadId);

	/**
	 * 
	 * @param entityIds
	 * @return number of threads that mentioned a particular entity for a list of entity
	 */
	public EntityThreadCounts getThreadCounts(List<String> entityIds);

	/**
	 * 
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter
	 * @return a paginated list of threads that mentioned the enityId
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(long entityId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending,
			DiscussionFilter filter);


	/**
	 * Insert a batch of DiscussionThreadEntityReference
	 * 
	 * @param refs
	 */
	public void insertEntityReference(List<DiscussionThreadEntityReference> refs);
}
