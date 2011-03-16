package org.sagebionetworks.repo.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;

public class UserSynchronization {
	
	private static final Logger log = Logger.getLogger(BaseController.class
			.getName());

	@Autowired
	private CrowdAuthUtil crowdAuthUtil = null;
	
	private UserDAO userDAO = null;
	
	public UserSynchronization(UserDAO userDAO) {
		this.userDAO=userDAO;
	}
	

	/**
	 	- get users in Crowd
			for all users:  if user isn't in PL, create it
		- get users in PL:
			for all users: if user isn't in Crowd, remove it
			(what about admin or other users)
	*/
	public void synchronizeUsers() throws AuthenticationException, 
									IOException, DatastoreException, InvalidModelException, 
									UnauthorizedException, NotFoundException {


		Collection<String> crowdUserIds = crowdAuthUtil.getUsersInGroup(AuthUtilConstants.PLATFORM_GROUP);
 
		Collection<User> users = userDAO.getInRange(0, Integer.MAX_VALUE);
		Map<String,User> idToUserMap = new HashMap<String,User>();
		for (User user : users) {
			String userId = user.getUserId();
			if (idToUserMap.containsKey(userId)) throw new IllegalStateException("Duplicate userId: "+userId);
			idToUserMap.put(userId, user);
		}
		
		for (String crowdUserId : crowdUserIds) {
			if (!idToUserMap.containsKey(crowdUserId)) {
				// then we need to make the new user in the persistence layer
				User user = new User();
				user.setUserId(crowdUserId);
				user.setCreationDate(new Date());
				userDAO.create(user);
			}
		}
		
		for (String userId : idToUserMap.keySet()) {
			if (!crowdUserIds.contains(userId)) {
				// then we need to remove it from the persistence layer
				userDAO.delete(idToUserMap.get(userId).getId());
			}
		}
	}
}
