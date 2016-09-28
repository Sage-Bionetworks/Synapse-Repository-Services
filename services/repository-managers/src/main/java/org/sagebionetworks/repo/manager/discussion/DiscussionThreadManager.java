package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
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
	 * Mark a thread as not deleted
	 * 
	 * @param userInfo
	 * @param threadId
	 */
	public void markThreadAsNotDeleted(UserInfo userInfo, String threadId);

	/**
	 * Get threads that belongs to forumId
	 * 
	 * @param userInfo
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @param filter 
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo, String forumId, Long limit, Long offset, DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter);

	/**
	 * Get number of threads a forum has
	 * 
	 * @param userInfo
	 * @param forumId
	 * @param filter 
	 * @return
	 */
	public ThreadCount getThreadCountForForum(UserInfo userInfo, String forumId, DiscussionFilter filter);


	/**
	 * Get message Url for a thread
	 * 
	 * @param user
	 * @param messageKey
	 */
	public MessageURL getMessageUrl(UserInfo user, String messageKey);

	/**
	 * Pin a thread
	 * 
	 * @param userInfo
	 * @param threadId
	 */
	public void pinThread(UserInfo userInfo, String threadId);

	/**
	 * unpin a thread
	 * 
	 * @param userInfo
	 * @param threadId
	 */
	public void unpinThread(UserInfo userInfo, String threadId);

	/**
	 * Check to see if the user has read permission on this thread
	 * 
	 * @param userInfo
	 * @param threadId
	 * @param accessType
	 */
	void checkPermission(UserInfo userInfo, String threadId, ACCESS_TYPE accessType);

	/**
	 *Get threads that belongs to projects user can view and references the given entity
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(UserInfo user, String entityId, Long limit,
			Long offset, DiscussionThreadOrder order, Boolean ascending);

	/**
	 * Get list of entity and count pairs, with count is the number of threads
	 *  that belongs to projects user can view and references the given entity.
	 * 
	 * @param user
	 * @param entityIds
	 * @return
	 */
	public EntityThreadCounts getEntityThreadCounts(UserInfo user, EntityIdList entityIds);

	/**
	 * Retrieve a page of moderators for a given forumID
	 * 
	 * @param user
	 * @param forumId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedIds getModerators(UserInfo user, String forumId, Long limit, Long offset);
}
