package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ControllerEntityClassHelper;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AccessApprovalServiceImpl implements AccessApprovalService {

	@Autowired
	AccessApprovalManager accessApprovalManager;	
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessApproval createAccessApproval(String userId, HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, 
			NotFoundException, ForbiddenException, InvalidModelException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessApproval accessApproval = (AccessApproval)ControllerEntityClassHelper.deserialize(request, header);
		return accessApprovalManager.createAccessApproval(userInfo, accessApproval);
	}

	@Override
	public PaginatedResults<AccessApproval> getAccessApprovals(String userId, 
			String entityId, HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException, ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessApproval> results = 
			accessApprovalManager.getAccessApprovalsForEntity(userInfo, entityId);
		
		return new PaginatedResults<AccessApproval>(
				request.getServletPath()+UrlHelpers.ACCESS_APPROVAL, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAccessApprovals(String userId, String approvalId) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.deleteAccessApproval(userInfo, approvalId);
	}
	
}
