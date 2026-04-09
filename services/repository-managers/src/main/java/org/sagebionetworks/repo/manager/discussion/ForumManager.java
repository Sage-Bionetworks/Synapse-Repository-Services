package org.sagebionetworks.repo.manager.discussion;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;

public interface ForumManager {

	/**
	 * create forum for a project
	 * 
	 * @param user
	 * @param projectId
	 * @return
	 */
	public Forum createForum(UserInfo user, String projectId);

	/**
	 * create forum for a project
	 * 
	 * @param user
	 * @param projectId
	 * @return
	 */
	public Forum getForumByProjectId(UserInfo user, String projectId);

	/**
	 * create forum for a given ID
	 * 
	 * @param user
	 * @param forumId
	 * @return
	 */
	public Forum getForum(UserInfo user, String forumId);

	/**
	 * Get forum for the given object ID and Type.
	 *
	 * @param user
	 * @param objectId
	 * @param objectType
	 */
	public Forum getForumByObjectIdAndType(UserInfo user, String objectId, ForumObjectType objectType);
}
