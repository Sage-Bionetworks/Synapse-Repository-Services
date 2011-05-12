package org.sagebionetworks.repo.web.controller;

import org.springframework.stereotype.Controller;

/**
 * 
 * This class is slated for deletion
 * 
 * REST controller for mirroring user directory to persistence layer
 * 
 * @author bhoff
 */
@Controller
public class UserMirrorController extends BaseController {

	UserMirrorController() {
	}
	
//	private UserDAO userDAO = null;
//
//	private void checkAuthorization(String userId, Boolean readOnly) {
//		UserDAO dao = getDaoFactory().getUserDAO(userId);
//		this.userDAO=dao;
//	}

//	@ResponseStatus(HttpStatus.CREATED)
//	@RequestMapping(value=UrlHelpers.USER_MIRROR, method = RequestMethod.POST)
//	public void mirrorUsers(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			HttpServletRequest request)
//			throws DatastoreException, InvalidModelException,
//			UnauthorizedException, NotFoundException, 
//			IOException, AuthenticationException {
//		
//		checkAuthorization(userId, false);
//		UserSynchronization us = new UserSynchronization(userDAO);
//		us.synchronizeUsers();
//	}


}
