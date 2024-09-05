package org.sagebionetworks.repo.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipRequestService {
	/**
	 * 
	 * @param userId
	 * @param dto
	 * @return
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public MembershipRequest create(Long userId,
	                                MembershipRequest dto,
	                                String acceptRequestEndpoint,
	                                String notificationUnsubscribeEndpoint) throws UnauthorizedException, InvalidModelException, DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param requestorId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipRequest> getOpenRequests(Long userId, String requesterId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param requestorId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipRequest> getOpenRequestSubmissions(Long userId, String requesterId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param dtoId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public MembershipRequest get(Long userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param dtoId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public void delete(Long userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @return
	 */
	public Count getOpenMembershipRequestCount(Long userId);
}
