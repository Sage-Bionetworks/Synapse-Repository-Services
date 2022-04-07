package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementService {

	AccessRequirement createAccessRequirement(Long userId,
			AccessRequirement accessRequirement) throws Exception;

	AccessRequirement createLockAccessRequirement(Long userId,
			String entityId) throws Exception;
	
	AccessRequirement getAccessRequirement(String requirementId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	PaginatedResults<AccessRequirement> getAccessRequirements(
			Long userId, RestrictableObjectDescriptor subjectId, Long limit, Long offset)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;
	
	AccessRequirement updateAccessRequirement(
			Long userId, String requirementId, AccessRequirement accessRequirement) throws Exception;


	void deleteAccessRequirements(Long userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException;

	AccessRequirement convertAccessRequirements(Long userId, AccessRequirementConversionRequest request);

	RestrictableObjectDescriptorResponse getSubjects(String requirementId, String nextPageToken);
	
	AccessControlList getAccessRequirementAcl(Long userId, String requirementId);
	
	AccessControlList createAccessRequirementAcl(Long userId, String requirementId, AccessControlList acl);
	
	AccessControlList updateAccessRequirementAcl(Long userId, String requirementId, AccessControlList acl);
	
	void deleteAccessRequirementAcl(Long userId, String requirementId);

}