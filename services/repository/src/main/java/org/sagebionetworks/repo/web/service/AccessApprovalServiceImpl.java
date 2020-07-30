package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessApprovalServiceImpl implements AccessApprovalService {

	private AccessApprovalManager accessApprovalManager;
	
	private AccessApprovalNotificationManager notificationManager;
	
	private UserManager userManager;

	@Autowired
	public AccessApprovalServiceImpl(
			final AccessApprovalManager accessApprovalManager,
			final AccessApprovalNotificationManager notificationManager, 
			final UserManager userManager) {
		this.accessApprovalManager = accessApprovalManager;
		this.notificationManager = notificationManager;
		this.userManager = userManager;
	}

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
	
	@Override
	public AccessApprovalNotificationResponse listNotificationsRequest(Long userId,
			AccessApprovalNotificationRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return notificationManager.listNotificationsRequest(user, request);
	}
}
