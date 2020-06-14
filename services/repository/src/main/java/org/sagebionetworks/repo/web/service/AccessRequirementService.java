package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementService {

	public AccessRequirement createAccessRequirement(Long userId,
			AccessRequirement accessRequirement) throws Exception;

	public AccessRequirement createLockAccessRequirement(Long userId,
			String entityId) throws Exception;
	
	public AccessRequirement getAccessRequirement(String requirementId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			Long userId, RestrictableObjectDescriptor subjectId, Long limit, Long offset)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;
	
	public AccessRequirement updateAccessRequirement(
			Long userId, String requirementId, AccessRequirement accessRequirement) throws Exception;


	public void deleteAccessRequirements(Long userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

	public AccessRequirement convertAccessRequirements(Long userId, AccessRequirementConversionRequest request);

	public RestrictableObjectDescriptorResponse getSubjects(String requirementId, String nextPageToken);

}