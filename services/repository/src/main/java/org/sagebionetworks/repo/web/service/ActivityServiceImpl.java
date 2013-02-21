package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.ActivityManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivityServiceImpl implements ActivityService {

	@Autowired
	ActivityManager activityManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	UserManager userManager;
	
	public ActivityServiceImpl() {	}
	
	/**
	 * For Testing purposes
	 * @param activityManager
	 * @param entityManager
	 * @param userManager
	 */
	public ActivityServiceImpl(ActivityManager activityManager,
			EntityManager entityManager, UserManager userManager) {
		super();
		this.activityManager = activityManager;
		this.entityManager = entityManager;
		this.userManager = userManager;
	}

	@Override
	public Activity createActivity(String userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		fillCurrentVersions(activity);
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
		fillCurrentVersions(activity);
		return activityManager.updateActivity(userInfo, activity);
	}
	
	@Override
	public void deleteActivity(String userId, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		activityManager.deleteActivity(userInfo, activityId);		
	}

	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(String userId,
			String activityId, int limit, int offset) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.getEntitiesGeneratedBy(userInfo, activityId, limit, offset);
	}

	/*
	 * Private Methods
	 */
	private void fillCurrentVersions(Activity activity) {
		// fina any used entities where version is not specified
		Map<String,List<UsedEntity>> entityIdToUsedEntity = new HashMap<String, List<UsedEntity>>();
		List<String> lookupCurrentVersion = new ArrayList<String>();
		if(activity.getUsed() != null) {
			for(UsedEntity ue : activity.getUsed()) {
				Reference ref = ue.getReference();
				if(ref != null && ref.getTargetId() != null && ref.getTargetVersionNumber() == null) {
					lookupCurrentVersion.add(ref.getTargetId());
					if(!entityIdToUsedEntity.containsKey(ref.getTargetId())) 
						entityIdToUsedEntity.put(ref.getTargetId(), new ArrayList<UsedEntity>());
					entityIdToUsedEntity.get(ref.getTargetId()).add(ue);
				}
			}
		}
		// look up current versions and set into UsedEntity
		if(lookupCurrentVersion.size() > 0) {
			List<Reference> currentVersions = entityManager.getCurrentRevisionNumbers(lookupCurrentVersion);
			for(Reference ref : currentVersions) {
				List<UsedEntity> ueList = entityIdToUsedEntity.get(ref.getTargetId());
				if(ueList != null) {
					for(UsedEntity ue : ueList) {
						if(ue.getReference() != null) {
							ue.getReference().setTargetVersionNumber(ref.getTargetVersionNumber());
						}
					}
				}
			}
		}
	}	
}
