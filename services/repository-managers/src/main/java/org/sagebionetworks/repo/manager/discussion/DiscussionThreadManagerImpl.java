package org.sagebionetworks.repo.manager.discussion;

import java.util.UUID;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadManagerImpl implements DiscussionThreadManager {
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private AuthorizationManager authorizationManager;

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) {
		ValidateArgument.requirement(createThread != null, "createThread cannot be null");
		ValidateArgument.requirement(createThread.getForumId() != null, "forumId can not be null");
		ValidateArgument.requirement(createThread.getTitle() != null, "title cannot be null");
		ValidateArgument.requirement(createThread.getMessageMarkdown() != null, "message cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(createThread.getForumId())).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		String messageUrl = uploadDao.upload(createThread.getMessageMarkdown(), UUID.randomUUID().toString());
		return threadDao.createThread(createThread.getForumId(), createThread.getTitle(), messageUrl, userInfo.getId());
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		if (threadId == null) {
			throw new IllegalArgumentException("threadId cannot be null");
		}
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return thread;
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, String newTitle) {
		if (threadId == null) {
			throw new IllegalArgumentException("threadId cannot be null");
		}
		if (newTitle == null) {
			throw new IllegalArgumentException("newTitle cannot be null");
		}
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			return threadDao.updateTitle(threadIdLong, newTitle);
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, String markdown) {
		if (threadId == null) {
			throw new IllegalArgumentException("threadId cannot be null");
		}
		if (markdown == null) {
			throw new IllegalArgumentException("markdown cannot be null");
		}
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			String messageUrl = uploadDao.upload(markdown, UUID.randomUUID().toString());
			return threadDao.updateMessageUrl(threadIdLong, messageUrl);
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransaction
	@Override
	public void markThreadAsDeleted(UserInfo userInfo, String threadId) {
		if (threadId == null) {
			throw new IllegalArgumentException("threadId cannot be null");
		}
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		threadDao.markThreadAsDeleted(threadIdLong);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, DiscussionOrder order,
			Integer limit, Integer offset) {
		if (forumId == null) {
			throw new IllegalArgumentException("forumId cannot be null");
		}
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return threadDao.getThreads(Long.parseLong(forumId), order, limit, offset);
	}

}
