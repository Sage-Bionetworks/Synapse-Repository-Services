package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;

public interface DiscussionThreadManager {

	/**
	 * Create a new thread
	 * 
	 * @param userInfo
	 * @param createThread
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) throws IOException;

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
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle);

	/**
	 * Update the message of a thread
	 * 
	 * @param userInfo
	 * @param threadId
	 * @param message
	 * @return
	 * @throws IOException 
	 */
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, UpdateThreadMessage message) throws IOException;

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
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo, String forumId, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending );

	/**
	 * Get message Url for a thread
	 * 
	 * @param user
	 * @param messageKey
	 */
	public String getMessageUrl(UserInfo user, String messageKey);
}
