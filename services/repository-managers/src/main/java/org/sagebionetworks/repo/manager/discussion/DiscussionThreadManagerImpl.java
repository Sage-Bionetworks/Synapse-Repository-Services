package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
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
	@Autowired
	private IdGenerator idGenerator;


	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) throws IOException {
		ValidateArgument.required(createThread, "createThread cannot be null");
		ValidateArgument.required(createThread.getForumId(), "forumId can not be null");
		ValidateArgument.required(createThread.getTitle(), "title cannot be null");
		ValidateArgument.required(createThread.getMessageMarkdown(), "message cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(createThread.getForumId())).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		String messageKey = uploadDao.uploadDiscussionContent(createThread.getMessageMarkdown(), createThread.getForumId(), id.toString());
		return addMessageUrl(threadDao.createThread(createThread.getForumId(), id.toString(), createThread.getTitle(), messageKey, userInfo.getId()));
	}

	private DiscussionThreadBundle addMessageUrl(DiscussionThreadBundle thread) {
		thread.setMessageUrl(uploadDao.getUrl(thread.getMessageKey()));
		return thread;
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return addMessageUrl(thread);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle) {
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(newTitle, "newTitle cannot be null");
		ValidateArgument.required(newTitle.getTitle(), "title cannot be null");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			return addMessageUrl(threadDao.updateTitle(threadIdLong, newTitle.getTitle()));
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId,
			UpdateThreadMessage newMessage) throws IOException {
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(newMessage, "newMessage cannot be null");
		ValidateArgument.required(newMessage.getMessageMarkdown(), "message markdown cannot be null");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			String messageKey = uploadDao.uploadDiscussionContent(newMessage.getMessageMarkdown(), thread.getForumId(), thread.getId());
			return addMessageUrl(threadDao.updateMessageKey(threadIdLong, messageKey));
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void markThreadAsDeleted(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		threadDao.markThreadAsDeleted(threadIdLong);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending) {
		ValidateArgument.required(forumId, "forumId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return addMessageUrl(threadDao.getThreads(Long.parseLong(forumId), limit, offset, order, ascending));
	}

	private PaginatedResults<DiscussionThreadBundle> addMessageUrl(
			PaginatedResults<DiscussionThreadBundle> threads) {
		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		for (DiscussionThreadBundle thread : threads.getResults()) {
			list.add(addMessageUrl(thread));
		}
		threads.setResults(list);
		return threads;
	}

	@Override
	public AuthorizationStatus canAccess(UserInfo userInfo, String threadId, ACCESS_TYPE accessType) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(accessType, "accessType cannot be null");
		DiscussionThreadBundle thread = threadDao.getThread(Long.parseLong(threadId));
		return authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, accessType);
	}
}
