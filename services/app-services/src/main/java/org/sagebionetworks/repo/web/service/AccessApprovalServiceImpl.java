package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.sagebionetworks.repo.transactions.WriteTransaction;

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
			RestrictableObjectDescriptor subjectId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessApproval> results = 
			accessApprovalManager.getAccessApprovalsForSubject(userInfo, subjectId);
		
		return new PaginatedResults<AccessApproval>(
				results.getResults(),
				(int)results.getTotalNumberOfResults());
	}

	@WriteTransaction
	@Override
	public void deleteAccessApprovals(Long userId, String approvalId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.deleteAccessApproval(userInfo, approvalId);
	}
	
}
