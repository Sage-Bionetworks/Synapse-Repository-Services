package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.manager.AccessApprovalManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.AccessClassHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.amazonaws.util.StringInputStream;

@Controller
public class AccessApprovalController extends BaseController {

	@Autowired
	AccessApprovalManager accessApprovalManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL, method = RequestMethod.POST)
	public @ResponseBody
	AccessApproval createAccessApproval(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException, InvalidModelException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessApproval accessApproval = deserialize(request, header);
		return accessApprovalManager.createAccessApproval(userInfo, accessApproval);
	}
	
	public AccessApproval deserialize(HttpServletRequest request, HttpHeaders header) throws DatastoreException, IOException {
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		AccessApprovalType type = AccessClassHelper.getAccessApprovalTypeFromJSON(requestBody);
		Class<? extends AccessApproval> clazz = AccessClassHelper.getClass(type);
		// now we know the type so we can deserialize into the correct one
		// need an input stream
		InputStream sis = new StringInputStream(requestBody);
		
		return (AccessApproval) objectTypeSerializer.deserialize(sis, header, clazz, header.getContentType());
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL, method = RequestMethod.PUT)
	public @ResponseBody
	AccessApproval updateAccessApproval(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, ForbiddenException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessApproval accessApproval = deserialize(request, header);		
		if(etag != null){
			accessApproval.setEtag(etag.toString());
		}
		return accessApprovalManager.updateAccessApproval(userInfo, accessApproval);
	}

	

	
}
