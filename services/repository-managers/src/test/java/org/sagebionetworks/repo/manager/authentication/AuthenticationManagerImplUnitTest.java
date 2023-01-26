package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserCredentialValidator;
import org.sagebionetworks.repo.manager.UserManager;
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
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
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
		when(mock2FaManager.get2FaStatus(any())).thenReturn(new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED));
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
	public void testLoginAnd2FaEnabled() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.get2FaStatus(any())).thenReturn(new TwoFactorAuthStatus().setStatus(TwoFactorState.ENABLED));
		when(mock2FaManager.generate2FaLoginToken(any())).thenReturn("2faToken");
		
		TwoFactorAuthRequiredException result = assertThrows(TwoFactorAuthRequiredException.class, () -> {			
			// call under test
			authManager.login(loginRequest, issuer);
		});
		
		assertEquals(userId, result.getUserId());
		assertEquals("2faToken", result.getTwoFaToken());
		
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).get2FaStatus(userInfo);
		verify(mock2FaManager).generate2FaLoginToken(userInfo);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
	}
	
	@Test
	public void testLoginWithNoPasswordCheckWith2FaDisabled() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.get2FaStatus(any())).thenReturn(new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED));
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
		verify(mock2FaManager).get2FaStatus(userInfo);
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).setAuthenticatedOn(userId, now);
	}
	
	@Test
	public void testLoginWithNoPasswordCheckWith2FaEnabled() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.get2FaStatus(any())).thenReturn(new TwoFactorAuthStatus().setStatus(TwoFactorState.ENABLED));
		when(mock2FaManager.generate2FaLoginToken(any())).thenReturn("2faToken");
		
		TwoFactorAuthRequiredException result = assertThrows(TwoFactorAuthRequiredException.class, () -> {			
			// call under test
			authManager.loginWithNoPasswordCheck(userId, issuer);
		});
		
		assertEquals(userId, result.getUserId());
		assertEquals("2faToken", result.getTwoFaToken());

		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).get2FaStatus(userInfo);
		verify(mock2FaManager).generate2FaLoginToken(userInfo);
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
	public void testValidateAuthReceiptAndCheckPassword_WithoutReceipt() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, null);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithInvalidReceipt() {
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(false);

		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithInvalidReceiptAndWrongPassword() {
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
	public void testValidateAuthReceiptAndCheckPassword_WithValidReceipt() {
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);

		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithValidReceiptAndWrongPassword() {
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
	public void testValidateAuthReceiptAndCheckPassword_WeakPassword_NotUsersActualPassword(){
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
	public void testValidateAuthReceiptAndCheckPassword_WeakPassword_PassPasswordCheck(){
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
	public void testFindUserForAuthentication_userFound(){
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(userId);
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(anyString(), ArgumentMatchers.<AliasType>any())).thenReturn(principalAlias);

		//method under test
		assertEquals(userId, (Long) authManager.findUserIdForAuthentication(username));

		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
	}

	@Test
	public void testFindUserForAuthentication_userNotFound(){
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
	public void testValidateChangePassword_withCurrentPassword_nullUsername(){
		changePasswordWithCurrentPassword.setUsername(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		});
	}

	@Test
	public void testValidateChangePassword_withCurrentPassword_nullCurrentPassword(){
		changePasswordWithCurrentPassword.setCurrentPassword(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			authManager.validateChangePassword(changePasswordWithCurrentPassword);
		});
	}

	@Test
	public void testValidateChangePassword_withCurrentPassword_valid(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();

		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithCurrentPassword);

		//method under test
		assertEquals(userId, validatedUserId);

		verify(mockPassswordValidator).validatePassword(password);
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}

	/////////////////////////////////////////////////////////////
	// validateChangePassword(ChangePasswordWithToken)
	/////////////////////////////////////////////////////////////

	@Test
	public void testValidateChangePassword_withToken_invalidToken(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(false);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.validateChangePassword(changePasswordWithToken);
		});
	}

	@Test
	public void testValidateChangePassword_withToken_valid(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(true);

		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithToken);

		assertEquals(validatedUserId, userId);

		verify(mockPasswordResetTokenGenerator).isValidToken(passwordResetSignedToken);
	}

	@Test
	public void testValidateChangePassword_withToken_missingToken() {
		changePasswordWithToken.setPasswordChangeToken(null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.validateChangePassword(changePasswordWithToken);
		});
	}


	////////////////////
	// changePassword()
	////////////////////

	@Test
	public void testChangePassword_NullNewPassword(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();

		changePasswordWithCurrentPassword.setNewPassword(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			authManager.changePassword(changePasswordWithCurrentPassword);
		});
	}


	@Test
	public void testChangePassword_unknownImplementation(){
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
	public void testChangePassword_withCurrentPassword(){
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();

		Long changedPasswordUserId = authManager.changePassword(changePasswordWithCurrentPassword);

		verify(mockPassswordValidator).validatePassword(newChangedPassword);
		assertEquals(userId, changedPasswordUserId);
		verifyZeroInteractions(mockPasswordResetTokenGenerator);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO).changePassword(eq(userId), anyString());
		verify(mockUserCredentialValidator).forceResetLoginThrottle(userId);
	}

	@Test
	public void testChangePassword_weakNewPassword(){
		setupMockPrincipalAliasDAO();
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
	public void testLoginWith2FaWithTotpCode() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaLoginToken(any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(true);
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(anyLong())).thenReturn("authReceipt");
		when(mockOIDCTokenHelper.createClientTotalAccessToken(any(), any())).thenReturn(synapseAccessToken);
		when(mockAuthDAO.hasUserAcceptedToU(anyLong())).thenReturn(true);
		when(mockClock.now()).thenReturn(new Date(12345L));
		
		LoginResponse expected = new LoginResponse();
		
		expected.setAcceptsTermsOfUse(true);
		expected.setAccessToken(synapseAccessToken);
		expected.setAuthenticationReceipt("authReceipt");
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		LoginResponse result = authManager.loginWith2Fa(loginRequest, issuer);
		
		assertEquals(expected, result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaLoginToken(userInfo, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).hasUserAcceptedToU(userId);
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@Test
	public void testLoginWith2FaWithRecoveryCode() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaLoginToken(any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaRecoveryCode(any(), any())).thenReturn(true);
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(anyLong())).thenReturn("authReceipt");
		when(mockOIDCTokenHelper.createClientTotalAccessToken(any(), any())).thenReturn(synapseAccessToken);
		when(mockAuthDAO.hasUserAcceptedToU(anyLong())).thenReturn(true);
		when(mockClock.now()).thenReturn(new Date(12345L));
		
		LoginResponse expected = new LoginResponse();
		
		expected.setAcceptsTermsOfUse(true);
		expected.setAccessToken(synapseAccessToken);
		expected.setAuthenticationReceipt("authReceipt");
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.RECOVERY_CODE)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		LoginResponse result = authManager.loginWith2Fa(loginRequest, issuer);
		
		assertEquals(expected, result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaLoginToken(userInfo, "2faToken");
		verify(mock2FaManager).validate2FaRecoveryCode(userInfo, "123456");
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).hasUserAcceptedToU(userId);
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@Test
	public void testLoginWith2FaWithNoOtpType() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaLoginToken(any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(true);
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(anyLong())).thenReturn("authReceipt");
		when(mockOIDCTokenHelper.createClientTotalAccessToken(any(), any())).thenReturn(synapseAccessToken);
		when(mockAuthDAO.hasUserAcceptedToU(anyLong())).thenReturn(true);
		when(mockClock.now()).thenReturn(new Date(12345L));
		
		LoginResponse expected = new LoginResponse();
		
		expected.setAcceptsTermsOfUse(true);
		expected.setAccessToken(synapseAccessToken);
		expected.setAuthenticationReceipt("authReceipt");
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(null)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		// Call under test
		LoginResponse result = authManager.loginWith2Fa(loginRequest, issuer);
		
		assertEquals(expected, result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaLoginToken(userInfo, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockOIDCTokenHelper).createClientTotalAccessToken(userId, issuer);
		verify(mockAuthDAO).hasUserAcceptedToU(userId);
		verifyNoMoreInteractions(mock2FaManager);
	}
	
	@Test
	public void testLoginWith2FaWithInvalidToken() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaLoginToken(any(), any())).thenReturn(false);
				
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The provided 2fa token is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaLoginToken(userInfo, "2faToken");
		verifyNoMoreInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
	
	@Test
	public void testLoginWith2FaWithInvalidCode() {
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mock2FaManager.validate2FaLoginToken(any(), any())).thenReturn(true);
		when(mock2FaManager.validate2FaTotpCode(any(), any())).thenReturn(false);
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(userId)
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode("123456")
			.setTwoFaToken("2faToken");
		
		String result = assertThrows(UnauthenticatedException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The provided code is invalid.", result);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mock2FaManager).validate2FaLoginToken(userInfo, "2faToken");
		verify(mock2FaManager).validate2FaTotpCode(userInfo, "123456");
		verifyNoMoreInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
	
	@Test
	public void testLoginWith2FaWithNullRequest() {
		
		TwoFactorAuthLoginRequest loginRequest = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The loginRequest is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
	
	@Test
	public void testLoginWith2FaWithNoUserId() {
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(null)
				.setOtpCode("123456")
				.setTwoFaToken("2faToken");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The userId is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
	
	@Test
	public void testLoginWith2FaWithNoOtpCode() {
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(userId)
				.setOtpCode(null)
				.setTwoFaToken("2faToken");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The otpCode is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
	
	@Test
	public void testLoginWith2FaWithNo2FaToken() {
		
		TwoFactorAuthLoginRequest loginRequest = new TwoFactorAuthLoginRequest()
				.setUserId(userId)
				.setOtpCode("123456")
				.setTwoFaToken(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			authManager.loginWith2Fa(loginRequest, issuer);
		}).getMessage();
		
		assertEquals("The twoFaToken is required.", result);
		
		verifyZeroInteractions(mockUserManager, mock2FaManager, mockReceiptTokenGenerator, mockOIDCTokenHelper, mockAuthDAO);
	}
}
