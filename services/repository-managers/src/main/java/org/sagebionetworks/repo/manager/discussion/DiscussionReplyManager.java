package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;

public interface DiscussionReplyManager {

	/**
	 * Creating a new reply
	 * 
	 * @param userInfo
	 * @param createReply
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle createReply(UserInfo userInfo, CreateDiscussionReply createReply) throws IOException;

	/**
	 * Get a reply by its ID
	 * 
	 * @param userInfo
	 * @param replyId
	 * @return
	 */
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId);

	/**
	 * Update a reply message
	 * 
	 * @param userInfo
	 * @param replyId
	 * @param newMessage
	 * @return
	 * @throws IOException 
	 */
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo, String replyId, UpdateReplyMessage newMessage) throws IOException;

	/**
	 * Mark a reply as deleted
	 * 
	 * @param userInfo
	 * @param replyId
	 */
	public void markReplyAsDeleted(UserInfo userInfo, String replyId);

	/**
	 * Get a thread's replies
	 * 
	 * @param userInfo
	 * @param threadId
	 * @param limit
	 * @param offset
	 * @param order
	 * @param ascending
	 * @return
	 */
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(UserInfo userInfo, String threadId, Long limit, Long offset, DiscussionReplyOrder order, Boolean ascending);

	/**
	 * Get message Url for a reply
	 * 
	 * @param user
	 * @param replyId
	 */
	public MessageURL getMessageUrl(UserInfo user, String replyId);
}
