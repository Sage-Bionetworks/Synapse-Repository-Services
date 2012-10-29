package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.ActivityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.QueryUtils;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.sagebionetworks.repo.web.controller.metadata.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ActivityServiceImpl implements ActivityService {

	@Autowired
	ActivityManager activityManager;	
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;
	
	@Override
	public Activity createActivity(String userId, Activity activity)
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return activityManager.createActivity(userInfo, activity);		
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
