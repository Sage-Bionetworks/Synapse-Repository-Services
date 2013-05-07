package org.sagebionetworks.auth.services;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;


/**
 * Abstraction for the handling authentication
 */
public interface AuthenticationService {
	/**
	 * 
	 * @param userId email address of the current user
	 * @param newEmailAddress validated email address that it should be changed to
	 */
	void updateEmail(String oldUserId, String newUserId) throws DatastoreException, NotFoundException, XPathExpressionException, IOException, AuthenticationException;
}
