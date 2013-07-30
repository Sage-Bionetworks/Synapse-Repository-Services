package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementService {

	public AccessRequirement createAccessRequirement(String userId,
			AccessRequirement accessRequirement) throws Exception;

	public AccessRequirement createLockAccessRequirement(String userId,
			String entityId) throws Exception;

	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirements(
			String userId, RestrictableObjectDescriptor subjectId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			String userId, RestrictableObjectDescriptor subjectId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;


	public void deleteAccessRequirements(String userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

}