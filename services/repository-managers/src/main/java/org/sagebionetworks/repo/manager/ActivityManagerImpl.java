package org.sagebionetworks.repo.manager;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ActivityManagerImpl implements ActivityManager {
	static private Log log = LogFactory.getLog(ActivityManagerImpl.class);

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ActivityDAO activityDAO;	
	@Autowired
	AuthorizationManager authorizationManager;	

	/**
	 * For testing
	 * @param idGenerator
	 * @param activityDAO
	 * @param authorizationManager
	 */
	public ActivityManagerImpl(IdGenerator idGenerator,
			ActivityDAO activityDAO, AuthorizationManager authorizationManager) {
		super();
		this.idGenerator = idGenerator;
		this.activityDAO = activityDAO;
		this.authorizationManager = authorizationManager;
	}

	public ActivityManagerImpl() { }
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createActivity(UserInfo userInfo, Activity activity)
			throws DatastoreException, InvalidModelException {		

		// for idGenerator based id on create, regardless of what is passed
		activity.setId(idGenerator.generateNewId().toString());

		populateCreationFields(userInfo, activity);
		return activityDAO.create(activity);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Activity updateActivity(UserInfo userInfo, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {
		
		// only owner can change
		UserInfo.validateUserInfo(userInfo);
		String requestorId = userInfo.getIndividualGroup().getId();
		String requestorName = userInfo.getIndividualGroup().getName();
		Activity currentAct = activityDAO.get(activity.getId());
		if(!currentAct.getCreatedBy().equals(requestorId) && !userInfo.isAdmin()) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
		
		// lock and get new etag
		String neweTag = activityDAO.lockActivityAndGenerateEtag(activity.getId(), activity.getEtag(), ChangeType.UPDATE);
		activity.setEtag(neweTag);
		
		if(log.isDebugEnabled()){
			log.debug("username "+requestorName+" updated activity: "+currentAct.getId());
		}
		populateModifiedFields(userInfo, activity);
		// update
		return activityDAO.update(activity);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteActivity(UserInfo userInfo, String activityId) throws DatastoreException, UnauthorizedException {				
		Activity activity;
		try {
			activity = activityDAO.get(activityId);
		} catch (NotFoundException ex) {
			return; // don't bug people with 404s on delete
		}
		UserInfo.validateUserInfo(userInfo);
		String requestorId = userInfo.getIndividualGroup().getId();
		String requestorName = userInfo.getIndividualGroup().getName();
		// only owner can change
		if(!activity.getCreatedBy().equals(requestorId) && !userInfo.isAdmin()) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
				
		activityDAO.sendDeleteMessage(activity.getId());
		activityDAO.delete(activity.getId());
	}

	@Override
	public Activity getActivity(UserInfo userInfo, String activityId) 
		throws DatastoreException, NotFoundException, UnauthorizedException {		
		Activity act = activityDAO.get(activityId);
		if(!authorizationManager.canAccessActivity(userInfo, activityId)) { 			
			throw new UnauthorizedException(userInfo.getIndividualGroup().getName() +" lacks access to the requested object.");
		}
		return act;
	}

	@Override
	public boolean doesActivityExist(String id) {
		return activityDAO.doesActivityExist(id);
	}
	
	
	/*
	 * Private Methods
	 */
	static void populateCreationFields(UserInfo userInfo, Activity a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getIndividualGroup().getId());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	static void populateModifiedFields(UserInfo userInfo, Activity a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

}
