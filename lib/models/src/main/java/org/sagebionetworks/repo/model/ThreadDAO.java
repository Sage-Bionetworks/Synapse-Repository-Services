package org.sagebionetworks.repo.model;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThread;

public interface ThreadDAO {

	/**
	 * Create a new DiscussionThread
	 * 
	 * @param dto
	 * @return
	 */
	public DiscussionThread createThread(DiscussionThread dto);

	/**
	 * Get a DiscussionThread
	 * 
	 * @param threadId
	 * @return
	 */
	public DiscussionThread getThread(long threadId);

	/**
	 * Get the number of DiscussionThread in a given forum
	 * 
	 * @param forumId
	 * @return
	 */
	public long getThreadCount(long forumId);

	/**
	 * Get a paginated list of DiscussionThread for a forum given forumId, the order of 
	 * the DiscussionThread, limit and offset
	 * 
	 * @param forumId
	 * @param order
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<DiscussionThread> getThreads(long forumId, DiscussionOrder order, Integer limit, Integer offset);

	/**
	 * Mark a DiscussionThread as deleted
	 * 
	 * @param threadId
	 */
	public void deleteThread(long threadId);

	/**
	 * Update a DiscussionThread message
	 * 
	 * @param threadId
	 * @param newMessageUrl
	 */
	public DiscussionThread updateMessageUrl(long threadId, String newMessageUrl);

	/**
	 * Update a DiscussionThread title
	 * 
	 * @param threadId
	 * @param title
	 */
	public DiscussionThread updateTitle(long threadId, byte[] title);
}
