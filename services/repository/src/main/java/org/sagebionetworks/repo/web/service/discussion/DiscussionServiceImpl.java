package org.sagebionetworks.repo.web.service.discussion;

import java.io.UnsupportedEncodingException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionServiceImpl implements DiscussionService{
	@Autowired
	private UserManager userManager;
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private DiscussionThreadManager threadManager;

	@Override
	public Forum getForumMetadata(Long userId, String projectId) {
		UserInfo user = userManager.getUserInfo(userId);
		return forumManager.getForumMetadata(user, projectId);
	}

	@Override
	public DiscussionThreadBundle createThread(Long userId,
			CreateDiscussionThread toCreate) throws UnsupportedEncodingException {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.createThread(user, toCreate);
	}

	@Override
	public DiscussionThreadBundle getThread(Long userId, String threadId) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.getThread(user, threadId);
	}

	@Override
	public DiscussionThreadBundle updateThreadTitle(Long userId, String threadId, String title) {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.updateTitle(user, threadId, title);
	}

	@Override
	public DiscussionThreadBundle updateThreadMessage(Long userId, String threadId,
			String message) throws UnsupportedEncodingException {
		UserInfo user = userManager.getUserInfo(userId);
		return threadManager.updateMessage(user, threadId, message);
	}

	@Override
	public void markThreadAsDeleted(Long userId,
			String threadId) {
		UserInfo user = userManager.getUserInfo(userId);
		threadManager.markThreadAsDeleted(user, threadId);
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreads(Long userId,
			String forumId, Long limit, Long offset, String sort,
			Boolean ascending) {
		// TODO Auto-generated method stub
		return null;
	}

}
