package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DataAccessRequestDAO {

	/**
	 * Create a new DataAccessRequest.
	 * 
	 * @param toCreate
	 * @return
	 */
	public DataAccessRequestInterface create(DataAccessRequestInterface toCreate);

	/**
	 * Retrieve the current request that the user created (or own) for the given accessRequirementId.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	public DataAccessRequestInterface getCurrentRequest(String accessRequirementId, String userId) throws NotFoundException;

	/**
	 * Update an existing DataAccessRequest.
	 * 
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 */
	public DataAccessRequestInterface update(DataAccessRequestInterface toUpdate) throws NotFoundException;

	/**
	 * used for tests
	 */
	void truncateAll();

	/**
	 * Retrieve the current DataAccessRequest for update.
	 * 
	 * @param id
	 * @effect this call will put a lock on the returned object.
	 * @return
	 */
	public DataAccessRequestInterface getForUpdate(String id);
}
