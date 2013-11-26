package org.sagebionetworks.bridge.model;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CommunityTeamDAO {
	/**
	 * create a team community mappings
	 * 
	 * @param communityId
	 * @param teamId
	 * @throws DatastoreException
	 */
	public void create(long communityId, long teamId) throws DatastoreException;

	/**
	 * Retrieves the community id for the give team id
	 * 
	 * @param teamId the team for which we want a community
	 * @return the community id
	 * @throws DatastoreException
	 * @throws NotFoundException if the teams is not a community team
	 */
	public long getCommunityId(long teamId) throws DatastoreException, NotFoundException;

	/**
	 * Retrieves the community ids for the give member
	 * 
	 * @param memberId the member for whom we want communities
	 * @return the list of unique community ids (order not defined)
	 * @throws DatastoreException
	 * @throws NotFoundException if any of the teams is not a community team
	 */
	public List<Long> getCommunityIdsByMember(String memberId) throws DatastoreException, NotFoundException;

	/**
	 * Retrieves all the community ids
	 * 
	 * @return the list of unique community ids (order not defined)
	 * @throws DatastoreException
	 * @throws NotFoundException if any of the teams is not a community team
	 */
	public List<Long> getCommunityIds() throws DatastoreException, NotFoundException;
}
