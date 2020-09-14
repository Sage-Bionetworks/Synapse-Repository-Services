package org.sagebionetworks.repo.manager.dataaccess;

import java.time.Instant;
import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalManager {
	
	/**
	 *  create access approval
	 */
	AccessApproval createAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param approvalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) throws DatastoreException, NotFoundException;

	/**
	 * Revoke all access approvals that gives accessorId access to subject(s) that requires access requirement accessRequirementId
	 * 
	 * @param userInfo - the user who making the request
	 * @param accessRequirementId - the target access requirement
	 * @param accessorId - the user whose access is being revoked
	 * @throws UnauthorizedException - if the user is not an admin or an ACT member
	 */
	void revokeAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId) throws UnauthorizedException;

	/**
	 * List a page of accessor groups.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	AccessorGroupResponse listAccessorGroup(UserInfo userInfo, AccessorGroupRequest request);

	/**
	 * Revoke a group of accessors
	 * 
	 * @param userInfo
	 * @param request
	 */
	void revokeGroup(UserInfo userInfo, AccessorGroupRevokeRequest request);

	/**
	 * Revoke a group of access approvals for the given access requirement, submitter and list of accessors
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @param submitterId
	 * @param accessorIds
	 */
	void revokeGroup(UserInfo userInfo, String accessRequirementId, String submitterId, List<String> accessorIds);

	/**
	 * Retrieve a batch of AccessApprovalInfo.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	BatchAccessApprovalInfoResponse getAccessApprovalInfo(UserInfo userInfo, BatchAccessApprovalInfoRequest request);
	
	/**
	 * Revokes all the approval that are expired. This method is invoked periodically by a worker.
	 * 
	 * @param user The user used for revoking
	 * @param expiredAfter Only revoke approvals expired after the given instant. Must be a value in the past.
	 * @param maxBatchSize The maximum number of approval to expire
	 * @return The number of expired approvals
	 */
	int revokeExpiredApprovals(UserInfo user, Instant expiredAfter, int maxBatchSize);
}
