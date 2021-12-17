package org.sagebionetworks.repo.manager.discussion;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionSearchIndexDao;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@Service
public class DiscussionSearchIndexManagerImpl implements DiscussionSearchIndexManager {

	private static final int MIN_SEARCH_STRING_LENGTH = 3;
	
	private static boolean isThreadDeleted(DiscussionThreadBundle thread) {
		return thread == null || Boolean.TRUE.equals(thread.getIsDeleted());
	}
	
	private static boolean isReplyDeleted(DiscussionReplyBundle reply) {
		return reply == null || Boolean.TRUE.equals(reply.getIsDeleted());
	}
	
	static String buildSearchContent(String ...contentArray) {
		StringBuilder contentBuilder = new StringBuilder();
		
		for (String content : contentArray) {
			contentBuilder
				.append(StringUtils.trimToEmpty(content))
				.append(" ");
		}
		
		return StringUtils.trimToEmpty(contentBuilder.toString());		
	}

	private DiscussionSearchIndexDao searchIndexDao;

	private ForumDAO forumDao;

	private DiscussionThreadDAO threadDao;
	
	private DiscussionReplyDAO replyDao;
	
	private UploadContentToS3DAO contentDao;

	private AuthorizationManager authManager;
	
	@Autowired
	public DiscussionSearchIndexManagerImpl(DiscussionSearchIndexDao searchIndexDao, ForumDAO forumDao, DiscussionThreadDAO threadDao, DiscussionReplyDAO replyDao, UploadContentToS3DAO contentDao, AuthorizationManager authManager) {
		this.searchIndexDao = searchIndexDao;
		this.forumDao = forumDao;
		this.threadDao = threadDao;
		this.replyDao = replyDao;
		this.contentDao = contentDao;
		this.authManager = authManager;
	}

	@Override
	public DiscussionSearchResponse search(UserInfo userInfo, Long forumId, DiscussionSearchRequest request) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(forumId, "The forumId");
		ValidateArgument.required(request, "The search request");
		ValidateArgument.requiredNotBlank(request.getSearchString(), "The request.searchString");
		
		String searchString = buildSearchContent(request.getSearchString());
		
		ValidateArgument.requirement(searchString.length() >= MIN_SEARCH_STRING_LENGTH, "The search string should be at least " + MIN_SEARCH_STRING_LENGTH + " characters.");
		
		Forum forum = forumDao.getForum(forumId);
		
		authManager.canAccess(userInfo, forum.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		
		NextPageToken pageToken = new NextPageToken(request.getNextPageToken());
		
		// Since the getNextPageTokenForCurrentResults might potentially alters the result list (removing the last element) we make sure that the list is mutable
		List<Match> matches = new ArrayList<>(searchIndexDao.search(forumId, searchString, pageToken.getLimitForQuery(), pageToken.getOffset()));
		
		String nextPageToken = pageToken.getNextPageTokenForCurrentResults(matches);
		
		return new DiscussionSearchResponse().setMatches(matches).setNextPageToken(nextPageToken);
	}

	@Override
	@WriteTransaction
	public void processThreadChange(Long threadId) {
		ValidateArgument.required(threadId, "The threadId");
		
		DiscussionThreadBundle thread = null;
		
		try {
			thread = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		} catch (NotFoundException ex) {
			// If a thread does not exists anymore, we can mark it as deleted.
			// Note that this should not happen in general, unless the forum was deleted by cascade from the owning entity
		}
		
		if (isThreadDeleted(thread)) {
			// We mark all the records matching that thread as deleted so they won't appear in the search results
			searchIndexDao.markThreadAsDeleted(threadId);
		} else {
			String title = thread.getTitle();
			String content = fetchContent(thread.getMessageKey());
			
			String searchContent = buildSearchContent(title, content);
			
			Long forumId = Long.valueOf(thread.getForumId());
			
			searchIndexDao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
			// Makes sure that the thread is searchable (e.g. if this is an update due to a restore)
			searchIndexDao.markThreadAsNotDeleted(threadId);
		}
 		
	}

	@Override
	@WriteTransaction
	public void processReplyChange(Long replyId) {
		ValidateArgument.required(replyId, "The replyId");
		
		Long threadId = null;
		DiscussionReplyBundle reply = null;
		DiscussionThreadBundle thread = null;
		
		try {
			reply = replyDao.getReply(replyId, DiscussionFilter.NO_FILTER);
			threadId = Long.valueOf(reply.getThreadId());
			thread = threadDao.getThread(threadId, DiscussionFilter.NO_FILTER);
		} catch (NotFoundException ex) {
			// If a reply does not exists anymore, we can mark it as deleted.
			// Note that this should not happen in general, unless the forum was deleted by cascade from the owning entity
		}
		
		if (isReplyDeleted(reply)) {
			searchIndexDao.markReplyAsDeleted(replyId);
		} else {
			String content = fetchContent(reply.getMessageKey());
			
			String searchContent = buildSearchContent(content);
			
			Long forumId = Long.valueOf(reply.getForumId());
			
			searchIndexDao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
			// Makes sure that the reply is searchable 
			searchIndexDao.markReplyAsNotDeleted(replyId);
		}
		
		if (threadId != null) {
			// If we have a threadId (e.g. the reply was found) we also need to make sure that the thread deleted flag is synced 
			// because the messages of thread and replies are independent and might come out of order.
			// For example if a message for a thread that was deleted comes before the message for a reply in the thread, the record for 
			// the thread would be set as deleted but the record for the message does not exist yet, so a new one with a 
			// default threadDeleted=false would be created
			if (isThreadDeleted(thread)) {
				searchIndexDao.markThreadAsDeleted(threadId);
			} else {
				searchIndexDao.markThreadAsNotDeleted(threadId);
			}
		}
		
	}
		
	private String fetchContent(String messageKey) {
		try {
			return contentDao.getMessage(messageKey);
		} catch (AmazonServiceException ex) {
			// The key does not exists anymore, the search content is empty
			if (ex instanceof AmazonS3Exception && ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return "";
			}
			// Service error can be retried
			if (ErrorType.Service == ex.getErrorType()) {
				throw new RecoverableMessageException(ex);
			}
			throw ex;
		}
	}

}
