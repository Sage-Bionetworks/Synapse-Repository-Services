package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import org.sagebionetworks.util.ValidateArgument;

public class DiscussionReplyUtils {

	/**
	 * Create a new DBODiscussionReply
	 * 
	 * @param threadId
	 * @param messageKey
	 * @param userId
	 * @param id
	 * @param etag
	 * @return
	 */
	public static DBODiscussionReply createDBO(String threadId,
			String messageKey, Long userId, Long id, String etag) {
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(messageKey, "messageKey cannot be null");
		ValidateArgument.required(userId, "userId cannot be null");
		ValidateArgument.required(id, "id cannot be null");
		ValidateArgument.required(etag, "etag cannot be null");
		DBODiscussionReply dbo = new DBODiscussionReply();
		dbo.setId(id);
		dbo.setThreadId(Long.parseLong(threadId));
		dbo.setMessageKey(messageKey);
		dbo.setCreatedBy(userId);
		dbo.setEtag(etag);
		dbo.setIsEdited(false);
		dbo.setIsDeleted(false);
		return dbo;
	}

}
