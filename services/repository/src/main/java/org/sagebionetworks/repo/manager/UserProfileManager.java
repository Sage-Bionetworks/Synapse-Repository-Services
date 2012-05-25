package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserProfileManager {

	/**
	 * Get an existing UserProfile
	 * @param userInfo
	 * @param ownerid
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserProfile getUserProfile(UserInfo userInfo, String ownerid) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the public profiles of the users in the system, paginated
	 * 
	 * @param userInfo
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @return
	 */
	public QueryResults<UserProfile> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException;
	
	/**
	 * Update a UserProfile.
	 * @param userInfo
	 * @param updated
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public UserProfile updateUserProfile(UserInfo userInfo, UserProfile updated) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;
	
}
