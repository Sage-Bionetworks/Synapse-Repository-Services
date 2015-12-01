package org.sagebionetworks.repo.manager.discussion;

import java.io.UnsupportedEncodingException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;

public interface DiscussionThreadManager {

	/**
	 * Create a new thread
	 * 
	 * @param userInfo
	 * @param createThread
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) throws UnsupportedEncodingException;

	/**
	 * Get a thread given its Id
	 * 
	 * @param userInfo
	 * @param threadId
	 * @return
	 */
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId);

	/**
	 * Update the thread's title
	 * 
	 * @param userInfo
	 * @param threadId
	 * @param newTitle
	 * @return
	 */
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, String newTitle);

	/**
	 * Update the message of a thread
	 * 
	 * @param userInfo
	 * @param threadId
	 * @param markdown
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, String markdown) throws UnsupportedEncodingException;

	/**
	 * Mark a thread as deleted
	 * 
	 * @param userInfo
	 * @param threadId
	 */
	public void markThreadAsDeleted(UserInfo userInfo, String threadId);

	/**
	 * Get threads that belongs to forumId
	 * 
	 * @param userInfo
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo, String forumId, Long limit, Long offset, DiscussionOrder order, Boolean ascending );
}
