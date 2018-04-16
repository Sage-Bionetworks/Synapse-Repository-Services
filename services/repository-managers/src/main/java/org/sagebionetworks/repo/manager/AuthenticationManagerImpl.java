package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.LockedException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.auth.AuthenticationReceiptDAO;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationManagerImpl implements AuthenticationManager {
	public static final String LOGIN_FAIL_ATTEMPT_METRIC_UNIT = "Count";

	public static final double LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE = 1.0;

	public static final String LOGIN_FAIL_ATTEMPT_METRIC_NAME = "LoginFailAttemptExceedLimit";

	public static final Long AUTHENTICATION_RECEIPT_LIMIT = 100L;

	public static final int PASSWORD_MIN_LENGTH = 8;

	public static final long LOCK_TIMOUTE_SEC = 5*60;

	public static final int MAX_CONCURRENT_LOCKS = 10;

	public static final String ACCOUNT_LOCKED_MESSAGE = "This account has been locked. Reason: too many requests. Please try again in five minutes.";

	@Autowired
	private AuthenticationDAO authDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private AuthenticationReceiptDAO authReceiptDAO;
	@Autowired
	private MemoryCountingSemaphore authenticationThrottleMemoryCountingSemaphore;
	@Autowired
	private Consumer consumer;

	private void logAttemptAfterAccountIsLocked(long principalId) {
		ProfileData loginFailAttemptExceedLimit = new ProfileData();
		loginFailAttemptExceedLimit.setNamespace(this.getClass().getName());
		loginFailAttemptExceedLimit.setName(LOGIN_FAIL_ATTEMPT_METRIC_NAME);
		loginFailAttemptExceedLimit.setValue(LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE);
		loginFailAttemptExceedLimit.setUnit(LOGIN_FAIL_ATTEMPT_METRIC_UNIT);
		loginFailAttemptExceedLimit.setTimestamp(new Date());
		loginFailAttemptExceedLimit.setDimension(Collections.singletonMap("UserId", ""+principalId));
		consumer.addProfileData(loginFailAttemptExceedLimit);
	}
	
	@Override
	public Long getPrincipalId(String sessionToken) {
		Long principalId = authDAO.getPrincipal(sessionToken);
		if (principalId == null) {
			throw new UnauthenticatedException("The session token (" + sessionToken + ") has expired");
		}
		return principalId;
	}
	
	@Override
	@WriteTransaction
	public Long checkSessionToken(String sessionToken, boolean checkToU) throws NotFoundException {
		Long principalId = authDAO.getPrincipalIfValid(sessionToken);
		if (principalId == null) {
			// Check to see why the token is invalid
			Long userId = authDAO.getPrincipal(sessionToken);
			if (userId == null) {
				throw new UnauthenticatedException("The session token (" + sessionToken + ") is invalid");
			}
			throw new UnauthenticatedException("The session token (" + sessionToken + ") has expired");
		}
		// Check the terms of use
		if (checkToU && !authDAO.hasUserAcceptedToU(principalId)) {
			throw new TermsOfUseException();
		}
		authDAO.revalidateSessionTokenIfNeeded(principalId);
		return principalId;
	}

	@Override
	@WriteTransaction
	public void invalidateSessionToken(String sessionToken) {
		authDAO.deleteSessionToken(sessionToken);
	}
	
	@Override
	@WriteTransaction
	public void changePassword(Long principalId, String password) {
		ValidateArgument.requirement(password.length() >= PASSWORD_MIN_LENGTH, "Password must contain "+PASSWORD_MIN_LENGTH+" or more characters .");
		String passHash = PBKDF2Utils.hashPassword(password, null);
		authDAO.changePassword(principalId, passHash);
	}
	
	@Override
	public String getSecretKey(Long principalId) throws NotFoundException {
		return authDAO.getSecretKey(principalId);
	}

	@Override
	@WriteTransaction
	public void changeSecretKey(Long principalId) {
		authDAO.changeSecretKey(principalId);
	}
	
	@Override
	@WriteTransaction
	public Session getSessionToken(long principalId) throws NotFoundException {
		// Get the session token
		Session session = authDAO.getSessionTokenIfValid(principalId);
		
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
			String token = authDAO.changeSessionToken(principalId, null);
			boolean toU = authDAO.hasUserAcceptedToU(principalId);
			session.setSessionToken(token);
			
			// Make sure to fetch the ToU state
			session.setAcceptsTermsOfUse(toU);
		}
		
		return session;
	}

	@Override
	public boolean hasUserAcceptedTermsOfUse(Long id) throws NotFoundException {
		return authDAO.hasUserAcceptedToU(id);
	}
	
	@Override
	@WriteTransaction
	public void setTermsOfUseAcceptance(Long principalId, Boolean acceptance) {
		if (acceptance == null) {
			throw new IllegalArgumentException("Cannot \"unsee\" the terms of use");
		}
		authDAO.setTermsOfUseAcceptance(principalId, acceptance);
	}

	@WriteTransactionReadCommitted
	@Override
	public LoginResponse login(Long principalId, String password, String authenticationReceipt) {
		authReceiptDAO.deleteExpiredReceipts(principalId, System.currentTimeMillis());

		Boolean hasValidReceipt = false;
		String lockToken = null;
		if (authenticationReceipt == null || !authReceiptDAO.isValidReceipt(principalId, authenticationReceipt)) {
			lockToken = authenticationThrottleMemoryCountingSemaphore.attemptToAcquireLock(""+principalId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
			if (lockToken == null) {
				logAttemptAfterAccountIsLocked(principalId);
				throw new LockedException(ACCOUNT_LOCKED_MESSAGE);
			}
		} else {
			hasValidReceipt = true;
		}

		authenticateAndThrowException(principalId, password);
		if (lockToken != null) {
			authenticationThrottleMemoryCountingSemaphore.releaseLock(""+principalId, lockToken);
		}

		Session session = getSessionToken(principalId);

		String newReceipt = null;
		if (authReceiptDAO.countReceipts(principalId) < AUTHENTICATION_RECEIPT_LIMIT) {
			if (hasValidReceipt) {
				newReceipt = authReceiptDAO.replaceReceipt(principalId, authenticationReceipt);
			} else {
				newReceipt = authReceiptDAO.createNewReceipt(principalId);
			}
		}

		return createLoginResponse(session, newReceipt);
	}

	/**
	 * Create a login response from the session and the new authentication receipt
	 * 
	 * @param session
	 * @param newReceipt
	 * @return
	 */
	private LoginResponse createLoginResponse(Session session, String newReceipt) {
		LoginResponse response = new LoginResponse();
		response.setSessionToken(session.getSessionToken());
		response.setAcceptsTermsOfUse(session.getAcceptsTermsOfUse());
		response.setAuthenticationReceipt(newReceipt);
		return response;
	}

	/**
	 * Check username, password combination
	 * 
	 * @param principalId
	 * @param password
	 */
	private void authenticateAndThrowException(Long principalId, String password) {
		byte[] salt = authDAO.getPasswordSalt(principalId);
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		authDAO.checkUserCredentials(principalId, passHash);
	}
}
