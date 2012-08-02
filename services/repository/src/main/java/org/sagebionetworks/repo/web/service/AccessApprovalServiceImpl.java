package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.io.InputStream;

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
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.MediaTypeHelper;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import com.amazonaws.util.StringInputStream;

@Controller
public class AccessApprovalServiceImpl implements AccessApprovalService {

	@Autowired
	AccessApprovalManager accessApprovalManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessApprovalService#createAccessApproval(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public AccessApproval createAccessApproval(String userId, HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, 
			NotFoundException, ForbiddenException, InvalidModelException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessApproval accessApproval = deserialize(request, header);
		return accessApprovalManager.createAccessApproval(userInfo, accessApproval);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessApprovalService#deserialize(javax.servlet.http.HttpServletRequest, org.springframework.http.HttpHeaders)
	 */
	@Override
	public AccessApproval deserialize(HttpServletRequest request, HttpHeaders header) throws DatastoreException, IOException {
		try {
			String requestBody = ControllerUtil.getRequestBodyAsString(request);
			String type = MediaTypeHelper.entityType(requestBody, header.getContentType());
			Class<? extends AccessApproval> clazz = (Class<? extends AccessApproval>)Class.forName(type);
			// now we know the type so we can deserialize into the correct one
			// need an input stream
			InputStream sis = new StringInputStream(requestBody);
			return (AccessApproval) objectTypeSerializer.deserialize(sis, header, clazz, header.getContentType());
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessApprovalService#getAccessApprovals(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
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

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessApprovalService#deleteAccessApprovals(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteAccessApprovals(String userId, String approvalId) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessApprovalManager.deleteAccessApproval(userInfo, approvalId);
	}
	
}
