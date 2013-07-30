package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalService {

	public AccessApproval createAccessApproval(String userId,
			AccessApproval accessApproval) throws DatastoreException,
			UnauthorizedException, NotFoundException, InvalidModelException,
			IOException;

	public PaginatedResults<AccessApproval> getAccessApprovals(String userId,
			RestrictableObjectDescriptor subjectId, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	public void deleteAccessApprovals(String userId, String approvalId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

}