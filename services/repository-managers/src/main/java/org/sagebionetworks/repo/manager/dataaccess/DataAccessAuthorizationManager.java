package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

public interface DataAccessAuthorizationManager {
	
	/**
	 * Checks if the user is allowed to download files attached to the data access request with the given id
	 * 
	 * @param userInfo The user attempting to download files
	 * @param requestId The id of the request
	 * @return The {@link AuthorizationStatus} defining the user access
	 */
	AuthorizationStatus canDownloadRequestFiles(UserInfo userInfo, String requestId);
	
	/**
	 * Checks if the user is allowed to download files attached to the data access submission with the given id
	 * 
	 * @param userInfo The user attempting to download files
	 * @param submissionId The id of the submission
	 * @return The {@link AuthorizationStatus} defining the user access
	 */
	AuthorizationStatus canDownloadSubmissionFiles(UserInfo userInfo, String submissionId);
	
	
	/**
	 * Checks if the given user can review submissions for the given access requirement.
	 * 
	 * @param userInfo The user reviewing the access requirement submissions
	 * @param accessRequirementId The id of the access requirement
	 * @return The {@link AuthorizationStatus} defining the user access
	 */
	AuthorizationStatus canReviewAccessRequirementSubmissions(UserInfo userInfo, String accessRequirementId);

}
