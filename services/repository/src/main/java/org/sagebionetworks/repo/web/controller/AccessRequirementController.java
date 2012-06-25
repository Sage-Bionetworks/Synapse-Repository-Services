package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessClassHelper;
import org.sagebionetworks.repo.model.AccessRequirementType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller

public class AccessRequirementController extends BaseController {

	@Autowired
	AccessRequirementManager accessRequirementManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;
	
//	class AccessRequirementType implements JSONEntity {
//		private AccessRequirementType accessRequirementType;
//	    public AccessRequirementType getAccessRequirementType() {return accessRequirementType;}
//	    public void setAccessRequirementType(AccessRequirementType t) {accessRequirementType=t;}
//		
//	}
//	
//	public static AccessRequirementType getType(String jsonString) {
//		AccessRequirementType e = EntityFactory.createEntityFromJSONString(jsonString, AccessRequirementType.class);
//		return e.get
//	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT, method = RequestMethod.POST)
	public @ResponseBody
	AccessRequirement createAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException, InvalidModelException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// TODO the following will not work, since we're passing in an interface, not a class
		AccessRequirement accessRequirement = (AccessRequirement) objectTypeSerializer.deserialize(request.getInputStream(), header, AccessRequirement.class, header.getContentType());
			// now that we have the type
		Class<? extends AccessRequirement> type = AccessClassHelper.getClass(accessRequirement.getAccessRequirementType());
		accessRequirement = (AccessRequirement) objectTypeSerializer.deserialize(request.getInputStream(), header, type, header.getContentType());
		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getUnfulfilledAccessReqAccessRequirement(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String entityId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getUnmetAccessRequirement(userInfo, entityId);
		
		return new PaginatedResults<AccessRequirement>(
				request.getServletPath()+UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);

	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT, method = RequestMethod.PUT)
	public @ResponseBody
	AccessRequirement updateAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, ForbiddenException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// TODO the following will not work, since we're passing in an interface, not a class
		AccessRequirement accessRequirement = (AccessRequirement) objectTypeSerializer.deserialize(request.getInputStream(), header, AccessRequirement.class, header.getContentType());
			// now that we have the type
		Class<? extends AccessRequirement> type = AccessClassHelper.getClass(accessRequirement.getAccessRequirementType());
		accessRequirement = (AccessRequirement) objectTypeSerializer.deserialize(request.getInputStream(), header, type, header.getContentType());
		if(etag != null){
			accessRequirement.setEtag(etag.toString());
		}
		return accessRequirementManager.updateAccessRequirement(userInfo, accessRequirement);
	}

	

	
}
