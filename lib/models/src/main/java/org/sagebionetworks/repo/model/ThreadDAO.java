package org.sagebionetworks.repo.model;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.Thread;

public interface ThreadDAO {

	/**
	 * Create a new Thread
	 * 
	 * @param dto
	 * @return
	 */
	public Thread createThread(Thread dto);

	/**
	 * Get a Thread
	 * 
	 * @param threadId
	 * @return
	 */
	public Thread getThread(long threadId);

	/**
	 * Get a paginated list of Threads for a forum given forumId, the order of 
	 * the Thread, limit and offset
	 * 
	 * @param forumId
	 * @param order
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<Thread> getThreads(long forumId, DiscussionOrder order, long limit, long offset);

	/**
	 * Mark a Thread as deleted
	 * 
	 * @param threadId
	 */
	public void deleteThread(long threadId);

	/**
	 * Update a Thread's message
	 * 
	 * @param threadId
	 * @param newMessageUrl
	 */
	public Thread updateMessageUrl(long threadId, String newMessageUrl);

	/**
	 * Update a Thread's title
	 * 
	 * @param threadId
	 * @param title
	 */
	public Thread updateTitle(long threadId, byte[] title);
}
