package org.sagebionetworks.repo.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementServiceImpl implements AccessRequirementService {

	private AccessRequirementManager accessRequirementManager;
	private UserManager userManager;

	@Autowired
	public AccessRequirementServiceImpl(AccessRequirementManager accessRequirementManager, UserManager userManager) {
		this.accessRequirementManager = accessRequirementManager;
		this.userManager = userManager;
	}

	@WriteTransaction
	@Override
	public AccessRequirement createAccessRequirement(Long userId, AccessRequirement accessRequirement) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);

		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}

	@WriteTransaction
	@Override
	public AccessRequirement updateAccessRequirement(Long userId, String accessRequirementId, AccessRequirement accessRequirement)
			throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.updateAccessRequirement(userInfo, accessRequirementId, accessRequirement);
	}

	@WriteTransaction
	@Override
	public AccessRequirement createLockAccessRequirement(Long userId, String entityId) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.createLockAccessRequirement(userInfo, entityId);
	}

	@Override
	public AccessRequirement getAccessRequirement(String requirementId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return accessRequirementManager.getAccessRequirement(requirementId);
	}

	@Override
	public PaginatedResults<AccessRequirement> getAccessRequirements(Long userId, RestrictableObjectDescriptor subjectId, Long limit,
			Long offset) throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		List<AccessRequirement> results = accessRequirementManager.getAccessRequirementsForSubject(userInfo, subjectId, limit, offset);

		// This services is not actually paginated so PaginatedResults is being misused.
		return PaginatedResults.createMisusedPaginatedResults(results);
	}

	@WriteTransaction
	@Override
	public void deleteAccessRequirements(Long userId, String requirementId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessRequirementManager.deleteAccessRequirement(userInfo, requirementId);
	}

	@Override
	public AccessRequirement convertAccessRequirements(Long userId, AccessRequirementConversionRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.convertAccessRequirement(userInfo, request);
	}

	@Override
	public RestrictableObjectDescriptorResponse getSubjects(String requirementId, String nextPageToken) {
		return accessRequirementManager.getSubjects(requirementId, nextPageToken);
	}

	@Override
	public AccessControlList getAccessRequirementAcl(Long userId, String requirementId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.getAccessRequirementAcl(userInfo, requirementId);
	}

	@Override
	public AccessControlList createAccessRequirementAcl(Long userId, String requirementId, AccessControlList acl) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.createAccessRequirementAcl(userInfo, requirementId, acl);
	}

	@Override
	public AccessControlList updateAccessRequirementAcl(Long userId, String requirementId, AccessControlList acl) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return accessRequirementManager.updateAccessRequirementAcl(userInfo, requirementId, acl);
	}

	@Override
	public void deleteAccessRequirementAcl(Long userId, String requirementId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		accessRequirementManager.deleteAccessRequirementAcl(userInfo, requirementId);
	}
	
	@Override
	public AccessRequirementSearchResponse searchAccessRequirements(AccessRequirementSearchRequest request) {
		return accessRequirementManager.searchAccessRequirements(request);
	}

}
