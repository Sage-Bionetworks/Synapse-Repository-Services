package org.sagebionetworks.repo.manager.discussion;

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
	 */
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread);

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
	 */
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, String markdown);

	/**
	 * Mark a thread as deleted
	 * 
	 * @param userInfo
	 * @param threadId
	 */
	public void deleteThread(UserInfo userInfo, String threadId);

	/**
	 * Get threads that belongs to forumId
	 * 
	 * @param userInfo
	 * @param forumId
	 * @param order
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo, String forumId, DiscussionOrder order, Integer limit, Integer offset);
}
