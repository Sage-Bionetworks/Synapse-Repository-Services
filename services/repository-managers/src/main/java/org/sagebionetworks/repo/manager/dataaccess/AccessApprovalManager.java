package org.sagebionetworks.repo.manager.dataaccess;

import java.time.Instant;

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
	public AccessApproval createAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param approvalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) throws DatastoreException, NotFoundException;

	/**
	 * Delete all access approvals that gives accessorId access to subject(s) that requires access requirement accessRequirementId
	 * 
	 * @param userInfo - the user who making the request
	 * @param accessRequirementId - the target access requirement
	 * @param accessorId - the user whose access is being revoked
	 * @throws UnauthorizedException - if the user is not an admin or an ACT member
	 */
	public void revokeAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId) throws UnauthorizedException;

	/**
	 * List a page of accessor groups.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public AccessorGroupResponse listAccessorGroup(UserInfo userInfo, AccessorGroupRequest request);

	/**
	 * Revoke a group of accessors
	 * 
	 * @param userInfo
	 * @param request
	 */
	public void revokeGroup(UserInfo userInfo, AccessorGroupRevokeRequest request);

	/**
	 * Retrieve a batch of AccessApprovalInfo.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public BatchAccessApprovalInfoResponse getAccessApprovalInfo(UserInfo userInfo, BatchAccessApprovalInfoRequest request);
	
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
