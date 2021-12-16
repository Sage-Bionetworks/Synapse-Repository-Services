package org.sagebionetworks.repo.manager.discussion;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;
import org.sagebionetworks.repo.model.message.ChangeType;

/**
 * Manager for operations related to the search index for forums
 */
public interface DiscussionSearchIndexManager {

	/* API methods */
	
	/**
	 * 
	 * @param userInfo
	 * @param forumId
	 * @param request
	 * @return
	 */
	DiscussionSearchResponse search(UserInfo userInfo, Long forumId, DiscussionSearchRequest request);
	
	/* Worker methods */
	
	// When a thread is created or updated they need to be (re)indexed
	
	// A reply can be "soft" deleted, this would send an update message and we should check if the reply is marked as deleted in which case the
	// record should mark the reply as deleted.
	
	// A thread can be "soft" deleted, this would send an update message and we should check if the thread was deleted in which case
	// all the records for that thread will be marked as deleted.
	
	// A thread can also be restored, this would send an update message. In this case we need to make sure all the records matching the thread as "restored" (e.g. marked as not deleted).
		
	void processThreadChange(Long threadId, ChangeType changeType);
	
	void processReplyChange(Long replyId, ChangeType changeType);
	
}
