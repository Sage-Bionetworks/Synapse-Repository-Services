package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityClassHelper;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
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

import com.amazonaws.util.StringInputStream;

@Controller
public class AccessRequirementController extends BaseController {

	@Autowired
	AccessRequirementManager accessRequirementManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT, method = RequestMethod.POST)
	public @ResponseBody
	AccessRequirement createAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessRequirement accessRequirement = deserialize(request, header);
		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}
	
	public AccessRequirement deserialize(HttpServletRequest request, HttpHeaders header) throws DatastoreException, IOException {
		try {
			String requestBody = ControllerUtil.getRequestBodyAsString(request);
			// TODO:  what if the body is not JSON??
			JSONObjectAdapter jsonObjectAdapter = (new JSONObjectAdapterImpl()).createNew(requestBody);
			String type = EntityClassHelper.entityType(jsonObjectAdapter);
			Class<? extends AccessRequirement> clazz = (Class<? extends AccessRequirement>)Class.forName(type);
			// now we know the type so we can deserialize into the correct one
			// need an input stream
			InputStream sis = new StringInputStream(requestBody);
			return (AccessRequirement) objectTypeSerializer.deserialize(sis, header, clazz, header.getContentType());
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}


	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getUnfulfilledAccessRequirement(
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
		AccessRequirement accessRequirement = deserialize(request, header);
		if(etag != null){
			accessRequirement.setEtag(etag.toString());
		}
		return accessRequirementManager.updateAccessRequirement(userInfo, accessRequirement);
	}

	

	
}
