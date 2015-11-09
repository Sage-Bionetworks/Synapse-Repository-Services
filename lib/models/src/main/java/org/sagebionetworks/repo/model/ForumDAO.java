package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.discussion.Forum;

public interface ForumDAO {

	/**
	 * create new object
	 * @param projectId
	 * @return
	 */
	public Forum createForum(Long projectId);

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
	public Forum getForumByProjectId(long projectId);

	/**
	 * delete forum that has the given forumId
	 * @param id
	 */
	public int deleteForum(long id);

	/**
	 * delete forum that has the given projectId
	 * @param projectId
	 */
	public int deleteForumByProjectId(long projectId);

	/**
	 * truncate all data in Forum table
	 * This method should only be used for testing
	 */
	public void truncateAll();
}
