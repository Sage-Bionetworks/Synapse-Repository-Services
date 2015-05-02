package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipInvtnSubmissionDAO {
	/**
	 * @param dto object to be created
	 * @return the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public MembershipInvtnSubmission create(MembershipInvtnSubmission dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvtnSubmission get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @param now
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<MembershipInvtnSubmission> getOpenSubmissionsByTeamInRange(
			long teamId, long now, long limit, long offset);

	/**
	 * 
	 * @param teamId
	 * @param userId
	 * @param now
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<MembershipInvtnSubmission> getOpenSubmissionsByTeamAndUserInRange(
			long teamId, long userId, long now, long limit, long offset);
	
	/**
	 * 
	 * @param teamId
	 * @param userId
	 * @return
	 */
	public List<String> getInvitersByTeamAndUser(long teamId, long userId, long now);

	/**
	 * Get the open (unexpired and unfulfilled) MembershipInvtnSubmissions received by the given user
	 * 
	 * @param userId
	 * @param now current time, expressed as a long
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<MembershipInvitation> getOpenByUserInRange(long userId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get the open (unexpired and unfulfilled) MembershipInvtnSubmissions received by the given user from a given team
	 * @param teamId
	 * @param userId
	 * @param now
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<MembershipInvitation> getOpenByTeamAndUserInRange(long teamId, long userId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByUserCount(long userId, long now) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByTeamCount(long teamId, long now) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param userId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByTeamAndUserCount(long teamId, long userId, long now) throws DatastoreException, NotFoundException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @param inviteeId
	 * @throws DatastoreException
	 */
	public void deleteByTeamAndUser(long teamId, long inviteeId) throws DatastoreException;



}
