package org.sagebionetworks.repo.manager.discussion;

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
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.upload.discussion.MessageKeyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadManagerImpl implements DiscussionThreadManager {
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;
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
		ValidateArgument.required(createThread, "createThread");
		ValidateArgument.required(createThread.getForumId(), "CreateDiscussionThread.forumId");
		ValidateArgument.required(createThread.getTitle(), "CreateDiscussionThread.title");
		ValidateArgument.required(createThread.getMessageMarkdown(), "CreateDiscussionThread.messageMarkdown");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(createThread.getForumId())).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Long id = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID);
		String messageKey = uploadDao.uploadThreadMessage(createThread.getMessageMarkdown(), createThread.getForumId(), id.toString());
		DiscussionThreadBundle thread = threadDao.createThread(createThread.getForumId(), id.toString(), createThread.getTitle(), messageKey, userInfo.getId());
		return updateNumberOfReplies(thread);
	}

	private DiscussionThreadBundle updateNumberOfReplies(DiscussionThreadBundle thread) {
		thread.setNumberOfReplies(replyDao.getReplyCount(Long.parseLong(thread.getId())));
		return thread;
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return updateNumberOfReplies(thread);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newTitle, "newTitle");
		ValidateArgument.required(newTitle.getTitle(), "UpdateThreadTitle.title");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			thread = threadDao.updateTitle(threadIdLong, newTitle.getTitle());
			return updateNumberOfReplies(thread);
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
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			String messageKey = uploadDao.uploadThreadMessage(newMessage.getMessageMarkdown(), thread.getForumId(), thread.getId());
			thread = threadDao.updateMessageKey(threadIdLong, messageKey);
			return updateNumberOfReplies(thread);
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
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		threadDao.markThreadAsDeleted(threadIdLong);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending) {
		ValidateArgument.required(forumId, "forumId");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		PaginatedResults<DiscussionThreadBundle> threads = threadDao.getThreads(Long.parseLong(forumId), limit, offset, order, ascending);
		return updateNumberOfReplies(threads);
	}

	private PaginatedResults<DiscussionThreadBundle> updateNumberOfReplies(
			PaginatedResults<DiscussionThreadBundle> threads) {
		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		for (DiscussionThreadBundle thread : threads.getResults()) {
			list.add(updateNumberOfReplies(thread));
		}
		threads.setResults(list);
		return threads;
	}

	@Override
	public String getMessageUrl(UserInfo userInfo, String messageKey) {
		ValidateArgument.required(messageKey, "messageKey");
		UserInfo.validateUserInfo(userInfo);
		String threadId = MessageKeyUtils.getThreadId(messageKey);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		return uploadDao.getThreadUrl(messageKey);
	}
}
