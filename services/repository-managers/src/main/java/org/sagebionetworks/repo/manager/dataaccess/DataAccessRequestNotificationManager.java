package org.sagebionetworks.repo.manager.dataaccess;

public interface DataAccessRequestNotificationManager {

	/**
	 * Send an email notification to all non-ACT reviewers of the given data access
	 * request.
	 * 
	 * @param dataAccessRequestId
	 */
	void sendNotificationToReviewers(String dataAccessRequestId);

}
