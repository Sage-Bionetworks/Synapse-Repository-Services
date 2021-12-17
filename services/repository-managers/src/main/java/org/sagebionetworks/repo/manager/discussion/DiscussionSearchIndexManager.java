package org.sagebionetworks.repo.manager.discussion;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager for operations related to the search index for forums
 */
public interface DiscussionSearchIndexManager {

	/* API methods */
	
	/**
	 * Performs a full text search in the forum with the given id using the query and page token from the given request. The user needs to have read access to the forum. 
	 * Does not return threads or replies that were marked as deleted.
	 * 
	 * @param userInfo The user performing the request
	 * @param forumId The id of the forum to search in
	 * @param request The search request
	 * @return The response will contain a page or results matching the query in the request
	 */
	DiscussionSearchResponse search(UserInfo userInfo, Long forumId, DiscussionSearchRequest request);
	
	/* Worker methods */
		
	/**
	 * When a thread change message is sent and consumed by a worker this method will update the search index accordingly.
	 * In particular if a thread has been marked as deleted all the records for that thread will be marked as deleted so they won't appear in the search results.
	 * If a thread is instead available the thread will be added or updated in the index and all the records associated with that thread will be marked as not deleted. 
	 *  
	 * @param threadId The id of the thread that was changed (created or updated)
	 */
	void processThreadChange(Long threadId) throws RecoverableMessageException;
	
	/**
	 * When a reply change message is send and consumed by a worker this method will updated the search index accordingly.
	 * In particular if a reply has been marked as deleted the record for the reply will be marked as deleted so it won't appear in the search results.
	 * If a reply is instead available the reply will be added or updated in the index and marked as not deleted.
	 * 
	 * Additionally, all the records for the thread of the reply will be marked as deleted or not according to the current status of the thread.
	 *  
	 * @param replyId The id of the reply that was changed (created or updated)
	 */
	void processReplyChange(Long replyId) throws RecoverableMessageException;
	
}
