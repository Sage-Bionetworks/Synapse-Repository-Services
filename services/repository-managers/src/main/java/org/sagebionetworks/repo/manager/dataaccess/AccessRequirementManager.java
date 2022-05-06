package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementManager {
	
	/**
	 *  create access requirement
	 */
	<T extends AccessRequirement> T  createAccessRequirement(UserInfo userInfo, T AccessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param requirementId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	AccessRequirement getAccessRequirement(String requirementId) throws DatastoreException, NotFoundException;

	/**
	 *  get a page of the access requirements for an entity
	 */
	List<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 *  update an access requirement
	 *
	 */
	<T extends AccessRequirement> T  updateAccessRequirement(UserInfo userInfo, String acccessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException;
	
	/*
	 *  delete an access requirement
	 */
	void deleteAccessRequirement(UserInfo userInfo, String accessRequirementId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Create an LockAccessRequirement on an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	LockAccessRequirement createLockAccessRequirement(UserInfo userInfo,
			String entityId) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException;

	/**
	 * Convert an ACTAccessRequirement to a ManagedACTAccessRequirement
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	AccessRequirement convertAccessRequirement(UserInfo userInfo, AccessRequirementConversionRequest request)
			throws NotFoundException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Retrieve a page of subjects for a given accessRequirementId
	 * 
	 * @param accessRequirementId
	 * @param nextPageToken
	 * @return
	 */
	RestrictableObjectDescriptorResponse getSubjects(String accessRequirementId,
			String nextPageToken);
	
	/**
	 * Fetch the ACL assigned to the AR with the given id if any
	 * 
	 * @param userInfo 
	 * @param accessRequirementId The id of the AR
	 * @return The ACL assigned to the AR if any
	 */
	AccessControlList getAccessRequirementAcl(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Creates an ACL for the access requirement with the given id
	 * 
	 * @param userInfo The user creating the ACL, must be a member of the ACT
	 * @param accessRequirementId The if of the access requirement
	 * @param acl The ACL to assign to the AR, can only contain the REVIEW_SUBMISSION ACCESS_TYPE
	 * @return
	 */
	AccessControlList createAccessRequirementAcl(UserInfo userInfo, String accessRequirementId, AccessControlList acl) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Updates the ACL for the access requirement with the given id, the caller must be a member of the ACT
	 * 
	 * @param userInfo The user updating the AR acl, must be a member of the ACT
	 * @param accessRequirementId The id of the access requirement
	 * @param acl The ACL to assign to the AR, can only contain the REVIEW_SUBMISSION ACCESS_TYPE
	 * @return The updated ACL
	 */
	AccessControlList updateAccessRequirementAcl(UserInfo userInfo, String accessRequirementId, AccessControlList acl) throws NotFoundException, UnauthorizedException;

	/**
	 * Deletes the ACL assigned to the access requirement with the given id
	 * 
	 * @param userInfo The user deleting the AR acl, must be a member of the ACT
	 * @param accessRequirementId The id of the access requirement
	 */
	void deleteAccessRequirementAcl(UserInfo userInfo, String accessRequirementId) throws NotFoundException, UnauthorizedException;
	
}
