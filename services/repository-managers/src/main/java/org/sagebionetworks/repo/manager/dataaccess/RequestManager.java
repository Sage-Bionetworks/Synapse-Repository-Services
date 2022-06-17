package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface RequestManager {


	/**
	 * Retrieve a Request object suitable for update, which has the original one created by or has associated research project owned by the user, for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Create or update a Request
	 * 
	 * @param user
	 * @param toCreateOrUpdate
	 * @return
	 */
	public RequestInterface createOrUpdate(UserInfo user, RequestInterface toCreateOrUpdate);

	/**
	 * This method is called when the ACT approves a submission. For the given
	 * request ID, the request will unconditionally be converted to a renewal.
	 * All revoked accessors changes will be removed and all other accessor
	 * changes will be set to a change type of renewal.
	 * 
	 * 
	 * @param requestId
	 *            The ID of the request to update.
	 */
	public void updateApprovedRequest(String requestId);

	/**
	 * Get a request to to create a submission.
	 * @param requestId
	 * @return
	 */
	public RequestInterface getRequestForSubmission(String requestId);

	public void truncateAll();
}
