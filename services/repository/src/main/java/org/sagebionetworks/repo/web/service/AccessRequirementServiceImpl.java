package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessRequirementServiceImpl implements AccessRequirementService {

	@Autowired
	AccessRequirementManager accessRequirementManager;	
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@WriteTransaction
	@Override
	public AccessRequirement createAccessRequirement(Long userId, 
			AccessRequirement accessRequirement) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);

		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}
	

	@WriteTransaction
	@Override
	public AccessRequirement updateAccessRequirement(Long userId,
			String accessRequirementId, AccessRequirement accessRequirement) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.updateAccessRequirement(userInfo, accessRequirementId, accessRequirement);
	}
	
	
	@WriteTransaction
	@Override
	public AccessRequirement createLockAccessRequirement(Long userId, 
			String entityId) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.createLockAccessRequirement(userInfo, entityId);
	}
	
	@Override
	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirements(
			Long userId, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) 
			throws DatastoreException, UnauthorizedException, 
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
	
		List<AccessRequirement> results = 
			accessRequirementManager.getAllUnmetAccessRequirements(userInfo, subjectId, accessType);

		// This services is not actually paginated so PaginatedResults is being misused.
		return PaginatedResults.createMisusedPaginatedResults(results);
	}
	
	@Override
	public AccessRequirement getAccessRequirement(
			Long userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.getAccessRequirement(userInfo, requirementId);
	}



	@Override	
	public PaginatedResults<AccessRequirement> getAccessRequirements(
			Long userId, RestrictableObjectDescriptor subjectId, Long limit, Long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException
			 {
		UserInfo userInfo = userManager.getUserInfo(userId);

		List<AccessRequirement> results = 
			accessRequirementManager.getAccessRequirementsForSubject(userInfo, subjectId, limit, offset);
		
		// This services is not actually paginated so PaginatedResults is being misused.
		return PaginatedResults.createMisusedPaginatedResults(results);
	}
	
	@WriteTransaction
	@Override
	public void deleteAccessRequirements(Long userId, String requirementId) 
			throws DatastoreException, UnauthorizedException, NotFoundException
			 {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessRequirementManager.deleteAccessRequirement(userInfo, requirementId);
	}


	@Override
	public AccessRequirement updateVersion(Long userId, Long requirementId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.adminUpdateAccessRequirementVersion(userInfo, requirementId);
	}

}
