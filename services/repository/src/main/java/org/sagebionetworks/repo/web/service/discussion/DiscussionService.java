package org.sagebionetworks.repo.web.service.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;

public interface DiscussionService {

	/**
	 * create a new forum
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 */
	public Forum createForum(Long userId, String projectId);

	/**
	 * get forum metadata for the given project
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 */
	public Forum getForumMetadata(Long userId, String projectId);
}
