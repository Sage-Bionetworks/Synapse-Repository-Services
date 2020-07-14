package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

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
	public <T extends AccessRequirement> T  createAccessRequirement(UserInfo userInfo, T AccessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param requirementId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AccessRequirement getAccessRequirement(String requirementId) throws DatastoreException, NotFoundException;

	/**
	 *  get a page of the access requirements for an entity
	 */
	public List<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 *  update an access requirement
	 *
	 */
	public <T extends AccessRequirement> T  updateAccessRequirement(UserInfo userInfo, String acccessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException;
	
	/*
	 *  delete an access requirement
	 */
	public void deleteAccessRequirement(UserInfo userInfo, String accessRequirementId) throws NotFoundException, DatastoreException, UnauthorizedException;

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
	public LockAccessRequirement createLockAccessRequirement(UserInfo userInfo,
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
	public AccessRequirement convertAccessRequirement(UserInfo userInfo, AccessRequirementConversionRequest request)
			throws NotFoundException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Retrieve a page of subjects for a given accessRequirementId
	 * 
	 * @param accessRequirementId
	 * @param nextPageToken
	 * @return
	 */
	public RestrictableObjectDescriptorResponse getSubjects(String accessRequirementId,
			String nextPageToken);

}
