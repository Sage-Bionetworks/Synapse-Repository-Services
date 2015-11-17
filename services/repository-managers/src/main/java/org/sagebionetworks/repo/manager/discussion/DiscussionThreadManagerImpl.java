package org.sagebionetworks.repo.manager.discussion;

import java.util.UUID;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DiscussionThreadUtils;
import org.sagebionetworks.repo.model.discussion.CreateThread;
import org.sagebionetworks.repo.model.discussion.DiscussionOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadManagerImpl implements DiscussionThreadManager {

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private UploadContentToS3DAO uploadContentDao;

	@WriteTransaction
	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateThread createThread) {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, createThread.getForumId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		DiscussionThreadUtils.validateCreateThreadAndThrowException(createThread);
		String messageUrl = uploadMessageContent(createThread.getMessageMarkdown());
		return threadDao.createThread(createThread.getForumId(), createThread.getTitle(), messageUrl, userInfo.getId());
	}

	private String uploadMessageContent(String messageMarkdown) {
		String fileName = UUID.randomUUID().toString();
		return uploadContentDao.upload(messageMarkdown, fileName);
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		if (threadId == null) throw new IllegalArgumentException("threadId cannot be null");
		DiscussionThreadBundle dto = threadDao.getThread(Long.parseLong(threadId));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, dto.getForumId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		return dto;
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, String newTitle) {
		// TODO Auto-generated method stub
		return null;
	}

	@WriteTransaction
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId, String markdown) {
		// TODO Auto-generated method stub
		return null;
	}

	@WriteTransaction
	@Override
	public void deleteThread(UserInfo userInfo, String threadId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, DiscussionOrder order,
			Integer limit, Integer offset) {
		// TODO Auto-generated method stub
		return null;
	}

}
