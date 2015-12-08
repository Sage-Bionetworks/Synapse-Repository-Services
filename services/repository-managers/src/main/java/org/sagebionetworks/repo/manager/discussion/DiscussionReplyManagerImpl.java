package org.sagebionetworks.repo.manager.discussion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
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

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle createReply(UserInfo userInfo,
			CreateDiscussionReply createReply) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(createReply, "createReply cannot be null");
		String threadId = createReply.getThreadId();
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(createReply.getMessageMarkdown(), "message cannot be null");
		DiscussionThreadBundle thread = threadManager.getThread(userInfo, threadId);
		String messageKey = uploadDao.uploadDiscussionContent(createReply.getMessageMarkdown(), thread.getForumId(), threadId);
		return addMessageUrl(replyDao.createReply(threadId, messageKey, userInfo.getId()));
	}

	private DiscussionReplyBundle addMessageUrl(DiscussionReplyBundle reply) {
		reply.setMessageUrl(uploadDao.getUrl(reply.getMessageKey()));
		return reply;
	}

	@Override
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId cannot be null");
		DiscussionReplyBundle reply = replyDao.getReply(Long.parseLong(replyId));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				threadManager.canAccess(userInfo, reply.getThreadId(), ACCESS_TYPE.READ));
		return addMessageUrl(reply);
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo,
			String replyId, UpdateReplyMessage newMessage) throws IOException {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId cannot be null");
		ValidateArgument.required(newMessage, "newMessage cannot be null");
		ValidateArgument.required(newMessage.getMessageMarkdown(), "message markdown cannot be null");
		Long replyIdLong = Long.parseLong(replyId);
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong);
		DiscussionThreadBundle thread = threadManager.getThread(userInfo, reply.getThreadId());
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, reply.getCreatedBy())) {
			String messageKey = uploadDao.uploadDiscussionContent(newMessage.getMessageMarkdown(), thread.getForumId(), reply.getThreadId());
			return addMessageUrl(replyDao.updateMessageKey(replyIdLong, messageKey));
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void markReplyAsDeleted(UserInfo userInfo, String replyId) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(replyId, "replyId cannot be null");
		Long replyIdLong = Long.parseLong(replyId);
		DiscussionReplyBundle reply = replyDao.getReply(replyIdLong);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				threadManager.canAccess(userInfo, reply.getThreadId(), ACCESS_TYPE.DELETE));
		replyDao.markReplyAsDeleted(replyIdLong);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getRepliesForThread(
			UserInfo userInfo, String threadId, Long limit, Long offset,
			DiscussionReplyOrder order, Boolean ascending) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(threadId, "threadId cannot be null");
		ValidateArgument.required(limit, "limit cannot be null");
		ValidateArgument.required(offset, "offset cannot be null");
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				threadManager.canAccess(userInfo, threadId, ACCESS_TYPE.READ));
		return addMessageUrl(replyDao.getRepliesForThread(Long.parseLong(threadId), limit, offset, order, ascending));
	}

	private PaginatedResults<DiscussionReplyBundle> addMessageUrl(
			PaginatedResults<DiscussionReplyBundle> repliesForThread) {
		List<DiscussionReplyBundle> list = new ArrayList<DiscussionReplyBundle>();
		for (DiscussionReplyBundle reply : repliesForThread.getResults()) {
			list.add(addMessageUrl(reply));
		}
		repliesForThread.setResults(list);
		return repliesForThread;
	}

}
