package org.sagebionetworks.repo.model;

import java.util.List;

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
			String messageUrl, long userId);

	/**
	 * Get a discussion thread
	 * 
	 * @param threadId
	 * @param userId
	 * @return
	 */
	public DiscussionThreadBundle getThread(long threadId, long userId);

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
	 * @param userId
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreads(long forumId,
			DiscussionOrder order, Integer limit, Integer offset, long userId);

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

	/**
	 * update number of views for the given thread
	 * 
	 * @param threadId
	 * @param numberOfViews
	 */
	public void setNumberOfViews(long threadId, long numberOfViews);

	/**
	 * update number of replies for the given thread
	 * 
	 * @param threadId
	 * @param numberOfReplies
	 */
	public void setNumberOfReplies(long threadId, long numberOfReplies);

	/**
	 * update the last activity of the given thread
	 * 
	 * @param threadId
	 * @param lastActivity
	 */
	public void setLastActivity(long threadId, long lastActivity);

	/**
	 * update active authors for the given thread
	 * 
	 * @param threadId
	 * @param activeAuthors - the top 5 active authors
	 */
	public void setActiveAuthors(long threadId, List<Long> activeAuthors);

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
}
