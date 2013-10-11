package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
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
	public MembershipRqstSubmission create(String userId,
			MembershipRqstSubmission dto) throws UnauthorizedException, InvalidModelException, DatastoreException, NotFoundException;
	
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
	public PaginatedResults<MembershipRequest> getOpenRequests(String userId, String requestorId, String teamId, long limit, long offset)
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
	public MembershipRqstSubmission get(String userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param dtoId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public void delete(String userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;
}
