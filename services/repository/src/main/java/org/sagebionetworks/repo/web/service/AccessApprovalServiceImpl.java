package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessApprovalServiceImpl implements AccessApprovalService {

	@Autowired
	AccessApprovalManager accessApprovalManager;
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@WriteTransaction
	@Override
	public AccessApproval createAccessApproval(Long userId,
			AccessApproval accessApproval) throws DatastoreException, UnauthorizedException, 
			NotFoundException, InvalidModelException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessApprovalManager.createAccessApproval(userInfo, accessApproval);
	}

	@Override
	public AccessApproval getAccessApproval(
			Long userId, String approvalId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessApprovalManager.getAccessApproval(userInfo, approvalId);
	}

	@Override
	public PaginatedResults<AccessApproval> getAccessApprovals(Long userId, 
			RestrictableObjectDescriptor subjectId, Long limit, Long offset)
					throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		List<AccessApproval> results = 
			accessApprovalManager.getAccessApprovalsForSubject(userInfo, subjectId, limit, offset);
		return PaginatedResults.createMisusedPaginatedResults(results);
	}

	@WriteTransaction
	@Override
	public void deleteAccessApproval(Long userId, String approvalId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.deleteAccessApproval(userInfo, approvalId);
	}

	@WriteTransaction
	@Override
	public void revokeAccessApprovals(Long userId, String accessRequirementId, String accessorId) 
			throws UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Override
	public AccessorGroupResponse listAccessorGroup(Long userId, AccessorGroupRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessApprovalManager.listAccessorGroup(userInfo, request);
	}

	@Override
	public void revokeGroup(Long userId, AccessorGroupRevokeRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.revokeGroup(userInfo, request);
	}

	@Override
	public BatchAccessApprovalInfoResponse getBatchAccessApprovalInfo(Long userId,
			BatchAccessApprovalInfoRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessApprovalManager.getAccessApprovalInfo(userInfo, request);
	}
}
