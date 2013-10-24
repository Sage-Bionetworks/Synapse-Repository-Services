package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AuthenticationManagerImpl implements AuthenticationManager {

	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	public AuthenticationManagerImpl() { }
	
	/**
	 * For unit testing
	 */
	public AuthenticationManagerImpl(AuthenticationDAO authDAO, UserGroupDAO userGroupDAO) {
		this.authDAO = authDAO;
		this.userGroupDAO = userGroupDAO;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session authenticate(String email, String password) throws NotFoundException {
		// Check the username password combination
		// This will throw an UnauthorizedException if invalid
		if (password != null) {
			byte[] salt = authDAO.getPasswordSalt(email);
			String passHash = PBKDF2Utils.hashPassword(password, salt);
			authDAO.checkEmailAndPassword(email, passHash);
		}
		
		return getSessionToken(email);
	}
	
	@Override
	public Long checkSessionToken(String sessionToken) throws UnauthorizedException, TermsOfUseException {
		Long principalId = authDAO.getPrincipalIfValid(sessionToken);
		if (principalId == null) {
			// Check to see why the token is invalid
			Long userId = authDAO.getPrincipal(sessionToken);
			if (userId == null) {
				throw new UnauthorizedException("The session token (" + sessionToken + ") is invalid");
			}
			if (!authDAO.hasUserAcceptedToU(userId.toString())) {
				throw new TermsOfUseException();
			}
			throw new UnauthorizedException("The session token (" + sessionToken + ") has expired");
		}
		return principalId;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void invalidateSessionToken(String sessionToken) {
		authDAO.deleteSessionToken(sessionToken);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(String id, String password) {
		String passHash = PBKDF2Utils.hashPassword(password, null);
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
	public Session getSessionToken(String username) throws NotFoundException {
		// Get the session token
		Session session = authDAO.getSessionTokenIfValid(username);
		
		// Make the session token if none was returned
		if (session == null) {
			session = new Session();
		}
		if (session.getSessionToken() == null) {
			UserGroup ug = userGroupDAO.findGroup(username, true);
			if (ug == null) {
				throw new NotFoundException("The user (" + username + ") does not exist");
			}
			String principalId = ug.getId();
			String token = authDAO.changeSessionToken(principalId, null);
			session.setSessionToken(token);
		}
		
		return session;
	}

	@Override
	public boolean hasUserAcceptedTermsOfUse(String id) {
		return authDAO.hasUserAcceptedToU(id);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setTermsOfUseAcceptance(String id, Boolean acceptance) {
		if (acceptance == null) {
			throw new IllegalArgumentException("Cannot \"unsee\" the terms of use");
		}
		authDAO.setTermsOfUseAcceptance(id, acceptance);
	}
	
}
