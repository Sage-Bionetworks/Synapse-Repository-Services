package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.manager.password.PasswordValidator;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.auth.AuthenticationReceiptDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationManagerImpl implements AuthenticationManager {


	public static final Long AUTHENTICATION_RECEIPT_LIMIT = 100L;

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
	private PasswordValidator passwordValidator;

	@Autowired
	private AuthenticationManagerUtil authUtil;
	
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
	public void setPassword(Long principalId, String password) {
		passwordValidator.validatePassword(password);

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

		String validAuthReceipt = null;
		if (authenticationReceipt != null && authReceiptDAO.isValidReceipt(principalId, authenticationReceipt)){
			validAuthReceipt = authenticationReceipt;
		}

		//callers that have previously logged in successfully are able to bypass lockout caused by failed attempts
		boolean correctCredentials = validAuthReceipt != null ? authUtil.checkPassword(principalId, password) : authUtil.checkPasswordWithLock(principalId, password);
		if(!correctCredentials){
			throw new UnauthenticatedException(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT);
		}

		return getLoginResponseAfterSuccessfulAuthentication(principalId, validAuthReceipt);
	}

	@Override
	public LoginResponse loginWithNoPasswordCheck(long principalId){
		return getLoginResponseAfterSuccessfulAuthentication(principalId, null);
	}

	private LoginResponse getLoginResponseAfterSuccessfulAuthentication(long principalId, String validatedAuthenticationReciept){
		String newAuthenticationReceipt = createOrRefreshAuthenticationReceipt(principalId, validatedAuthenticationReciept);
		//generate session tokens for user after successful check
		Session session = getSessionToken(principalId);
		return createLoginResponse(session, newAuthenticationReceipt);
	}

	private String createOrRefreshAuthenticationReceipt(Long principalId, String validatedAuthenticationReciept) {
		String newAuthenticationReceipt = null;
		if(validatedAuthenticationReciept != null) {
			newAuthenticationReceipt = authReceiptDAO.replaceReceipt(principalId, validatedAuthenticationReciept);
		} else {
			if (authReceiptDAO.countReceipts(principalId) < AUTHENTICATION_RECEIPT_LIMIT) {
				newAuthenticationReceipt = authReceiptDAO.createNewReceipt(principalId);
			}
		}
		return newAuthenticationReceipt;
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

}
