package org.sagebionetworks.repo.model.dao.discussion;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadStat;
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
	public List<DiscussionThreadBundle> getThreadsForForum(long forumId,
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
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter
	 * @param projectIds
	 * @return a list of threads that are in the given projectIds and referenced
	 *  the given enityId
	 */
	public List<DiscussionThreadBundle> getThreadsForEntity(long entityId,
			Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending,
			DiscussionFilter filter, Set<Long> projectIds);

	/**
	 * Insert a batch of references from a thread to an entity
	 * 
	 * @param refs
	 */
	public void insertEntityReference(List<DiscussionThreadEntityReference> refs);

	/**
	 * Get a list of projectIds that threads, which mentioned entityIds, belongs to.
	 * 
	 * @param list
	 * @return
	 */
	public Set<Long> getDistinctProjectIdsOfThreadsReferencesEntityIds(List<Long> list);

	/**
	 * @param entityIds
	 * @param projectIds
	 * @return number of threads, within a range or projects, that mentioned a
	 *  particular entity, for a list of entityIds.
	 */
	public EntityThreadCounts getThreadCounts(List<Long> entityIds, Set<Long> projectIds);

	/**
	 * @param entityId
	 * @param filter
	 * @param projectIds
	 * @return number of threads, within a range or projects, that mentioned a
	 *  particular entity, for a given entityId
	 */
	public long getThreadCountForEntity(long entityId, DiscussionFilter filter, Set<Long> projectIds);

	/**
	 * Update the statistic of a thread
	 * 
	 * @param stats
	 */
	public void updateThreadStats(List<DiscussionThreadStat> stats);

	/**
	 * Mark a discussion thread as not deleted
	 * 
	 * @param threadId
	 */
	public void markThreadAsNotDeleted(long threadId);
}
