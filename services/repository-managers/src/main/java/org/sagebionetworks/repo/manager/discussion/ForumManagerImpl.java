package org.sagebionetworks.repo.manager.discussion;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ForumManagerImpl implements ForumManager {
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private AuthorizationManager authorizationManager;

	@WriteTransaction
	@Override
	public Forum createForum(UserInfo user, String projectId) {
		UserInfo.validateUserInfo(user);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(user, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return forumDao.createForum(projectId);
	}

	@Override
	public Forum getForumMetadata(UserInfo user, String projectId) {
		UserInfo.validateUserInfo(user);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(user, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		try {
			return forumDao.getForumByProjectId(projectId);
		} catch (NotFoundException e) {
			return createForum(user, projectId);
		}
	}

}
