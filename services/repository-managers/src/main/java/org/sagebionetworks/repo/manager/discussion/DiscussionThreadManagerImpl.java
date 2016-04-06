package org.sagebionetworks.repo.manager.discussion;

import static org.sagebionetworks.repo.manager.AuthorizationManagerImpl.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.upload.discussion.MessageKeyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadManagerImpl implements DiscussionThreadManager {

	private static final DiscussionFilter DEFAULT_FILTER = DiscussionFilter.NO_FILTER;
	public static final int MAX_TITLE_LENGTH = 140;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) throws IOException {
		ValidateArgument.required(createThread, "createThread");
		ValidateArgument.required(createThread.getForumId(), "CreateDiscussionThread.forumId");
		ValidateArgument.required(createThread.getTitle(), "CreateDiscussionThread.title");
		ValidateArgument.required(createThread.getMessageMarkdown(), "CreateDiscussionThread.messageMarkdown");
		ValidateArgument.requirement(createThread.getTitle().length() <= MAX_TITLE_LENGTH, "Title cannot exceed "+MAX_TITLE_LENGTH+" characters.");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(createThread.getForumId())).getProjectId();
		if (authorizationManager.isAnonymousUser(userInfo)){
			throw new UnauthorizedException(ANONYMOUS_ACCESS_DENIED_REASON);
		}
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		String messageKey = uploadDao.uploadThreadMessage(createThread.getMessageMarkdown(), createThread.getForumId(), id.toString());
		DiscussionThreadBundle thread = threadDao.createThread(createThread.getForumId(), id.toString(), createThread.getTitle(), messageKey, userInfo.getId());
		transactionalMessenger.sendMessageAfterCommit(""+id, ObjectType.THREAD, thread.getEtag(),  ChangeType.CREATE, userInfo.getId());
		handleSubscription(userInfo.getId().toString(), thread.getId(), thread.getForumId());
		return updateNumberOfReplies(thread, DiscussionFilter.NO_FILTER);
	}

	private void handleSubscription(String userId, String threadId, String forumId) {
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		subscriptionDao.subscribeForumSubscriberToThread(forumId, threadId);
	}

	private DiscussionThreadBundle updateNumberOfReplies(DiscussionThreadBundle thread, DiscussionFilter filter) {
		thread.setNumberOfReplies(replyDao.getReplyCount(Long.parseLong(thread.getId()), filter));
		return thread;
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId");
		UserInfo.validateUserInfo(userInfo);
		DiscussionFilter filter = DiscussionFilter.EXCLUDE_DELETED;
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, filter);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return updateNumberOfReplies(thread, filter);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newTitle, "newTitle");
		ValidateArgument.required(newTitle.getTitle(), "UpdateThreadTitle.title");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DEFAULT_FILTER);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			thread = threadDao.updateTitle(threadIdLong, newTitle.getTitle());
			return updateNumberOfReplies(thread, DiscussionFilter.NO_FILTER);
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId,
			UpdateThreadMessage newMessage) throws IOException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newMessage, "newMessage");
		ValidateArgument.required(newMessage.getMessageMarkdown(), "UpdateThreadMessage.messageMarkdown");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DEFAULT_FILTER);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			String messageKey = uploadDao.uploadThreadMessage(newMessage.getMessageMarkdown(), thread.getForumId(), thread.getId());
			thread = threadDao.updateMessageKey(threadIdLong, messageKey);
			return updateNumberOfReplies(thread, DiscussionFilter.NO_FILTER);
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void markThreadAsDeleted(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DEFAULT_FILTER);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		threadDao.markThreadAsDeleted(threadIdLong);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		if (filter.equals(DiscussionFilter.EXCLUDE_DELETED)) {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		} else {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		}
		PaginatedResults<DiscussionThreadBundle> threads = threadDao.getThreads(Long.parseLong(forumId), limit, offset, order, ascending, filter);
		return updateNumberOfReplies(threads, filter);
	}

	private PaginatedResults<DiscussionThreadBundle> updateNumberOfReplies(
			PaginatedResults<DiscussionThreadBundle> threads, DiscussionFilter filter) {
		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		for (DiscussionThreadBundle thread : threads.getResults()) {
			list.add(updateNumberOfReplies(thread, filter));
		}
		threads.setResults(list);
		return threads;
	}

	@Override
	public MessageURL getMessageUrl(UserInfo userInfo, String messageKey) {
		ValidateArgument.required(messageKey, "messageKey");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(MessageKeyUtils.getThreadId(messageKey));
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DEFAULT_FILTER);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return uploadDao.getThreadUrl(thread.getMessageKey());
	}

	@Override
	public ThreadCount getThreadCountForForum(UserInfo userInfo, String forumId, DiscussionFilter filter) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		ThreadCount count = new ThreadCount();
		count.setCount(threadDao.getThreadCount(Long.parseLong(forumId), filter));
		return count;
	}
}
