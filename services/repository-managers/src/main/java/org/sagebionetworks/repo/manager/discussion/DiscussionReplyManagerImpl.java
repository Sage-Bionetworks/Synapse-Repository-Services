package org.sagebionetworks.repo.manager.discussion;

import static org.sagebionetworks.repo.manager.AuthorizationManagerImpl.*;

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
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.upload.discussion.MessageKeyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionReplyManagerImpl implements DiscussionReplyManager {
	private static final DiscussionFilter DEFAULT_FILTER = DiscussionFilter.NO_FILTER;
	@Autowired
	private DiscussionThreadManager threadManager;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle createReply(UserInfo userInfo,
			CreateDiscussionReply createReply) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(createReply, "createReply");
		String threadId = createReply.getThreadId();
		ValidateArgument.required(threadId, "CreateDiscussionReply.threadId");
		ValidateArgument.required(createReply.getMessageMarkdown(), "CreateDiscussionReply.messageMarkdown");
		if (authorizationManager.isAnonymousUser(userInfo)){
			throw new UnauthorizedException(ANONYMOUS_ACCESS_DENIED_REASON);
		}
		DiscussionThreadBundle thread = threadManager.getThread(userInfo, threadId);
		String replyId = idGenerator.generateNewId(TYPE.DISCUSSION_REPLY_ID).toString();
		String messageKey = uploadDao.uploadReplyMessage(createReply.getMessageMarkdown(), thread.getForumId(), threadId, replyId);
		DiscussionReplyBundle reply = replyDao.createReply(threadId, replyId, messageKey, userInfo.getId());
		subscriptionDao.create(userInfo.getId().toString(), thread.getId(), SubscriptionObjectType.THREAD);
		transactionalMessenger.sendMessageAfterCommit(replyId, ObjectType.REPLY, reply.getEtag(), ChangeType.CREATE, userInfo.getId());
		return reply;
	}

	@Override
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId");
		DiscussionReplyBundle reply = replyDao.getReply(Long.parseLong(replyId), DiscussionFilter.EXCLUDE_DELETED);
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
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong, DEFAULT_FILTER);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, reply.getCreatedBy())) {
			String messageKey = uploadDao.uploadReplyMessage(newMessage.getMessageMarkdown(), reply.getForumId(), reply.getThreadId(), reply.getId());
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
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong, DEFAULT_FILTER);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, reply.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		replyDao.markReplyAsDeleted(replyIdLong);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			UserInfo userInfo, String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending, DiscussionFilter filter) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(filter, "filter");
		threadManager.getThread(userInfo, threadId);
		return replyDao.getRepliesForThread(Long.parseLong(threadId), limit, offset, order, ascending, filter);
	}

	@Override
	public MessageURL getMessageUrl(UserInfo userInfo, String messageKey) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(messageKey, "messageKey");
		String replyId = MessageKeyUtils.getReplyId(messageKey);
		DiscussionReplyBundle reply = replyDao.getReply(Long.parseLong(replyId), DEFAULT_FILTER);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, reply.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		return uploadDao.getReplyUrl(reply.getMessageKey());
	}

	@Override
	public ReplyCount getReplyCountForThread(UserInfo userInfo, String threadId, DiscussionFilter filter) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(filter, "filter");
		UserInfo.validateUserInfo(userInfo);
		threadManager.getThread(userInfo, threadId);
		ReplyCount count = new ReplyCount();
		count.setCount(replyDao.getReplyCount(Long.parseLong(threadId), filter));
		return count;
	}
}
