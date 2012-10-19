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
import org.sagebionetworks.repo.model.provenance.ActivityType;
import org.sagebionetworks.repo.web.ForbiddenException;
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
		
	public ActivityManagerImpl(IdGenerator idGenerator,
			ActivityDAO activityDAO, AuthorizationManager authorizationManager) {
		super();
		this.idGenerator = idGenerator;
		this.activityDAO = activityDAO;
		this.authorizationManager = authorizationManager;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Activity> T createActivity(UserInfo userInfo, T activity)
			throws DatastoreException, InvalidModelException {		

		// for idGenerator based id on create, regardless of what is passed
		activity.setId(idGenerator.generateNewId().toString());

		populateCreationFields(userInfo, activity);
		// set default type
		if(activity.getActivityType() == null) activity.setActivityType(ActivityType.UNDEFINED);
		return activityDAO.create(activity);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Activity> T updateActivity(UserInfo userInfo, T activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {
		
		// only owner can change
		UserInfo.validateUserInfo(userInfo);
		String requestorId = userInfo.getUser().getId();
		String requestorName = userInfo.getUser().getDisplayName();
		Activity currentAct = activityDAO.get(activity.getId().toString());
		if(!currentAct.getCreatedBy().equals(requestorId)) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
		
		// update
		activityDAO.lockActivityAndIncrementEtag(activity.getId().toString(), activity.getEtag(), ChangeType.UPDATE);	
		if(log.isDebugEnabled()){
			log.debug("username "+requestorName+" updated activity: "+currentAct.getId());
		}
		populateModifiedFields(userInfo, activity);
		if(activity.getActivityType() == null) activity.setActivityType(ActivityType.UNDEFINED);
		return activityDAO.update(activity);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteActivity(UserInfo userInfo, String activityId) throws NotFoundException, DatastoreException, UnauthorizedException {		
		Activity activity = activityDAO.get(activityId);
		UserInfo.validateUserInfo(userInfo);
		String requestorId = userInfo.getUser().getId();
		String requestorName = userInfo.getUser().getDisplayName();
		// only owner can change
		if(!activity.getCreatedBy().equals(requestorId)) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
		
		// delete
		activityDAO.lockActivityAndIncrementEtag(activity.getId().toString(), activity.getEtag(), ChangeType.DELETE);
		if(log.isDebugEnabled()){
			log.debug("username "+requestorName+" deleted activity: "+activityId);
		}
				
		activityDAO.delete(activity.getId().toString());
	}

	@Override
	public Activity getActivity(UserInfo userInfo, String activityId) 
		throws DatastoreException, NotFoundException, UnauthorizedException {
//		Activity act = activityDAO.get(activityId);
//		// use must be able to see at least one used and 
//		if(!activity.getCreatedBy().equals(requestorId)) {
//			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
//		}
		
		// get Activity from DAO by id
		//   - if not found: NotFoundException
		
		// Authorization
		// Pass if:
		//  1) user can see one of the entities that wasGeneratedBy this activity?
		
		//TODO : finish
		
		return null;
	}

	
	
	/*
	 * Private Methods
	 */
	private static void populateCreationFields(UserInfo userInfo, Activity a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getIndividualGroup().getId());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	private static void populateModifiedFields(UserInfo userInfo, Activity a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

}
