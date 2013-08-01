package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.EVALUATION_ID_PATH_VAR_WITHOUT_BRACKETS;
import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerInfo(displayName="Access Approval Services", path="repo/v1")
@Controller
public class AccessApprovalController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL, method = RequestMethod.POST)
	public @ResponseBody
	AccessApproval createAccessApproval(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessApproval accessApproval,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException, IOException {
		return serviceProvider.getAccessApprovalService().createAccessApproval(userId, accessApproval);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_WITH_ENTITY_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessApproval> getEntityAccessApprovals(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= ID_PATH_VARIABLE) String entityId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		return serviceProvider.getAccessApprovalService().getAccessApprovals(userId, subjectId, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_WITH_EVALUATION_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessApproval> getEvaluationAccessApprovals(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value= EVALUATION_ID_PATH_VAR_WITHOUT_BRACKETS) String evaluationId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(evaluationId);
		subjectId.setType(RestrictableObjectType.EVALUATION);
		return serviceProvider.getAccessApprovalService().getAccessApprovals(userId, subjectId, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_WITH_APPROVAL_ID, method = RequestMethod.DELETE)
	public void deleteAccessApprovals(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String approvalId,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException {
		serviceProvider.getAccessApprovalService().deleteAccessApprovals(userId, approvalId);
	}
	
}
