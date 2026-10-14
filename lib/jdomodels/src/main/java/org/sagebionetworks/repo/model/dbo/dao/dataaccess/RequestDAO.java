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
	 * Set the hash of the eDUC-relevant request content that was last applied to the routed
	 * signature envelope. This is a server-managed value that is not part of the request DTO, so
	 * it is not affected by a user editing their request. It is updated only when an envelope is
	 * routed or corrected.
	 * <p>
	 * The request's etag is rotated, so a client holding the previous etag must re-fetch the request
	 * before updating it.
	 *
	 * @param requestId the request whose hash to set
	 * @param hash the content hash, or null to clear it
	 */
	void setEDucContentHash(String requestId, String hash);

	/**
	 * @param requestId
	 * @return The hash of the eDUC-relevant request content last applied to the routed envelope,
	 *         or null if none has been recorded.
	 */
	String getEDucContentHash(String requestId);

	/**
	 * Returns requests associated with the given user (as creator, PI, or collaborator).
	 * <p>
	 * The two filters are independent; each is applied only when it is provided.
	 *
	 * @param isEDuc when true, limits the results to requests that have a DUC envelope; when false,
	 *        to those that do not; when null, requests are returned regardless
	 * @param accessRequirementId when non-null, limits the results to requests for that access
	 *        requirement
	 */
	List<RequestUserInfo> getUserRequests(Long userId, Boolean isEDuc, Long accessRequirementId, long limit,
			long offset, AccessRequestSortField sortBy, SortDirection sortDirection);

	// For testing

	void truncateAll();
}
