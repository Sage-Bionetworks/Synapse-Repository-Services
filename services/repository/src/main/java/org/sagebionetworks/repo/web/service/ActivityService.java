package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ActivityService {

	public Activity createActivity(Activity activity) throws Exception;

	public PaginatedResults<Activity> getActivitys(HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public void deleteActivity(String userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

}