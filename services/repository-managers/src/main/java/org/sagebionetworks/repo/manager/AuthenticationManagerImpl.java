package org.sagebionetworks.repo.manager;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.sagebionetworks.repo.model.AuthenticationDAO;
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
			String passHash;
			try {
				passHash = PBKDF2Utils.hashPassword(password, salt);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			} catch (InvalidKeySpecException e) {
				throw new RuntimeException(e);
			}
			authDAO.checkEmailAndPassword(email, passHash);
		}
		
		return getSessionToken(email);
	}
	
	@Override
	public Long checkSessionToken(String sessionToken) throws UnauthorizedException {
		Long principalId = authDAO.getPrincipalIfValid(sessionToken);
		if (principalId == null) {
			throw new UnauthorizedException("The session token (" + sessionToken + ") is invalid");
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
	
}
