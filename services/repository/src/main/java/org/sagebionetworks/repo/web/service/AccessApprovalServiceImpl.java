package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
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
import org.springframework.beans.factory.annotation.Autowired;

public class AccessApprovalServiceImpl implements AccessApprovalService {

	@Autowired
	AccessApprovalManager accessApprovalManager;
	@Autowired
	OpenIDConnectManager oidcManager;

	@WriteTransaction
	@Override
	public AccessApproval createAccessApproval(String accessToken,
			AccessApproval accessApproval) throws DatastoreException, UnauthorizedException, 
			NotFoundException, InvalidModelException, IOException {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		return accessApprovalManager.createAccessApproval(userInfo, accessApproval);
	}

	@Override
	public AccessApproval getAccessApproval(
			String accessToken, String approvalId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		return accessApprovalManager.getAccessApproval(userInfo, approvalId);
	}

	@Override
	public PaginatedResults<AccessApproval> getAccessApprovals(String accessToken, 
			RestrictableObjectDescriptor subjectId, Long limit, Long offset)
					throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);

		List<AccessApproval> results = 
			accessApprovalManager.getAccessApprovalsForSubject(userInfo, subjectId, limit, offset);
		return PaginatedResults.createMisusedPaginatedResults(results);
	}

	@WriteTransaction
	@Override
	public void deleteAccessApproval(String accessToken, String approvalId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		accessApprovalManager.deleteAccessApproval(userInfo, approvalId);
	}

	@WriteTransaction
	@Override
	public void revokeAccessApprovals(String accessToken, String accessRequirementId, String accessorId) 
			throws UnauthorizedException, NotFoundException {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		accessApprovalManager.revokeAccessApprovals(userInfo, accessRequirementId, accessorId);
	}

	@Override
	public AccessorGroupResponse listAccessorGroup(String accessToken, AccessorGroupRequest request) {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		return accessApprovalManager.listAccessorGroup(userInfo, request);
	}

	@Override
	public void revokeGroup(String accessToken, AccessorGroupRevokeRequest request) {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		accessApprovalManager.revokeGroup(userInfo, request);
	}

	@Override
	public BatchAccessApprovalInfoResponse getBatchAccessApprovalInfo(String accessToken,
			BatchAccessApprovalInfoRequest request) {
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		return accessApprovalManager.getAccessApprovalInfo(userInfo, request);
	}
}
