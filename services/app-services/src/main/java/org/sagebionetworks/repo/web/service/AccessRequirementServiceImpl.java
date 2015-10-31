package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessRequirementServiceImpl implements AccessRequirementService {

	@Autowired
	AccessRequirementManager accessRequirementManager;	
	@Autowired
	UserManager userManager;

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
	
		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getUnmetAccessRequirements(userInfo, subjectId, accessType);
		
		return new PaginatedResults<AccessRequirement>(results.getResults(),
				(int)results.getTotalNumberOfResults());
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
			Long userId, RestrictableObjectDescriptor subjectId) 
			throws DatastoreException, UnauthorizedException, NotFoundException
			 {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getAccessRequirementsForSubject(userInfo, subjectId);
		
		return new PaginatedResults<AccessRequirement>(results.getResults(),
				(int)results.getTotalNumberOfResults());
	}
	
	@WriteTransaction
	@Override
	public void deleteAccessRequirements(Long userId, String requirementId) 
			throws DatastoreException, UnauthorizedException, NotFoundException
			 {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessRequirementManager.deleteAccessRequirement(userInfo, requirementId);
	}

}
