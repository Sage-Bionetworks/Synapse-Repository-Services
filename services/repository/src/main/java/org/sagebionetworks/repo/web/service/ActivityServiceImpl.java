package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.ActivityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivityServiceImpl implements ActivityService {

	@Autowired
	ActivityManager activityManager;	
	@Autowired
	UserManager userManager;
	
	@Override
	public Activity createActivity(String userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);		
		String id = activityManager.createActivity(userInfo, activity);
		return getActivity(userId, id);
	}
	
	@Override
	public Activity getActivity(String userId, String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.getActivity(userInfo, activityId);
	}

	@Override
	public Activity updateActivity(String userId, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.updateActivity(userInfo, activity);
	}
	
	@Override
	public void deleteActivity(String userId, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		activityManager.deleteActivity(userInfo, activityId);		
	}
	
}
