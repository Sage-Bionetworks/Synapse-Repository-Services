package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.dataaccess.AccessRequestSortField;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.SortDirection;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.web.NotFoundException;

public interface RequestDAO {

	/**
	 * Create a new Request.
	 * 
	 * @param toCreate
	 * @return
	 */
	Request create(Request toCreate);

	/**
	 * Retrieve the current request that the user created (or own) for the given accessRequirementId.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	RequestInterface getUserOwnCurrentRequest(String accessRequirementId, String userId) throws NotFoundException;

	/**
	 * Update an existing Request.
	 * 
	 * @param toUpdate
	 * @return
	 * @throws NotFoundException
	 */
	RequestInterface update(RequestInterface toUpdate) throws NotFoundException;

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
	RequestInterface getForUpdate(String id);

	/**
	 * Retrieve the current Request.
	 * 
	 * @param id
	 * @return
	 */
	RequestInterface get(String id);
	
	/**
	 * @param requestId
	 * @return The id of the access requirement for the request with the given id
	 */
	String getAccessRequirementId(String requestId);

	/**
	 * Returns requests associated with the given user (as creator, PI, or collaborator).
	 */
	List<RequestUserInfo> getUserRequests(Long userId, long limit, long offset, AccessRequestSortField sortBy, SortDirection sortDirection);

	// For testing

	void truncateAll();
}
