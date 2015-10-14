package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public interface MembershipInvitationService {
	
	/**
	 * 
	 * @param userId
	 * @param dto
	 * @param acceptInvitationEndpoint
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException 
	 */
	public MembershipInvtnSubmission create(Long userId,
			MembershipInvtnSubmission dto,
			String acceptInvitationEndpoint, 
			String notificationUnsubscribeEndpoint) throws UnauthorizedException, InvalidModelException, NotFoundException, ServiceUnavailableException;
	
	/**
	 * 
	 * @param inviteeId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipInvitation> getOpenInvitations(Long userId, String inviteeId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param inviteeId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipInvtnSubmission> getOpenInvitationSubmissions(Long userId, String inviteeId, String teamId, long limit, long offset)
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
	public MembershipInvtnSubmission get(Long userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param dtoId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public void delete(Long userId, String dtoId) throws DatastoreException, UnauthorizedException, NotFoundException;
}
