package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.util.Date;

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
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(messageKey, "messageKey");
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(id, "id");
		ValidateArgument.required(etag, "etag");
		DBODiscussionReply dbo = new DBODiscussionReply();
		dbo.setId(id);
		dbo.setThreadId(Long.parseLong(threadId));
		dbo.setMessageKey(messageKey);
		dbo.setCreatedBy(userId);
		dbo.setEtag(etag);
		dbo.setIsEdited(false);
		dbo.setIsDeleted(false);
		dbo.setCreatedOn(new Date());
		dbo.setModifiedOn(new Date());
		return dbo;
	}

}
