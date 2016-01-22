package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;

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
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.upload.discussion.MessageKeyUtils;
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
	@Autowired
	private IdGenerator idGenerator;

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle createReply(UserInfo userInfo,
			CreateDiscussionReply createReply) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(createReply, "createReply");
		String threadId = createReply.getThreadId();
		ValidateArgument.required(threadId, "CreateDiscussionReply.threadId");
		ValidateArgument.required(createReply.getMessageMarkdown(), "CreateDiscussionReply.messageMarkdown");
		DiscussionThreadBundle thread = threadManager.getThread(userInfo, threadId);
		String replyId = idGenerator.generateNewId(TYPE.DISCUSSION_REPLY_ID).toString();
		String messageKey = uploadDao.uploadReplyMessage(createReply.getMessageMarkdown(), thread.getForumId(), threadId, replyId);
		return replyDao.createReply(threadId, replyId, messageKey, userInfo.getId());
	}

	@Override
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId");
		DiscussionReplyBundle reply = replyDao.getReply(Long.parseLong(replyId));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, reply.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		return reply;
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo,
			String replyId, UpdateReplyMessage newMessage) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId");
		ValidateArgument.required(newMessage, "newMessage");
		ValidateArgument.required(newMessage.getMessageMarkdown(), "UpdateReplyMessage.messageMarkdown");
		Long replyIdLong = Long.parseLong(replyId);
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, reply.getCreatedBy())) {
			String messageKey = uploadDao.uploadThreadMessage(newMessage.getMessageMarkdown(), reply.getForumId(), reply.getThreadId());
			return replyDao.updateMessageKey(replyIdLong, messageKey);
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void markReplyAsDeleted(UserInfo userInfo, String replyId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId");
		Long replyIdLong = Long.parseLong(replyId);
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, reply.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		replyDao.markReplyAsDeleted(replyIdLong);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			UserInfo userInfo, String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		threadManager.getThread(userInfo, threadId);
		return replyDao.getRepliesForThread(Long.parseLong(threadId), limit, offset, order, ascending);
	}

	@Override
	public String getMessageUrl(UserInfo userInfo, String messageKey) {
		ValidateArgument.required(messageKey, "messageKey");
		UserInfo.validateUserInfo(userInfo);
		String replyId = MessageKeyUtils.getReplyId(messageKey);
		ValidateArgument.required(replyId, "replyId");
		DiscussionReplyBundle reply = replyDao.getReply(Long.parseLong(replyId));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, reply.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		return uploadDao.getReplyUrl(messageKey);
	}

}
