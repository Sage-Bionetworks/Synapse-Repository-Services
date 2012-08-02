package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
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
import com.amazonaws.util.StringInputStream;

public class AccessRequirementServiceImpl implements AccessRequirementService {

	@Autowired
	AccessRequirementManager accessRequirementManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#createAccessRequirement(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public AccessRequirement createAccessRequirement(String userId, 
			HttpHeaders header, HttpServletRequest request) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessRequirement accessRequirement = deserialize(request, header);
		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#deserialize(javax.servlet.http.HttpServletRequest, org.springframework.http.HttpHeaders)
	 */
	@Override
	public AccessRequirement deserialize(HttpServletRequest request, HttpHeaders header) throws DatastoreException, IOException {
		try {
			String requestBody = ControllerUtil.getRequestBodyAsString(request);
			String type = MediaTypeHelper.entityType(requestBody, header.getContentType());
			Class<? extends AccessRequirement> clazz = (Class<? extends AccessRequirement>)Class.forName(type);
			// now we know the type so we can deserialize into the correct one
			// need an input stream
			InputStream sis = new StringInputStream(requestBody);
			return (AccessRequirement) objectTypeSerializer.deserialize(sis, header, clazz, header.getContentType());
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#getUnfulfilledAccessRequirement(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirement(
				String userId, String entityId,	HttpServletRequest request) 
				throws DatastoreException, UnauthorizedException, 
				NotFoundException, ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getUnmetAccessRequirements(userInfo, entityId);
		
		return new PaginatedResults<AccessRequirement>(
				request.getServletPath()+UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#getAccessRequirements(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public PaginatedResults<AccessRequirement> getAccessRequirements(
			String userId, String entityId,	HttpServletRequest request) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getAccessRequirementsForEntity(userInfo, entityId);
		
		return new PaginatedResults<AccessRequirement>(
				request.getServletPath()+UrlHelpers.ACCESS_REQUIREMENT, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#deleteAccessRequirements(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteAccessRequirements(String userId, String requirementId) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		accessRequirementManager.deleteAccessRequirement(userInfo, requirementId);

	}
	
}
