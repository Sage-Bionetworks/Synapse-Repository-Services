package org.sagebionetworks.repo.web.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.ActivityManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;
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
	public Activity createActivity(Long userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		verifyUsedEntries(activity);
		String id = activityManager.createActivity(userInfo, activity);
		return getActivity(userId, id);
	}
	
	@Override
	public Activity getActivity(Long userId, String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.getActivity(userInfo, activityId);
	}

	@Override
	public Activity updateActivity(Long userId, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		verifyUsedEntries(activity);
		return activityManager.updateActivity(userInfo, activity);
	}
	
	@Override
	public void deleteActivity(Long userId, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		activityManager.deleteActivity(userInfo, activityId);		
	}

	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(Long userId,
			String activityId, int limit, int offset) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.getEntitiesGeneratedBy(userInfo, activityId, limit, offset);
	}

	/*
	 * Private Methods
	 */
	private void verifyUsedEntries(Activity activity) throws InvalidModelException {
		// Validate Used Entires
		// For UsedEntity: identify UsedEntities where version is not specified
		// For UsedURL: validate that URL is properly formed
		Map<String,List<UsedEntity>> entityIdToUsedEntity = new HashMap<String, List<UsedEntity>>();
		List<String> lookupCurrentVersion = new ArrayList<String>();
		if(activity.getUsed() != null) {
			for(Used used : activity.getUsed()) {
				if(used instanceof UsedEntity) {
					processUsedEntity(entityIdToUsedEntity, lookupCurrentVersion, (UsedEntity)used);
				} else if(used instanceof UsedURL) {
					processUsedURL((UsedURL) used);
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

	private void processUsedURL(UsedURL used) throws InvalidModelException {
		try {
			new URL(used.getUrl());
		} catch (MalformedURLException e) {
			throw new InvalidModelException("UsedURL url field is malformed: "+ used.getUrl());
		}
	}

	private void processUsedEntity(
			Map<String, List<UsedEntity>> entityIdToUsedEntity,
			List<String> lookupCurrentVersion, UsedEntity ue) {
		Reference ref = ue.getReference();
		if(ref != null && ref.getTargetId() != null && ref.getTargetVersionNumber() == null) {
			lookupCurrentVersion.add(ref.getTargetId());
			if(!entityIdToUsedEntity.containsKey(ref.getTargetId())) 
				entityIdToUsedEntity.put(ref.getTargetId(), new ArrayList<UsedEntity>());
			entityIdToUsedEntity.get(ref.getTargetId()).add(ue);
		}
	}	
}
