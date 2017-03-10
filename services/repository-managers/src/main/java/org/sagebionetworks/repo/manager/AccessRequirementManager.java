package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementManager {
	
	/**
	 *  create access requirement
	 */
	public <T extends AccessRequirement> T  createAccessRequirement(UserInfo userInfo, T AccessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param requirementId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AccessRequirement getAccessRequirement(UserInfo userInfo, String requirementId) throws DatastoreException, NotFoundException;
	
	/**
	 *  get all the access requirements for an entity
	 */
	public List<AccessRequirement> getAllAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 *  get a page of the access requirements for an entity
	 */
	public List<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 *  get a page of the unmet access requirements
	 *  This API includes an authorization check to see if
	 *  the user is allowed to READ the entity
	 * @param offset 
	 * @param limit 
	 *  
	 */
	public List<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType, Long limit, Long offset) throws DatastoreException, NotFoundException;
	
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
	 * Create an ACTAccessRequirement on an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public ACTAccessRequirement createLockAccessRequirement(UserInfo userInfo,
			String entityId) throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException;

	// will be removed after the ACT feature
	public List<AccessRequirement> getAllUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId,
			ACCESS_TYPE accessType) throws DatastoreException, NotFoundException;
}
