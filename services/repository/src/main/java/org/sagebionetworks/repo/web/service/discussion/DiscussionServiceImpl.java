package org.sagebionetworks.repo.web.service.discussion;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyOrder;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ReplyCount;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionServiceImpl implements DiscussionService{
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private DiscussionThreadManager threadManager;
	@Autowired
	private DiscussionReplyManager replyManager;

	@Override
	public Forum getForumByProjectId(UserInfo userInfo, String projectId) {
		return forumManager.getForumByProjectId(userInfo, projectId);
	}

	@Override
	public Forum getForum(UserInfo userInfo, String forumId) {
		return forumManager.getForum(userInfo, forumId);
	}

	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo,
			CreateDiscussionThread toCreate) throws IOException {
		return threadManager.createThread(userInfo, toCreate);
	}

	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		return threadManager.getThread(userInfo, threadId);
	}

	@Override
	public DiscussionThreadBundle updateThreadTitle(UserInfo userInfo, String threadId, UpdateThreadTitle title) {
		return threadManager.updateTitle(userInfo, threadId, title);
	}

	@Override
	public DiscussionThreadBundle updateThreadMessage(UserInfo userInfo, String threadId,
			UpdateThreadMessage message) throws IOException {
		return threadManager.updateMessage(userInfo, threadId, message);
	}

	@Override
	public void markThreadAsDeleted(UserInfo userInfo,
			String threadId) {
		threadManager.markThreadAsDeleted(userInfo, threadId);
	}

	@Override
	public void markThreadAsNotDeleted(UserInfo userInfo,
			String threadId) {

		threadManager.markThreadAsNotDeleted(userInfo, threadId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(UserInfo userInfo,
			String forumId, Long limit, Long offset, DiscussionThreadOrder order,
			Boolean ascending, DiscussionFilter filter) {

		return threadManager.getThreadsForForum(userInfo, forumId, limit, offset, order, ascending, filter);
	}

	@Override
	public DiscussionReplyBundle createReply(UserInfo userInfo, CreateDiscussionReply toCreate) throws IOException {

		return replyManager.createReply(userInfo, toCreate);
	}

	@Override
	public DiscussionReplyBundle getReply(UserInfo userInfo, String replyId) {

		return replyManager.getReply(userInfo, replyId);
	}

	@Override
	public DiscussionReplyBundle updateReplyMessage(UserInfo userInfo, String replyId, UpdateReplyMessage message) throws IOException {

		return replyManager.updateReplyMessage(userInfo, replyId, message);
	}

	@Override
	public void markReplyAsDeleted(UserInfo userInfo, String replyId) {

		replyManager.markReplyAsDeleted(userInfo, replyId);
	}

	@Override
	public PaginatedResults<DiscussionReplyBundle> getReplies(UserInfo userInfo,
			String threadId, Long limit, Long offset, DiscussionReplyOrder order,
			Boolean ascending, DiscussionFilter filter) {

		return replyManager.getRepliesForThread(userInfo, threadId, limit, offset, order, ascending, filter);
	}

	@Override
	public MessageURL getThreadUrl(UserInfo userInfo, String messageKey) {

		return threadManager.getMessageUrl(userInfo, messageKey);
	}

	@Override
	public MessageURL getReplyUrl(UserInfo userInfo, String messageKey) {

		return replyManager.getMessageUrl(userInfo, messageKey);
	}

	@Override
	public ThreadCount getThreadCount(UserInfo userInfo, String forumId, DiscussionFilter filter) {

		return threadManager.getThreadCountForForum(userInfo, forumId, filter);
	}

	@Override
	public ReplyCount getReplyCount(UserInfo userInfo, String threadId, DiscussionFilter filter) {
		return replyManager.getReplyCountForThread(userInfo, threadId, filter);
	}

	@Override
	public void pinThread(UserInfo userInfo, String threadId) {
		threadManager.pinThread(userInfo, threadId);
	}

	@Override
	public void unpinThread(UserInfo userInfo, String threadId) {

		threadManager.unpinThread(userInfo, threadId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(UserInfo userInfo, String entityId, Long limit,
			Long offset, DiscussionThreadOrder order, Boolean ascending) {
		return threadManager.getThreadsForEntity(userInfo, entityId, limit, offset, order, ascending);
	}

	@Override
	public EntityThreadCounts getThreadCounts(UserInfo userInfo, EntityIdList entityIds) {
		return threadManager.getEntityThreadCounts(userInfo, entityIds);
	}

	@Override
	public PaginatedIds getModerators(UserInfo userInfo, String forumId, Long limit, Long offset) {
		return threadManager.getModerators(userInfo, forumId, limit, offset);
	}
}
