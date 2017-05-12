package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface RequestManager {

	/**
	 * Create a new Request.
	 * 
	 * @param userInfo
	 * @param toCreate
	 * @return
	 */
	public Request create(UserInfo userInfo, Request toCreate);

	/**
	 * Retrieve the recent Request, which is created by or has associated research project owned by the user, for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public RequestInterface getUserOwnCurrentRequest(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Retrieve a Request object suitable for update, which has the original one created by or has associated research project owned by the user, for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public RequestInterface getRequestForUpdate(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Update an existing Request.
	 * Only the creator of this object or the owner of the associated ResearchProject can perform this action.
	 * 
	 * @param userInfo
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public RequestInterface update(UserInfo userInfo, RequestInterface toUpdate) throws NotFoundException, UnauthorizedException;

	/**
	 * Create or update a Request
	 * 
	 * @param user
	 * @param toCreateOrUpdate
	 * @return
	 */
	public RequestInterface createOrUpdate(UserInfo user, RequestInterface toCreateOrUpdate);
}
