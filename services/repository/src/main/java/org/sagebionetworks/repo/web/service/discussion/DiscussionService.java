package org.sagebionetworks.repo.web.service.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;

public interface DiscussionService {

	/**
	 * get forum metadata for the given project
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 */
	public Forum getForumMetadata(Long userId, String projectId);
}
