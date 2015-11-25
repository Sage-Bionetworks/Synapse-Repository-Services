package org.sagebionetworks.repo.manager.discussion;

import java.util.UUID;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.ObjectType;
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
		if (threadId == null) throw new IllegalArgumentException("threadId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, threadId, ObjectType.DISCUSSION_THREAD, ACCESS_TYPE.READ));
		Long threadIdLong = Long.parseLong(threadId);
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return threadDao.getThread(threadIdLong);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, String newTitle) {
		if (threadId == null) throw new IllegalArgumentException("threadId cannot be null");
		if (newTitle == null) throw new IllegalArgumentException("newTitle cannot be null");
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, threadId, ObjectType.DISCUSSION_THREAD, ACCESS_TYPE.UPDATE));
		return threadDao.updateTitle(Long.parseLong(threadId), newTitle);
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, String markdown) {
		if (threadId == null) throw new IllegalArgumentException("threadId cannot be null");
		if (markdown == null) throw new IllegalArgumentException("markdown cannot be null");
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, threadId, ObjectType.DISCUSSION_THREAD, ACCESS_TYPE.UPDATE));
		String messageUrl = uploadDao.upload(markdown, UUID.randomUUID().toString());
		return threadDao.updateMessageUrl(Long.parseLong(threadId), messageUrl);
	}

	@WriteTransaction
	@Override
	public void deleteThread(UserInfo userInfo, String threadId) {
		if (threadId == null) throw new IllegalArgumentException("threadId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, threadId, ObjectType.DISCUSSION_THREAD, ACCESS_TYPE.DELETE));
		threadDao.deleteThread(Long.parseLong(threadId));
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, DiscussionOrder order,
			Integer limit, Integer offset) {
		if (forumId == null) throw new IllegalArgumentException("forumId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		PaginatedResults<DiscussionThreadBundle> results = threadDao.getThreads(Long.parseLong(forumId), order, limit, offset);
		return results;
	}

}
