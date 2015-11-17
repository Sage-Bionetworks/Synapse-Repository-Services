package org.sagebionetworks.repo.model;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;

public interface DiscussionThreadDAO {

	/**
	 * Create a new discussion thread
	 * 
	 * @param forumId
	 * @param title
	 * @param messageUrl
	 * @param userId
	 * @return
	 */
	public DiscussionThreadBundle createThread(String forumId, String title,
			String messageUrl, Long userId);

	/**
	 * Get a discussion thread
	 * 
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadBundle getThread(long threadId);

	/**
	 * Get the number of discussion thread in a given forum
	 * 
	 * @param forumId
	 * @return
	 */
	public long getThreadCount(long forumId);

	/**
	 * Get a paginated list of discussion thread for a forum given forumId,
	 * the order of the discussion thread, limit and offset
	 * 
	 * @param forumId
	 * @param order
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			DiscussionOrder order, Integer limit, Integer offset);

	/**
	 * Mark a discussion thread as deleted
	 * 
	 * @param threadId
	 */
	public void deleteThread(long threadId);

	/**
	 * Update a discussion thread message
	 * 
	 * @param threadId
	 * @param newMessageUrl
	 */
	public DiscussionThreadBundle updateMessageUrl(long threadId, String newMessageUrl);

	/**
	 * Update a discussion thread title
	 * 
	 * @param threadId
	 * @param title
	 */
	public DiscussionThreadBundle updateTitle(long threadId, String title);
}
