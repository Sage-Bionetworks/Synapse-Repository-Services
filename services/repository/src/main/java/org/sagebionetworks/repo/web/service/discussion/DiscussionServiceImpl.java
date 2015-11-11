package org.sagebionetworks.repo.web.service.discussion;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionServiceImpl implements DiscussionService{
	@Autowired
	private UserManager userManager;
	@Autowired
	private ForumManager forumManager;

	@Override
	public Forum getForumMetadata(Long userId, String projectId) {
		UserInfo user = userManager.getUserInfo(userId);
		return forumManager.getForumMetadata(user, projectId);
	}

}
