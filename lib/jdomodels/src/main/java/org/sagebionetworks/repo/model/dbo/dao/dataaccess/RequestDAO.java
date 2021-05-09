package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface RequestDAO {

	/**
	 * Create a new Request.
	 * 
	 * @param toCreate
	 * @return
	 */
	public Request create(Request toCreate);

	/**
	 * Retrieve the current request that the user created (or own) for the given accessRequirementId.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	public RequestInterface getUserOwnCurrentRequest(String accessRequirementId, String userId) throws NotFoundException;

	/**
	 * Update an existing Request.
	 * 
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 */
	public RequestInterface update(RequestInterface toUpdate) throws NotFoundException;

	/**
	 * used for tests
	 */
	void delete(String id);

	/**
	 * Retrieve the current Request for update.
	 * 
	 * @param id
	 * @effect this call will put a lock on the returned object.
	 * @return
	 */
	public RequestInterface getForUpdate(String id);

	/**
	 * Retrieve the current Request.
	 * 
	 * @param id
	 * @return
	 */
	public RequestInterface get(String id);

	// For testing

	void truncateAll();
}
