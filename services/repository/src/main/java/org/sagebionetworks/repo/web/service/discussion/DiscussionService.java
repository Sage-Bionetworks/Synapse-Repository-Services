package org.sagebionetworks.repo.web.service.discussion;

import java.io.UnsupportedEncodingException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;

public interface DiscussionService {

	/**
	 * get forum metadata for the given project
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 */
	public Forum getForumMetadata(Long userId, String projectId);

	/**
	 * Create a new thread
	 * 
	 * @param userId
	 * @param toCreate
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public DiscussionThreadBundle createThread(Long userId, CreateDiscussionThread toCreate) throws UnsupportedEncodingException;

	/**
	 * Get a thread given its ID
	 * 
	 * @param userId
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadBundle getThread(Long userId, String threadId);

	/**
	 * Update a thread title
	 * 
	 * @param userId
	 * @param threadId
	 * @param newTitle
	 * @return
	 */
	public DiscussionThreadBundle updateThreadTitle(Long userId, String threadId, UpdateThreadTitle newTitle);

	/**
	 * Update a thread message
	 * 
	 * @param userId
	 * @param threadId
	 * @param message
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public DiscussionThreadBundle updateThreadMessage(Long userId, String threadId, UpdateThreadMessage message) throws UnsupportedEncodingException;

	/**
	 * Mark a thread as deleted
	 * 
	 * @param userId
	 * @param threadId
	 */
	public void markThreadAsDeleted(Long userId, String threadId);

	/**
	 * Get limit number of threads starting at offset for a given forum
	 * 
	 * @param userId
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreads(Long userId,
			String forumId, Long limit, Long offset, DiscussionOrder order, Boolean ascending);
}
