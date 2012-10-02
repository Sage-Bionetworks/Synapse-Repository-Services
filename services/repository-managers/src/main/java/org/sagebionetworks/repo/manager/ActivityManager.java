package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ActivityManager {
	
	/**
	 * create Activity
	 * @param <T>
	 * @param userInfo
	 * @param activity
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public <T extends Activity> T  createActivity(UserInfo userInfo, T activity) throws DatastoreException, InvalidModelException;
	

	/**
	 * update an Activity
	 * @param <T>
	 * @param userInfo
	 * @param activity
	 * @return
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Activity> T  updateActivity(UserInfo userInfo, T activity) throws InvalidModelException, NotFoundException, ConflictingUpdateException, DatastoreException, UnauthorizedException;
	
	/**
	 * delete an Activity
	 * @param userInfo
	 * @param activityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void deleteActivity(UserInfo userInfo, String activityId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get activity for a given activity id
	 * @param userInfo
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ForbiddenException
	 */
	public Activity getActivity(UserInfo userInfo, String activityId) throws DatastoreException, NotFoundException, UnauthorizedException;

}
