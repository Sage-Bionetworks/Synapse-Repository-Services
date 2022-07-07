package org.sagebionetworks.repo.manager.dataaccess;

public interface DataAccessSubmissionNotificationManager {

	/**
	 * Send an email notification to all non-ACT reviewers of the given data access
	 * submission.
	 * 
	 * @param dataAccessSubmissionId
	 */
	void sendNotificationToReviewers(String dataAccessSubmissionId);

}
