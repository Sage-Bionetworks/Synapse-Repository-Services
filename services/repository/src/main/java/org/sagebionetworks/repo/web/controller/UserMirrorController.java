package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.util.UserSynchronization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for mirroring user directory to persistence layer
 * 
 * @author bhoff
 */
@Controller
public class UserMirrorController extends BaseController {


	@Autowired
	private CrowdAuthUtil crowdAuthUtil;
	
	UserMirrorController() {
	}
	
	private UserDAO userDAO = null;

	private void checkAuthorization(String userId, Boolean readOnly) {
		UserDAO dao = getDaoFactory().getUserDAO(userId);
		this.userDAO=dao;
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value=UrlHelpers.USER_MIRROR, method = RequestMethod.POST)
	public void mirrorUsers(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, 
			IOException, AuthenticationException {
		
		// DEBUG
		if (crowdAuthUtil==null) throw new NullPointerException();

		checkAuthorization(userId, false);
		UserSynchronization us = new UserSynchronization(userDAO, crowdAuthUtil);
		us.synchronizeUsers();
	}


}
