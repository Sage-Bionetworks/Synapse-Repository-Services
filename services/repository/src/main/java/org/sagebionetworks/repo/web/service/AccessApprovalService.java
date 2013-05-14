package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface AccessApprovalService {

	public AccessApproval createAccessApproval(String userId,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException, InvalidModelException,
			IOException;

	public PaginatedResults<AccessApproval> getEntityAccessApprovals(String userId,
			String entityId, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public PaginatedResults<AccessApproval> getEvaluationAccessApprovals(String userId,
			String evaluationId, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public void deleteAccessApprovals(String userId, String approvalId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

}