package org.sagebionetworks.repo.model.dbo.dao.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;

public interface ForumDAO {

	/**
	 * Create a new forum for the given object.
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public Forum createForum(String objectId, ForumObjectType objectType);

	/**
	 * Get the forum object given its Id.
	 * @param id
	 * @return
	 */
	public Forum getForum(long id);

	/**
	 * Get the forum for the given object ID and Type.
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	public Forum getForumByObjectIdAndType(String objectId, ForumObjectType objectType);

	/**
	 * Delete forum that has the given forumId.
	 * @param id
	 */
	public int deleteForum(long id);

}
