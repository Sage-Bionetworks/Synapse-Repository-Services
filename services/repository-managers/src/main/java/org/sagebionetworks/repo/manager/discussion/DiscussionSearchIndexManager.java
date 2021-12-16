package org.sagebionetworks.repo.manager.discussion;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;

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
	
	// A reply can be deleted, this would send an update message and we should check if the reply is marked as deleted in which case the
	// record should be removed. Note that a reply cannot be "restored" atm
	
	// A thread can be deleted, this would send an update message and we should check if the thread was deleted in which case
	// all the records for that thread will be removed (including the reply records)
	
	// A thread can also be restored, this would send an update message. In this case we might need to re-index all the replies of the thread (since when marking the thread as deleted we would end up deleting all the
	// replies from the index). Since the change message machinery does not include what changed we could send an update message for each reply of a thread each time a thread is updated (considering doing this downstream).
	
	// Note the race condition (change messages are not in order): if a reply message comes after a thread is deleted we would re-index the reply. For each reply we need to check if the thread is deleted 
	// (in which case we can remove it from the index unconditionally).
	
	
	
}
