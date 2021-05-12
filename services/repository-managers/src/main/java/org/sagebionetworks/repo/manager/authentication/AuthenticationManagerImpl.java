package org.sagebionetworks.repo.manager.authentication;

import java.util.Date;

import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserCredentialValidator;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidator;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithCurrentPassword;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithToken;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationManagerImpl implements AuthenticationManager {

	public static final long LOCK_TIMOUTE_SEC = 5*60;

	public static final int MAX_CONCURRENT_LOCKS = 10;

	public static final String ACCOUNT_LOCKED_MESSAGE = "This account has been locked. Reason: too many requests. Please try again in five minutes.";



	@Autowired
	private AuthenticationDAO authDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private AuthenticationReceiptTokenGenerator authenticationReceiptTokenGenerator;

	@Autowired
	private PasswordValidator passwordValidator;

	@Autowired
	private UserCredentialValidator userCredentialValidator;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private PasswordResetTokenGenerator passwordResetTokenGenerator;
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
	@Autowired
	private Clock clock;

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

	@WriteTransaction
	public long changePassword(ChangePasswordInterface changePasswordInterface){
		ValidateArgument.required(changePasswordInterface, "changePasswordInterface");

		final long userId;
		if(changePasswordInterface instanceof ChangePasswordWithCurrentPassword){
			userId = validateChangePassword((ChangePasswordWithCurrentPassword) changePasswordInterface);
		}else if (changePasswordInterface instanceof ChangePasswordWithToken){
			userId = validateChangePassword((ChangePasswordWithToken) changePasswordInterface);
		}else{
			throw new IllegalArgumentException("Unknown implementation of ChangePasswordInterface");
		}

		//change password and invalidate previous session token
		setPassword(userId, changePasswordInterface.getNewPassword());
		authDAO.deleteSessionToken(userId);
		userCredentialValidator.forceResetLoginThrottle(userId);
		return userId;
	}

	/**
	 *
	 * @param changePasswordWithCurrentPassword
	 * @return id of user for which password change occurred
	 */
	long validateChangePassword(ChangePasswordWithCurrentPassword changePasswordWithCurrentPassword) {
		ValidateArgument.required(changePasswordWithCurrentPassword.getUsername(), "changePasswordWithCurrentPassword.userName");
		ValidateArgument.required(changePasswordWithCurrentPassword.getCurrentPassword(), "changePasswordWithCurrentPassword.currentPassword");

		final long userId = findUserIdForAuthentication(changePasswordWithCurrentPassword.getUsername());
		//we can ignore the return value here because we are not generating a new authentication receipt on success
		validateAuthReceiptAndCheckPassword(userId, changePasswordWithCurrentPassword.getCurrentPassword(), changePasswordWithCurrentPassword.getAuthenticationReceipt());

		return userId;
	}

	/**
	 *
	 * @param changePasswordWithToken
	 * @return id of user for which password change occurred
	 */
	long validateChangePassword(ChangePasswordWithToken changePasswordWithToken){
		ValidateArgument.required(changePasswordWithToken.getPasswordChangeToken(), "changePasswordWithToken.passwordChangeToken");

		if(!passwordResetTokenGenerator.isValidToken(changePasswordWithToken.getPasswordChangeToken())){
			throw new UnauthenticatedException("Password reset token is invalid");
		}

		return Long.parseLong(changePasswordWithToken.getPasswordChangeToken().getUserId());
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
	public PasswordResetSignedToken createPasswordResetToken(long userId) throws NotFoundException {
		return passwordResetTokenGenerator.getToken(userId);
	}

	@Override
	public boolean hasUserAcceptedTermsOfUse(Long id) throws NotFoundException {
		return authDAO.hasUserAcceptedToU(id);
	}
	
	@Override
	@WriteTransaction
	public void setTermsOfUseAcceptance(Long principalId, Boolean acceptance) {
		if (acceptance == null) {
			throw new IllegalArgumentException("Cannot \"unsign\" the terms of use");
		}
		authDAO.setTermsOfUseAcceptance(principalId, acceptance);
	}

	@Deprecated
	@Override
	public LoginResponse loginForSession(LoginRequest request){
		ValidateArgument.required(request, "loginRequest");
		ValidateArgument.required(request.getUsername(), "LoginRequest.username");
		ValidateArgument.required(request.getPassword(), "LoginRequest.password");

		final long userId = findUserIdForAuthentication(request.getUsername());
		final String password = request.getPassword();
		final String authenticationReceipt = request.getAuthenticationReceipt();

		validateAuthReceiptAndCheckPassword(userId, password, authenticationReceipt);

		return getLoginResponseWithSessionAfterSuccessfulPasswordAuthentication(userId);
	}

	@Override
	public LoginResponse login(LoginRequest request, String tokenIssuer){
		ValidateArgument.required(request, "loginRequest");
		ValidateArgument.required(request.getUsername(), "LoginRequest.username");
		ValidateArgument.required(request.getPassword(), "LoginRequest.password");

		final long userId = findUserIdForAuthentication(request.getUsername());
		final String password = request.getPassword();
		final String authenticationReceipt = request.getAuthenticationReceipt();

		validateAuthReceiptAndCheckPassword(userId, password, authenticationReceipt);

		return getLoginResponseAfterSuccessfulPasswordAuthentication(userId, tokenIssuer);
	}
	
	public AuthenticatedOn getAuthenticatedOn(UserInfo userInfo) {
		// Note the date will be null if the user has not logged in
		Date authenticatedOn = authDAO.getAuthenticatedOn(userInfo.getId());
		AuthenticatedOn result = new AuthenticatedOn();
		result.setAuthenticatedOn(authenticatedOn);
		return result;
	}

	/**
	 * Validate authenticationReceipt and then checks that the password is correct for the given principalId
	 * @param userId id of the user
	 * @param password password of the user
	 * @param authenticationReceipt Can be null. When valid, does not throttle attempts on consecutive incorrect passwords.
	 * @return authenticationReceipt if it is valid and password check passed. null, if the authenticationReceipt was invalid, but password check passed.
	 * @throws UnauthenticatedException if password check failed
	 */
	void validateAuthReceiptAndCheckPassword(final long userId, final String password, final String authenticationReceipt) {
		
		boolean isAuthenticationReceiptValid = authenticationReceiptTokenGenerator.isReceiptValid(userId, authenticationReceipt);
		//callers that have previously logged in successfully are able to bypass lockout caused by failed attempts
		boolean correctCredentials = isAuthenticationReceiptValid ? userCredentialValidator.checkPassword(userId, password) : userCredentialValidator.checkPasswordWithThrottling(userId, password);
		if(!correctCredentials){
			throw new UnauthenticatedException(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT);
		}
		// Now that the password has been verified,
		// ensure that if the current password is a weak password, only allow the user to reset via emailed token
		try{
			passwordValidator.validatePassword(password);
		} catch (InvalidPasswordException e){
			throw new PasswordResetViaEmailRequiredException("You must change your password via email reset.");
		}
	}

	@Deprecated
	@Override
	public LoginResponse loginForSessionWithNoPasswordCheck(long principalId){
		return getLoginResponseWithSessionAfterSuccessfulPasswordAuthentication(principalId);
	}

	@Override
	public LoginResponse loginWithNoPasswordCheck(long principalId, String issuer){
		return getLoginResponseAfterSuccessfulPasswordAuthentication(principalId, issuer);
	}

	@Deprecated
	LoginResponse getLoginResponseWithSessionAfterSuccessfulPasswordAuthentication(long principalId){
		String newAuthenticationReceipt = authenticationReceiptTokenGenerator.createNewAuthenticationReciept(principalId);
		//generate session tokens for user after successful check
		Session session = getSessionToken(principalId);
		return createLoginResponse(session, newAuthenticationReceipt);
	}

	LoginResponse getLoginResponseAfterSuccessfulPasswordAuthentication(long principalId, String issuer) {
		String newAuthenticationReceipt = authenticationReceiptTokenGenerator.createNewAuthenticationReciept(principalId);
		String accessToken = oidcTokenHelper.createClientTotalAccessToken(principalId, issuer);
		boolean acceptsTermsOfUse = authDAO.hasUserAcceptedToU(principalId);
		authDAO.setAuthenticatedOn(principalId, clock.now());
		return createLoginResponse(accessToken, acceptsTermsOfUse, newAuthenticationReceipt);
	}
	
	@Deprecated
	/**
	 * Create a login response from the session and the new authentication receipt
	 * 
	 * @param session
	 * @param newReceipt
	 * @return
	 */
	private static LoginResponse createLoginResponse(Session session, String newReceipt) {
		LoginResponse response = new LoginResponse();
		response.setSessionToken(session.getSessionToken());
		response.setAcceptsTermsOfUse(session.getAcceptsTermsOfUse());
		response.setAuthenticationReceipt(newReceipt);
		return response;
	}

	private static LoginResponse createLoginResponse(String accessToken, boolean acceptsTermsOfUse, String newReceipt) {
		LoginResponse response = new LoginResponse();
		response.setAccessToken(accessToken);
		response.setAcceptsTermsOfUse(acceptsTermsOfUse);
		response.setAuthenticationReceipt(newReceipt);
		return response;
	}

	long findUserIdForAuthentication(final String usernameOrEmail){
		PrincipalAlias principalAlias = principalAliasDAO.findPrincipalWithAlias(usernameOrEmail, AliasType.USER_EMAIL, AliasType.USER_NAME);
		if (principalAlias == null){
			throw new UnauthenticatedException(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT);
		}
		return principalAlias.getPrincipalId();
	}

}
