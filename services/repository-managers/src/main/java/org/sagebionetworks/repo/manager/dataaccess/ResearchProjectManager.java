package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ResearchProjectManager {

	/**
	 * Create a new ResearchProject.
	 * 
	 * @param userInfo
	 * @param toCreate
	 * @return
	 */
	public ResearchProject create(UserInfo userInfo, ResearchProject toCreate);

	/**
	 * Retrieve an existing ResearchProject that owned by the user for a given accessRequirementId.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	public ResearchProject getUserOwnResearchProject(UserInfo userInfo, String accessRequirementId) throws NotFoundException;

	/**
	 * Update an existing ResearchProject.
	 * Only owner can perform this action.
	 * 
	 * @param userInfo
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public ResearchProject update(UserInfo userInfo, ResearchProject toUpdate) throws NotFoundException, UnauthorizedException; 

	/**
	 * Change the ownership of an existing ResearchProject.
	 * Only ACT member can perform this action.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public ResearchProject changeOwnership(UserInfo userInfo, ChangeOwnershipRequest request)  throws NotFoundException, UnauthorizedException;
}
