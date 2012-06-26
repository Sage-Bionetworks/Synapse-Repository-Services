package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementManager {
	
	/**
	 *  create access requirement
	 * @throws ForbiddenException 
	 */
	public <T extends AccessRequirement> T  createAccessRequirement(UserInfo userInfo, T AccessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ForbiddenException;
	
	/**
	 *  get all the access requirements for an entity
	 * @throws ForbiddenException 
	 */
	public QueryResults<AccessRequirement> getAccessRequirementsForEntity(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, ForbiddenException;
	
	/**
	 *  get all the unmet access requirements
	 *  This API wraps 'getUnmetAccessRequirementIntern', 
	 *  and includes an authorization check to see if
	 *  the user is allowed to READ the entity
	 *  
	 * @throws ForbiddenException 
	 */
	public QueryResults<AccessRequirement> getUnmetAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, ForbiddenException;
	
	/**
	 * This API is for internal use and skips the authorization check  
	 * (see also 'getUnmetAccessRequirement' which includes the authorization check)
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	QueryResults<AccessRequirement> getUnmetAccessRequirementIntern(
			UserInfo userInfo, String entityId) throws DatastoreException,
			NotFoundException;
	/**
	 *  update an access requirement
	 * @throws ForbiddenException 
	 */
	public <T extends AccessRequirement> T  updateAccessRequirement(UserInfo userInfo, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, ForbiddenException, DatastoreException;
	
	/*
	 *  delete an access requirement
	 */
	public void deleteAccessRequirement(UserInfo userInfo, String accessRequirementId) throws NotFoundException, DatastoreException, UnauthorizedException, ForbiddenException;

	
}
