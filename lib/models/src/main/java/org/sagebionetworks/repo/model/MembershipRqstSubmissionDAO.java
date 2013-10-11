package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipRqstSubmissionDAO {
	/**
	 * @param dto object to be created
	 * @return the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public MembershipRqstSubmission create(MembershipRqstSubmission dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipRqstSubmission get(String id) throws DatastoreException, NotFoundException;
	

	/**
	 * Get the open (unexpired and unfulfilled) MembershipRqstSubmissions received by the given team
	 * 
	 * @param teamId
	 * @param now current time, expressed as a long
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<MembershipRequest> getOpenByTeamInRange(long teamId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

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
	 * Get the open (unexpired and unfulfilled) MembershipRqstSubmissions received by the given team from a given requestor
	 * 
	 * @param teamId
	 * @param requestorId
	 * @param now current time, expressed as a long
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<MembershipRequest> getOpenByTeamAndRequestorInRange(long teamId, long requestorId, long now, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param requestorId
	 * @param now
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getOpenByTeamAndRequestorCount(long teamId, long requestorId, long now) throws DatastoreException, NotFoundException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
