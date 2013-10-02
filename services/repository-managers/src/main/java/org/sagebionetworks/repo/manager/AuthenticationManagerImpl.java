package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Very simple implementation of an authentication manager
 * Merely passes each call to the appropriate method of the AuthenticationDAO
 */
public class AuthenticationManagerImpl implements AuthenticationManager {

	@Autowired
	AuthenticationDAO authDAO;
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(Credential credential, boolean validatePassword) 
			throws NotFoundException {
		if (validatePassword) {
			return authDAO.authenticate(credential);
		}
		return authDAO.getSessionToken(credential.getEmail());
	}
	
	@Override
	public Long checkSessionToken(String sessionToken) {
		return authDAO.getPrincipal(sessionToken);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void invalidateSessionToken(String sessionToken) {
		authDAO.deleteSessionToken(sessionToken);
	}
	
	@Override
	public void changePassword(String id, String passHash) {
		authDAO.changePassword(id, passHash);
	}
	
	@Override
	public String getSecretKey(String id) {
		return authDAO.getSecretKey(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changeSecretKey(String id) {
		authDAO.changeSecretKey(id);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session getSessionToken(String username) {
		return authDAO.getSessionToken(username);
	}
	
}
