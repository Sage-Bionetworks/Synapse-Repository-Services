package org.sagebionetworks.auth.services;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the authentication services
 * @author John
 *
 */
public class AuthenticationServiceImpl implements AuthenticationService {
	
	
	@Autowired
	UserManager userManager;
	
	@Override
	public void updateEmail(String oldUserId, String newUserId) throws DatastoreException, NotFoundException, XPathExpressionException, IOException, AuthenticationException {
		UserInfo userInfo = userManager.getUserInfo(oldUserId);
		userManager.updateEmail(userInfo, newUserId);
	}
	
	/**
	 * Used for testing
	 * @param userManager
	 */
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}
}
