package org.sagebionetworks.repo.model.dao.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;

public interface ForumDAO {

	/**
	 * create new object
	 * @param projectId
	 * @return
	 */
	public Forum createForum(String projectId);

	/**
	 * get the forum object given its Id
	 * @param id
	 * @return
	 */
	public Forum getForum(long id);

	/**
	 * get the forum object given the projectId
	 * @param projectId
	 * @return
	 */
	public Forum getForumByProjectId(String projectId);

	/**
	 * delete forum that has the given forumId
	 * @param id
	 */
	public int deleteForum(long id);

}
