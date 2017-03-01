package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DataAccessRequestManager {

	/**
	 * Create a new DataAccessRequest.
	 * 
	 * @param userInfo
	 * @param toCreate
	 * @return
	 */
	public DataAccessRequest create(UserInfo userInfo, DataAccessRequest toCreate);

	/**
	 * Retrieve the recent DataAccessRequest, which is created by or has associated research project owned by the user, for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public DataAccessRequestInterface getCurrent(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Retrieve a DataAccessRequest object suitable for update, which has the original one created by or has associated research project owned by the user, for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public DataAccessRequestInterface getDataAccessRequestForUpdate(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Update an existing DataAccessRequest.
	 * Only the creator of this object or the owner of the associated ResearchProject can perform this action.
	 * 
	 * @param userInfo
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public DataAccessRequestInterface update(UserInfo userInfo, DataAccessRequestInterface toUpdate) throws NotFoundException, UnauthorizedException;
}
