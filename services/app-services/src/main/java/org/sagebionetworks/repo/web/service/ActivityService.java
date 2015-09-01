package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ActivityService {

	public Activity createActivity(Long userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException;
	
	public Activity getActivity(Long userId, String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;
	
	public Activity updateActivity(Long userId, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException;

	public void deleteActivity(Long userId, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	public PaginatedResults<Reference> getEntitiesGeneratedBy(Long userId,
			String activityId, int limit, int offset) throws NotFoundException,
			DatastoreException, UnauthorizedException;

}