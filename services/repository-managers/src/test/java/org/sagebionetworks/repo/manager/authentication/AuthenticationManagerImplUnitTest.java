package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserCredentialValidator;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidatorImpl;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithCurrentPassword;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithToken;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithTwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.HasTwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthDisableRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetToken;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthTokenContext;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.TwoFactorAuthRequiredException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class AuthenticationManagerImplUnitTest {

	@InjectMocks
	private AuthenticationManagerImpl authManager;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private AuthenticationReceiptTokenGenerator mockReceiptTokenGenerator;
	@Mock
	private PasswordValidatorImpl mockPassswordValidator;
	@Mock
	private UserCredentialValidator mockUserCredentialValidator;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private PasswordResetTokenGenerator mockPasswordResetTokenGenerator;
	@Mock
	private OIDCTokenHelper mockOIDCTokenHelper;
	@Mock
	private Clock mockClock;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private TwoFactorAuthManager mock2FaManager;
	@Mock
	private FeatureManager mockFeatureManager;
	
	final Long userId = 12345L;
	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseAccessToken = "synapseaccesstoken";
	final String receipt = "receipt";
	final String issuer = "https://repo-prod.sagebase.org/v1";

	final String newChangedPassword = "hunter2";

	LoginRequest loginRequest;

	ChangePasswordWithCurrentPassword changePasswordWithCurrentPassword;
	ChangePasswordWithToken changePasswordWithToken;
	PasswordResetSignedToken passwordResetSignedToken;
	UserInfo userInfo;

	public void setupMockPrincipalAliasDAO() {
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(userId);

		when(mockPrincipalAliasDAO.findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME)).thenReturn(principalAlias);
	}

	public void setupMockUserGroupDAO() {
		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);
	}

	@BeforeEach
	public void setUp() throws Exception {
		loginRequest = new LoginRequest();
		loginRequest.setPassword(password);
		loginRequest.setUsername(username);
		loginRequest.setAuthenticationReceipt(receipt);

		changePasswordWithCurrentPassword = new ChangePasswordWithCurrentPassword();
		changePasswordWithCurrentPassword.setNewPassword(newChangedPassword);
		changePasswordWithCurrentPassword.setUsername(username);
		changePasswordWithCurrentPassword.setCurrentPassword(password);

		passwordResetSignedToken = new PasswordResetSignedToken();
		changePasswordWithToken = new ChangePasswordWithToken();
		changePasswordWithToken.setNewPassword(newChangedPassword);
		changePasswordWithToken.setPasswordChangeToken(passwordResetSignedToken);
		passwordResetSignedToken.setUserId(userId.toString());
		
		userInfo = new UserInfo(false, userId);

	}

	@Test
	public void testUnseeTermsOfUse() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			authManager.setTermsOfUseAcceptance(userId, null);
			}
		);
	}

	@Test
	public void testSetPasswordWithInvalidPassword() {
		String bannedPassword = "password123";
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(bannedPassword);
		
		assertThrows(InvalidPasswordException.class, ()->{
			authManager.setPassword(userId, bannedPassword);
		}).getMessage();
		
		verify(mockPassswordValidator).validatePassword(bannedPassword);
		verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
	}

	@Test
	public void testSetPasswordWithValidPassword() {
		String validPassword = UUID.randomUUID().toString();
		authManager.setPassword(userId, validPassword);
		verify(mockPassswordValidator).validatePassword(validPassword);
		verify(mockAuthDAO).changePassword(anyLong(), anyString());
	}

	////////////////
	// login()
	///////////////

	@Test
	public void testLogin() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		String newReceipt = "newReceipt";
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(userId)).thenReturn(newReceipt);
		when(mockOIDCTokenHelper.createClientTotalAccessToken(userId, issuer)).thenReturn(synapseAccessToken);
		Date now = new Date(12345L);
		when(mockClock.now()).thenReturn(now);

		// call under test
		LoginResponse response = authManager.login(loginRequest, issuer);
		assertNotNull(response);
		assertEquals(newReceipt, response.getAuthenticationReceipt());
		assertEquals(synapseAccessToken, response.getAccessToken());

		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).setAuthenticatedOn(userId, now);
	}
	
	@Test
	public void testLoginWithExpiredPassword() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockAuthDAO.getPasswordExpiresOn(anyLong())).thenReturn(Optional.of(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);

		String result = assertThrows(PasswordResetViaEmailRequiredException.class, () -> {			
			// call under test
			authManager.login(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("Your password has expired, please update your password via email reset.", result);
		
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).getPasswordExpiresOn(userId);
	}
	
	@Test
	public void testLoginWithNotExpiredPassword() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthDAO.getPasswordExpiresOn(anyLong())).thenReturn(Optional.of(Date.from(Instant.now().plus(1, ChronoUnit.DAYS))));
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		when(mockOIDCTokenHelper.createClientTotalAccessToken(userId, issuer)).thenReturn(synapseAccessToken);
		Date now = new Date(12345L);
		when(mockClock.now()).thenReturn(now);
		
		// call under test
		LoginResponse response = authManager.login(loginRequest, issuer);
		
		assertEquals(synapseAccessToken, response.getAccessToken());
		
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).getPasswordExpiresOn(userId);
		verify(mockAuthDAO).setAuthenticatedOn(userId, now);
	}
	
	@Test
	public void testLoginAnd2FaEnabled() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		userInfo.setTwoFactorAuthEnabled(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.generate2FaToken(any(), any())).thenReturn("2faToken");
		
		TwoFactorAuthRequiredException result = assertThrows(TwoFactorAuthRequiredException.class, () -> {			
			// call under test
			authManager.login(loginRequest, issuer);
		});
		
		assertEquals(userId, result.getUserId());
		assertEquals("2faToken", result.getTwoFaToken());
		
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).generate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
	}
	
	@Test
	public void testLoginWithNoPasswordCheckWith2FaDisabled() {
		userInfo.setTwoFactorAuthEnabled(false);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		String newReceipt = "newReceipt";
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(userId)).thenReturn(newReceipt);
		when(mockOIDCTokenHelper.createClientTotalAccessToken(userId, issuer)).thenReturn(synapseAccessToken);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		Date now = new Date(12345);		
		when(mockClock.now()).thenReturn(now);

		LoginResponse expected = new LoginResponse();
		expected.setAcceptsTermsOfUse(true);
		expected.setAccessToken(synapseAccessToken);
		expected.setAuthenticationReceipt(newReceipt);
		
		// call under test
		LoginResponse response = authManager.loginWithNoPasswordCheck(userId, issuer);

		assertEquals(expected, response);

		verify(mockUserManager).getUserInfo(userId);
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).setAuthenticatedOn(userId, now);
	}
	
	@Test
	public void testLoginWithNoPasswordCheckWith2FaEnabled() {
		userInfo.setTwoFactorAuthEnabled(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.generate2FaToken(any(), any())).thenReturn("2faToken");
		
		TwoFactorAuthRequiredException result = assertThrows(TwoFactorAuthRequiredException.class, () -> {			
			// call under test
			authManager.loginWithNoPasswordCheck(userId, issuer);
		});
		
		assertEquals(userId, result.getUserId());
		assertEquals("2faToken", result.getTwoFaToken());

		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).generate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION);
	}
	
	@Test
	public void testAuthenticatedOn() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(userId);
		Date authDate = new Date(123L);
		
		when(mockAuthDAO.getAuthenticatedOn(userId)).thenReturn(authDate);
		
		// method under test
		AuthenticatedOn authenticatedOn = authManager.getAuthenticatedOn(userInfo);
		
		assertEquals(authDate, authenticatedOn.getAuthenticatedOn());
	}

	@Test
	public void testAuthenticatedOnAnonymous() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		// method under test
		assertThrows(UnauthenticatedException.class, ()->{
			authManager.getAuthenticatedOn(userInfo);
		});

	}

	///////////////////////////////////////////////////////////
	// getLoginResponseAfterSuccessfulAuthentication ()
	///////////////////////////////////////////////////////////
	@Test
	public void testGetLoginResponseAfterSuccessfulAuthentication(){
		String newReceipt = "uwu";
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(userId)).thenReturn(newReceipt);
		when(mockOIDCTokenHelper.createClientTotalAccessToken(userId, issuer)).thenReturn(synapseAccessToken);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		Date authTime = new Date(12345L);
		when(mockClock.now()).thenReturn(authTime);
		LoginResponse expected = new LoginResponse();
		expected.setAcceptsTermsOfUse(true);
		expected.setAccessToken(synapseAccessToken);
		expected.setAuthenticationReceipt(newReceipt);

		//method under test
		LoginResponse loginResponse = authManager.getLoginResponseAfterSuccessfulAuthentication(userId, issuer);
		
		assertEquals(loginResponse, loginResponse);
		verifyZeroInteractions(mock2FaManager);
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).setAuthenticatedOn(userId, authTime);
	}

	///////////////////////////////////////////
	// validateAuthReceiptAndCheckPassword()
	////////////////////////////////////////////

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithoutReceipt() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, null);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithInvalidReceipt() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(false);

		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithInvalidReceiptAndWrongPassword() {
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(false);
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(false);


		assertThrows(UnauthenticatedException.class, ()->{
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);
		});

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithValidReceipt() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);

		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithValidReceiptAndWrongPassword() {
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(false);

		assertThrows(UnauthenticatedException.class, ()->{
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);
		});

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithWeakPassword_NotUsersActualPassword(){
		//case where someone tries to brute force a weak password such as "password123", but is not the user's actual password

		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(false);

		assertThrows(UnauthenticatedException.class, ()->{
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, null);
		});

		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockPassswordValidator, never()).validatePassword(password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPasswordWithWeakPassword_PassPasswordCheck(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		//case where someone's actual password is a weak password such as "password123"

		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(password);

		String message = assertThrows(PasswordResetViaEmailRequiredException.class, ()->{
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, null);
		}).getMessage();
		assertEquals("You must change your password via email reset.", message);

		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockPassswordValidator).validatePassword(password);
	}



	////////////////////////////////
	// findUserIdForAuthentication()
	////////////////////////////////
	@Test
	public void testFindUserForAuthenticationWithUserFound(){
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(userId);
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(anyString(), ArgumentMatchers.<AliasType>any())).thenReturn(principalAlias);

		//method under test
		assertEquals(userId, (Long) authManager.findUserIdForAuthentication(username));

		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
	}

	@Test
	public void testFindUserForAuthenticationWithUserNotFound(){
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(anyString(), ArgumentMatchers.<AliasType>any())).thenReturn(null);
		
		String message = assertThrows(UnauthenticatedException.class, ()->{
			authManager.findUserIdForAuthentication(username);
		}).getMessage();
		assertEquals(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT, message);
	}


	/////////////////////////////////////////////////////////////
	// validateChangePassword(ChangePasswordWithCurrentPassword)
	/////////////////////////////////////////////////////////////
	@Test
	public void testValidateChangePasswordWithCurrentPasswordAndNullUsername(){
		changePasswordWithCurrentPassword.setUsername(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		});
	}

	@Test
	public void testValidateChangePasswordWithCurrentPasswordAndNullCurrentPassword(){
		changePasswordWithCurrentPassword.setCurrentPassword(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		});
	}
	
	@Test
	public void testValidateChangePasswordWithCurrentPasswordAndRecentlyModified() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockAuthDAO.getPasswordModifiedOn(anyLong())).thenReturn(Optional.of(Date.from(Instant.now().minus(DBOCredential.MIN_PASSWORD_CHANGE_SECONDS - 1, ChronoUnit.SECONDS))));
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		}).getMessage();
			
		assertEquals("Your password was changed in the past 24 hours, you may update your password via email reset.", result);

		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).getPasswordModifiedOn(userId);
	}
	
	@Test
	public void testValidateChangePasswordWithCurrentPasswordAndPreviouslyModified() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthDAO.getPasswordModifiedOn(anyLong())).thenReturn(Optional.of(Date.from(Instant.now().minus(DBOCredential.MIN_PASSWORD_CHANGE_SECONDS + 1, ChronoUnit.SECONDS))));
		
		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithCurrentPassword);

		//method under test
		assertEquals(userId, validatedUserId);
				
		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).getPasswordModifiedOn(userId);
	}
	
	@Test
	public void testValidateChangePasswordWithCurrentPasswordAndModifiedInTheFuture() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthDAO.getPasswordModifiedOn(anyLong())).thenReturn(Optional.of(Date.from(Instant.now().plus(1, ChronoUnit.SECONDS))));
		
		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithCurrentPassword);

		//method under test
		assertEquals(userId, validatedUserId);
				
		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).getPasswordModifiedOn(userId);
	}

	@Test
	public void testValidateChangePasswordWithCurrentPassword(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);

		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithCurrentPassword);

		//method under test
		assertEquals(userId, validatedUserId);

		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}
	
	@Test
	public void testValidateChangePasswordWithCurrentPasswordAnd2FaEnabled(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		userInfo.setTwoFactorAuthEnabled(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);

		TwoFactorAuthRequiredException ex = assertThrows(TwoFactorAuthRequiredException.class, () -> {			
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		});
		
		assertEquals(userId, ex.getUserId());

		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).generate2FaToken(userInfo, TwoFactorAuthTokenContext.PASSWORD_CHANGE);
	}

	/////////////////////////////////////////////////////////////
	// validateChangePassword(ChangePasswordWithToken)
	/////////////////////////////////////////////////////////////

	@Test
	public void testValidateChangePasswordWithTokenAndInvalidToken(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(false);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.validateChangePassword(changePasswordWithToken);
		});
	}

	@Test
	public void testValidateChangePasswordWithToken(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);

		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithToken);

		assertEquals(validatedUserId, userId);

		verify(mockPasswordResetTokenGenerator).isValidToken(passwordResetSignedToken);
	}

	@Test
	public void testValidateChangePasswordWithTokenAndMissingToken() {
		changePasswordWithToken.setPasswordChangeToken(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.validateChangePassword(changePasswordWithToken);
		});
	}
	
	@Test
	public void testValidateChangePasswordWithTokenAnd2FaEnabled() {
		userInfo.setTwoFactorAuthEnabled(true);
		
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);

		TwoFactorAuthRequiredException ex = assertThrows(TwoFactorAuthRequiredException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithToken);
		});
		
		assertEquals(userInfo.getId(), ex.getUserId());
		
		verify(mockPasswordResetTokenGenerator).isValidToken(passwordResetSignedToken);
		verify(mock2FaManager).generate2FaToken(userInfo, TwoFactorAuthTokenContext.PASSWORD_CHANGE);
	}
	
	@Test
	public void testValidateChangePasswordWithTwoFaToken(){
		AuthenticationManagerImpl authManagerSpy = Mockito.spy(authManager);

		doNothing().when(authManagerSpy).validateTwoFactorAuthTokenRequest(any(), any());
		
		ChangePasswordWithTwoFactorAuthToken changePasswordWithTwoFaToken = new ChangePasswordWithTwoFactorAuthToken()
			.setNewPassword(newChangedPassword)
			.setOtpCode("code")
			.setTwoFaToken("twoFaToken")
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setUserId(userId);
		

		//method under test
		Long validatedUserId = authManagerSpy.validateChangePassword(changePasswordWithTwoFaToken);

		assertEquals(validatedUserId, userId);

		verify(authManagerSpy).validateTwoFactorAuthTokenRequest(changePasswordWithTwoFaToken, TwoFactorAuthTokenContext.PASSWORD_CHANGE);
	}


	////////////////////
	// changePassword()
	////////////////////

	@Test
	public void testChangePasswordWithNullNewPassword(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);

		changePasswordWithCurrentPassword.setNewPassword(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.changePassword(changePasswordWithCurrentPassword);
		});
	}


	@Test
	public void testChangePasswordWithUnknownImplementation(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			//use anonymous implementation
			authManager.changePassword(new ChangePasswordInterface() {
				@Override
				public String getNewPassword() {
					return "";
				}

				@Override
				public ChangePasswordInterface setNewPassword(String newPassword) {
					return this;
				}

				@Override
				public String getConcreteType() {
					return null;
				}

				@Override
				public ChangePasswordInterface setConcreteType(String concreteType) {
					return this;
				}

				@Override
				public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
					return null;
				}

				@Override
				public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
					return null;
				}
			});
		});
	}

	@Test
	public void testChangePasswordWithCurrentPassword(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		
		Long changedPasswordUserId = authManager.changePassword(changePasswordWithCurrentPassword);

		verify(mockPassswordValidator).validatePassword(newChangedPassword);
		assertEquals(userId, changedPasswordUserId);
		verifyZeroInteractions(mockPasswordResetTokenGenerator);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).changePassword(eq(userId), anyString());
		verify(mockUserCredentialValidator).forceResetLoginThrottle(userId);
	}

	@Test
	public void testChangePasswordWithWeakNewPassword(){
		setupMockPrincipalAliasDAO();
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);

		doNothing().when(mockPassswordValidator).validatePassword(password);
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(newChangedPassword);

		assertThrows(InvalidPasswordException.class,()->{
			authManager.changePassword(changePasswordWithCurrentPassword);
		});

		verify(mockPassswordValidator).validatePassword(newChangedPassword);
		verifyZeroInteractions(mockPasswordResetTokenGenerator);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
	}
	
	@Test
	public void testLoginWith2Fa() {
		
		AuthenticationManagerImpl authManagerSpy = Mockito.spy(authManager);
		
		doNothing().when(authManagerSpy).validateTwoFactorAuthTokenRequest(any(), any());
		
		LoginResponse loginResponse = new LoginResponse();
		
		loginResponse.setAcceptsTermsOfUse(true);
		loginResponse.setAccessToken(synapseAccessToken);
		loginResponse.setAuthenticationReceipt("authReceipt");
		
		doReturn(loginResponse).when(authManagerSpy).getLoginResponseAfterSuccessfulAuthentication(anyLong(), any());
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		LoginResponse result = authManagerSpy.loginWith2Fa(loginRequest, issuer);
		
		assertEquals(loginResponse, result);
		
		verify(authManagerSpy).validateTwoFactorAuthTokenRequest(loginRequest, TwoFactorAuthTokenContext.AUTHENTICATION);
		verify(authManagerSpy).getLoginResponseAfterSuccessfulAuthentication(userId, issuer);
	}
	
	@ParameterizedTest
	@EnumSource(TwoFactorAuthTokenContext.class)
	public void testValidateTwoFactorAuthTokenRequestWithTotpCode(TwoFactorAuthTokenContext context) {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(true);
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, context, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@ParameterizedTest
	@EnumSource(TwoFactorAuthTokenContext.class)
	public void testValidateTwoFactorAuthTokenRequestWithRecoveryCode(TwoFactorAuthTokenContext context) {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaRecoveryCode(any(), any())).thenReturn(true);
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.RECOVERY_CODE)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, context, "2faToken");
		verify(mock2FaManager).validate2FaRecoveryCode(userInfo, "123456");
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@ParameterizedTest
	@EnumSource(TwoFactorAuthTokenContext.class)
	public void testValidateTwoFactorAuthTokenRequestWithNoOtpType(TwoFactorAuthTokenContext context) {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(true);

		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(null)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, context, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@ParameterizedTest
	@EnumSource(TwoFactorAuthTokenContext.class)
	public void testValidateTwoFactorAuthTokenRequestWithInvalidToken(TwoFactorAuthTokenContext context) {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(false);
				
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The provided 2fa token is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, context, "2faToken");
		verifyNoMoreInteractions(mockUserManager, mock2FaManager);
	}
	
	@ParameterizedTest
	@EnumSource(TwoFactorAuthTokenContext.class)
	public void testValidateTwoFactorAuthTokenRequestWithInvalidCode(TwoFactorAuthTokenContext context) {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(false);
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The provided code is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, context, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verifyNoMoreInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testValidateTwoFactorAuthTokenRequestWithNullRequest() {
		
		HasTwoFactorAuthToken loginRequest = null;
		TwoFactorAuthTokenContext context = TwoFactorAuthTokenContext.AUTHENTICATION;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testValidateTwoFactorAuthTokenRequestWithNoUserId() {
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(null)
				.setOtpCode("123456")
				.setTwoFaToken("2faToken");
		
		TwoFactorAuthTokenContext context = TwoFactorAuthTokenContext.AUTHENTICATION;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The userId is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testValidateTwoFactorAuthTokenRequestWithNoOtpCode() {
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(userId)
				.setOtpCode(null)
				.setTwoFaToken("2faToken");
		
		TwoFactorAuthTokenContext context = TwoFactorAuthTokenContext.AUTHENTICATION;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The otpCode is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testValidateTwoFactorAuthTokenRequestWithNo2FaToken() {
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(userId)
				.setOtpCode("123456")
				.setTwoFaToken(null);
		
		TwoFactorAuthTokenContext context = TwoFactorAuthTokenContext.AUTHENTICATION;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The twoFaToken is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testValidateTwoFactorAuthTokenRequestWithNoContext() {
		
		HasTwoFactorAuthToken loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(userId)
				.setOtpCode("123456")
				.setTwoFaToken("twoFaToken");
		
		TwoFactorAuthTokenContext context = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.validateTwoFactorAuthTokenRequest(loginRequest, context);
		}).getMessage();
		
		assertEquals("The context is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithTwoFaToken() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setTwoFaToken("twoFaToken")
				.setUserId(userId)
				.setTwoFaResetEndpoint("http://synapse.org");
		
		// Call under test
		authManager.send2FaResetNotification(request);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION, "twoFaToken");
		verify(mock2FaManager).send2FaResetNotification(userInfo, "http://synapse.org");
	}
	
	@Test
	public void testSend2FaResetNotificationWithInvalidToken() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(false);
		
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setTwoFaToken("twoFaToken")
				.setUserId(userId)
				.setTwoFaResetEndpoint("http://synapse.org");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The provided 2fa token is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION, "twoFaToken");
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithPassword() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockUserCredentialValidator.checkPassword(anyLong(), any())).thenReturn(true);
		
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setUserId(userId)
				.setPassword("password")
				.setTwoFaResetEndpoint("http://synapse.org");
		
		// Call under test
		authManager.send2FaResetNotification(request);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserCredentialValidator).checkPassword(userInfo.getId(), "password");
		verify(mock2FaManager).send2FaResetNotification(userInfo, "http://synapse.org");
	}
	
	@Test
	public void testSend2FaResetNotificationWithInvalidPassword() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockUserCredentialValidator.checkPassword(anyLong(), any())).thenReturn(false);
		
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setUserId(userId)
				.setPassword("password")
				.setTwoFaResetEndpoint("http://synapse.org");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The provided password is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserCredentialValidator).checkPassword(userInfo.getId(), "password");
		verifyZeroInteractions(mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithNoRequest() {
		TwoFactorAuthResetRequest request = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithNoUser() {
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setTwoFaToken("twoFaToken")
				.setUserId(null)
				.setTwoFaResetEndpoint("http://synapse.org");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The userId is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithNoTwoFaTokenOrPassword() {
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setTwoFaToken(null)
				.setUserId(userId)
				.setTwoFaResetEndpoint("http://synapse.org");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The twoFaToken or the password are required.", result);
		
		verifyZeroInteractions(mock2FaManager);
	}
	
	@Test
	public void testSend2FaResetNotificationWithNoResetEndpoint() {
		TwoFactorAuthResetRequest request = new TwoFactorAuthResetRequest()
				.setTwoFaToken("twoFaToken")
				.setUserId(userId)
				.setTwoFaResetEndpoint(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.send2FaResetNotification(request);
		}).getMessage();
		
		assertEquals("The twoFaResetEndpoint is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager);
	}
	
	@Test
	public void testDisable2FaWithTokenWithTwoFaToken() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaResetToken(any(), any())).thenReturn(true);
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setTwoFaToken("twoFaToken")
			.setTwoFaResetToken(resetToken);
		
		// Call under test
		authManager.disable2FaWithToken(request);
		
		verify(mock2FaManager).validate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION, "twoFaToken");
		verify(mock2FaManager).validate2FaResetToken(userInfo, resetToken);
		verify(mock2FaManager).disable2Fa(userInfo);
		
	}
		
	@Test
	public void testDisable2FaWithTokenWithInvalidTwoFaToken() {
				
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(false);
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setTwoFaToken("twoFaToken")
			.setTwoFaResetToken(resetToken);
		
		String result = assertThrows(UnauthenticatedException.class, () -> {	
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The provided 2fa token is invalid.", result);
		
		verify(mock2FaManager).validate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION, "twoFaToken");
		verifyNoMoreInteractions(mock2FaManager);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithPassword() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockUserCredentialValidator.checkPassword(anyLong(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaResetToken(any(), any())).thenReturn(true);
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setPassword("password")
			.setTwoFaResetToken(resetToken);
		
		// Call under test
		authManager.disable2FaWithToken(request);
		
		verify(mockUserCredentialValidator).checkPassword(userInfo.getId(), "password");
		verify(mock2FaManager).validate2FaResetToken(userInfo, resetToken);
		verify(mock2FaManager).disable2Fa(userInfo);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithInvalidPassword() {
		
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockUserCredentialValidator.checkPassword(anyLong(), any())).thenReturn(false);
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setPassword("password")
			.setTwoFaResetToken(resetToken);
		
		String result = assertThrows(UnauthenticatedException.class, () -> {
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The provided password is invalid.", result);
		
		verify(mockUserCredentialValidator).checkPassword(userInfo.getId(), "password");
		verifyZeroInteractions(mock2FaManager);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithInvalidResetToken() {
				
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaToken(any(), any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaResetToken(any(), any())).thenReturn(false);
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setTwoFaToken("twoFaToken")
			.setTwoFaResetToken(resetToken);
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The provided 2fa reset token is invalid.", result);
		
		verify(mock2FaManager).validate2FaToken(userInfo, TwoFactorAuthTokenContext.AUTHENTICATION, "twoFaToken");
		verify(mock2FaManager).validate2FaResetToken(userInfo, resetToken);
		verifyNoMoreInteractions(mock2FaManager);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithNoRequest() {
		
		TwoFactorAuthDisableRequest request = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyNoMoreInteractions(mock2FaManager);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithNoTwoFaTokenOrPassword() {
		
		TwoFactorAuthResetToken resetToken = new TwoFactorAuthResetToken()
				.setUserId(userInfo.getId());
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setTwoFaToken(null)
			.setTwoFaResetToken(resetToken);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The twoFaToken or the password are required.", result);
		
		verifyNoMoreInteractions(mock2FaManager);
		
	}
	
	@Test
	public void testDisable2FaWithTokenWithNoResetToken() {
		
		TwoFactorAuthDisableRequest request = new TwoFactorAuthDisableRequest()
			.setTwoFaToken("twoFaToken")
			.setTwoFaResetToken(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.disable2FaWithToken(request);
		}).getMessage();
		
		assertEquals("The twoFaResetToken is required.", result);
		
		verifyNoMoreInteractions(mock2FaManager);
		
	}
	
}
