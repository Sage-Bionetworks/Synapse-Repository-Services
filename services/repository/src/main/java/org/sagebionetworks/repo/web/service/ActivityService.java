package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ActivityService {

	public Activity createActivity(String userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException;
	
	public Activity getActivity(String userId, String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;
	
	public Activity updateActivity(String userId, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException;

	public void deleteActivity(String userId, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public PaginatedResults<Activity> getActivitys(HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

}