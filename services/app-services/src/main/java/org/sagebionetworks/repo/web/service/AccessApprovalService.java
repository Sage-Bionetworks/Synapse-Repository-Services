package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalService {

	public AccessApproval createAccessApproval(Long userId,
			AccessApproval accessApproval) throws DatastoreException,
			UnauthorizedException, NotFoundException, InvalidModelException,
			IOException;

	public AccessApproval getAccessApproval(
			Long userId, String approvalId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

	public PaginatedResults<AccessApproval> getAccessApprovals(Long userId,
			RestrictableObjectDescriptor subjectId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	public void deleteAccessApprovals(Long userId, String approvalId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

}