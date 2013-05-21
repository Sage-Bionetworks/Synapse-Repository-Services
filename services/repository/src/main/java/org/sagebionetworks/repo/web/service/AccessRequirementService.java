package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementService {

	public AccessRequirement createAccessRequirement(String userId,
			AccessRequirement accessRequirement) throws Exception;

	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirements(
			String userId, RestrictableObjectDescriptor subjectId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			String userId, RestrictableObjectDescriptor subjectId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;


	public void deleteAccessRequirements(String userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

}