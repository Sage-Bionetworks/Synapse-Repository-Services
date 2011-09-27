package org.sagebionetworks.repo.util;

import java.io.IOException;
import java.util.logging.Logger;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("all")
public class UserSynchronization {
	
	private static final Logger log = Logger.getLogger(UserSynchronization.class.getName());

	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
	
	@Autowired
	private UserDAO userDAO = null;
		

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


	}
}
