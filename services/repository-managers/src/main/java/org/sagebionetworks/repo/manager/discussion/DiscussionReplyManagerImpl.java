package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionReplyManagerImpl implements DiscussionReplyManager {
	@Autowired
	private DiscussionThreadManager threadManager;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DiscussionReplyDAO replyDao;

	@Override
	public DiscussionReplyBundle createReply(UserInfo userInfo,
			CreateDiscussionReply createReply) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(createReply, "createReply cannot be null");
		String threadId = createReply.getThreadId();
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(createReply.getMessageMarkdown(), "message cannot be null");
		DiscussionThreadBundle thread = threadManager.getThread(userInfo, threadId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		String messageKey = uploadDao.uploadDiscussionContent(createReply.getMessageMarkdown(), thread.getForumId(), threadId);
		return addMessageUrl(replyDao.createReply(threadId, messageKey, userInfo.getId()));
	}

	private DiscussionReplyBundle addMessageUrl(DiscussionReplyBundle reply) {
		reply.setMessageUrl(uploadDao.getUrl(reply.getMessageKey()));
		return reply;
	}

	@Override
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo,
			String replyId, UpdateReplyMessage newMessage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void markReplyAsDeleted(UserInfo userInfo, String replyId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			UserInfo userInfo, String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending) {
		// TODO Auto-generated method stub
		return null;
	}

}
