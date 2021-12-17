package org.sagebionetworks.repo.model.dbo.dao.discussion;

import java.util.List;

import org.sagebionetworks.repo.model.discussion.Match;

public interface DiscussionSearchIndexDao {
	
	/**
	 * Creates a search index record for a thread, if a record exists already will update the search content. 
	 * <br/>
	 * Note: the threadDeleted and replyDeleted markers are not updated and set to false on creation.
	 * 
	 * @param forumId
	 * @param threadId
	 * @param searchContent
	 */
	void createOrUpdateRecordForThread(Long forumId, Long threadId, String searchContent);
	
	/**
	 * Creates a search index record for a reply, if a record exists already will update the search content. 
	 * <br/> 
	 * Note: the threadDeleted and replyDeleted markers are not updated and set to false on creation.
	 * 
	 * @param forumId
	 * @param threadId
	 * @param replyId
	 * @param searchContent
	 */
	void createOrUpdateRecordForReply(Long forumId, Long threadId, Long replyId, String searchContent);
	
	/**
	 * Marks the threadDeleted flag as true for all the records matching the given threadId.
	 * 
	 * @param threadId
	 */
	void markThreadAsDeleted(Long threadId);
	
	/**
	 * Marks the threadDeleted flag as false for all the records matching the given threadId.
	 * 
	 * @param threadId
	 */
	void markThreadAsNotDeleted(Long threadId);
	
	/**
	 * Marks the replyDeleted flag as true for all the records matching the given replyId.
	 *  
	 * @param replyId
	 */
	void markReplyAsDeleted(Long replyId);
	
	/**
	 * Marks the replyDeleted flag as false for all the records matching the given replyId.
	 * 
	 * @param replyId
	 */
	void markReplyAsNotDeleted(Long replyId);
	
	/**
	 * Performs a full text search in the forum with the given id, return a page of results ranked by relevance.
	 * 
	 * Excludes any thread or reply that is marked as deleted.
	 * 
	 * @param searchString
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Match> search(Long forumId, String searchString, long limit, long offset);
}
