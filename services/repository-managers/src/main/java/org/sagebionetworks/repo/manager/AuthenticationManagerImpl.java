package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.DomainType;
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
	public Session authenticate(long principalId, String password, DomainType domain) throws NotFoundException {
		// Check the username password combination
		// This will throw an UnauthorizedException if invalid
		if (password != null) {
			byte[] salt = authDAO.getPasswordSalt(principalId);
			String passHash = PBKDF2Utils.hashPassword(password, salt);
			authDAO.checkUserCredentials(principalId, passHash);
		}
		
		return getSessionToken(principalId, domain);
	}
	
	@Override
	public Long getPrincipalId(String sessionToken) {
		Long principalId = authDAO.getPrincipal(sessionToken);
		if (principalId == null) {
			throw new UnauthorizedException("The session token (" + sessionToken + ") has expired");
		}
		return principalId;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long checkSessionToken(String sessionToken, DomainType domain, boolean checkToU) throws NotFoundException {
		Long principalId = authDAO.getPrincipalIfValid(sessionToken);
		if (principalId == null) {
			// Check to see why the token is invalid
			Long userId = authDAO.getPrincipal(sessionToken);
			if (userId == null) {
				throw new UnauthorizedException("The session token (" + sessionToken + ") is invalid");
			}
			throw new UnauthorizedException("The session token (" + sessionToken + ") has expired");
		}
		// Check the terms of use
		if (checkToU && !authDAO.hasUserAcceptedToU(principalId, domain)) {
			throw new TermsOfUseException();
		}
		
		authDAO.revalidateSessionToken(principalId, domain);
		return principalId;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void invalidateSessionToken(String sessionToken) {
		authDAO.deleteSessionToken(sessionToken);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changePassword(Long principalId, String password) {
		String passHash = PBKDF2Utils.hashPassword(password, null);
		authDAO.changePassword(principalId, passHash);
	}
	
	@Override
	public String getSecretKey(Long principalId) throws NotFoundException {
		return authDAO.getSecretKey(principalId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void changeSecretKey(Long principalId) {
		authDAO.changeSecretKey(principalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Session getSessionToken(long principalId, DomainType domain) throws NotFoundException {
		// Get the session token
		Session session = authDAO.getSessionTokenIfValid(principalId, domain);
		
		// Make the session token if none was returned
		if (session == null) {
			session = new Session();
		}
		
		// Set a new session token if necessary
		if (session.getSessionToken() == null) {
			UserGroup ug = userGroupDAO.get(principalId);
			if (ug == null) {
				throw new NotFoundException("The user (" + principalId + ") does not exist");
			}
			if(!ug.getIsIndividual()) throw new IllegalArgumentException("Cannot get a session token for a team");
			String token = authDAO.changeSessionToken(principalId, null, domain);
			boolean toU = authDAO.hasUserAcceptedToU(principalId, domain);
			session.setSessionToken(token);
			
			// Make sure to fetch the ToU state
			session.setAcceptsTermsOfUse(toU);
		}
		
		return session;
	}

	@Override
	public boolean hasUserAcceptedTermsOfUse(Long id, DomainType domain) throws NotFoundException {
		if (domain == null) {
			throw new IllegalArgumentException("Must provide a domain");
		}
		return authDAO.hasUserAcceptedToU(id, domain);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setTermsOfUseAcceptance(Long principalId, DomainType domain, Boolean acceptance) {
		if (domain == null) {
			throw new IllegalArgumentException("Must provide a domain");
		}
		if (acceptance == null) {
			throw new IllegalArgumentException("Cannot \"unsee\" the terms of use");
		}
		authDAO.setTermsOfUseAcceptance(principalId, domain, acceptance);
	}
}
