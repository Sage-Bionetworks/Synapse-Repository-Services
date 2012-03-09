package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.model.UserProfile;

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
	
	// TODO delete the commented lines
//	/**
//	 * Get an existing UserProfile
//	 * @param userInfo
//	 * @param userName
//	 * @return
//	 * @throws UnauthorizedException 
//	 * @throws DatastoreException 
//	 * @throws NotFoundException 
//	 */
//	public UserProfile getUserProfileFromName(UserInfo userInfo, String userName) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
