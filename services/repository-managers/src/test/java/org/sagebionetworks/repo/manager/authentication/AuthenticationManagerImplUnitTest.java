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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidatorImpl;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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

	final Long userId = 12345L;
	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseSessionToken = "synapsesessiontoken";
	final String receipt = "receipt";

	final String newChangedPassword = "hunter2";

	LoginRequest loginRequest;

	ChangePasswordWithCurrentPassword changePasswordWithCurrentPassword;
	ChangePasswordWithToken changePasswordWithToken;
	PasswordResetSignedToken passwordResetSignedToken;

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

	}

	@Test
	public void testGetSessionToken() throws Exception {
		setupMockUserGroupDAO();
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		Session session = authManager.getSessionToken(userId);
		assertEquals(synapseSessionToken, session.getSessionToken());

		verify(mockAuthDAO, times(1)).getSessionTokenIfValid(eq(userId));
		verify(mockAuthDAO, times(1)).changeSessionToken(eq(userId), eq((String) null));
	}

	@Test
	public void testCheckSessionToken() throws Exception {
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		Long principalId = authManager.checkSessionToken(synapseSessionToken, true);
		assertEquals(userId, principalId);

		// Token matches, but terms haven't been signed
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(false);
		assertThrows(TermsOfUseException.class, ()->{
			authManager.checkSessionToken(synapseSessionToken, true);
		});

		// Nothing matches the token
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(null);
		
		String message = assertThrows(UnauthenticatedException.class, ()->{
			authManager.checkSessionToken(synapseSessionToken, true);
		}).getMessage();
		assertEquals("The session token (synapsesessiontoken) is invalid", message);

		// Token matches, but has expired
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		
		message = assertThrows(UnauthenticatedException.class, ()->{
			authManager.checkSessionToken(synapseSessionToken, true);
		}).getMessage();
		assertEquals("The session token (synapsesessiontoken) has expired", message);
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
	public void testLogin(){
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);
		setupMockPrincipalAliasDAO();
		setupMockUserGroupDAO();
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);
		when(mockReceiptTokenGenerator.isReceiptValid(userId, receipt)).thenReturn(true);
		String newReceipt = "newReceipt";
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(userId)).thenReturn(newReceipt);

		// call under test
		LoginResponse response = authManager.login(loginRequest);
		assertNotNull(response);
		assertEquals(newReceipt, response.getAuthenticationReceipt());
		assertEquals(synapseSessionToken, response.getSessionToken());


		verify(mockReceiptTokenGenerator).isReceiptValid(userId, receipt);
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
	}

	///////////////////////////////////////////////////////////
	// getLoginResponseAfterSuccessfulPasswordAuthentication ()
	///////////////////////////////////////////////////////////
	@Test
	public void testGetLoginResponseAfterSuccessfulAuthentication_validReciept(){
		setupMockUserGroupDAO();
		String newReceipt = "uwu";
		when(mockReceiptTokenGenerator.createNewAuthenticationReciept(userId)).thenReturn(newReceipt);
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		//method under test
		LoginResponse loginResponse = authManager.getLoginResponseAfterSuccessfulPasswordAuthentication(userId);

		assertEquals(newReceipt, loginResponse.getAuthenticationReceipt());
		assertEquals(synapseSessionToken, loginResponse.getSessionToken());
		verify(mockReceiptTokenGenerator).createNewAuthenticationReciept(userId);
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
		Assertions.assertThrows(UnauthenticatedException.class, () -> {
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
		verify(mockAuthDAO).deleteSessionToken(userId);
		verify(mockAuthDAO).changePassword(eq(userId), anyString());
		verify(mockUserCredentialValidator).forceResetLoginThrottle(userId);
	}

	@Test
	public void testChangePassword_withToken(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(true);

		Long changedPasswordUserId = authManager.changePassword(changePasswordWithToken);

		verify(mockPassswordValidator).validatePassword(newChangedPassword);
		assertEquals(userId, changedPasswordUserId);
		verify(mockPasswordResetTokenGenerator).isValidToken(passwordResetSignedToken);
		verify(mockAuthDAO).deleteSessionToken(userId);
		verify(mockAuthDAO).changePassword(eq(userId), anyString());
		verify(mockUserCredentialValidator).forceResetLoginThrottle(userId);
		verifyNoMoreInteractions(mockUserCredentialValidator);
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
		verify(mockAuthDAO, never()).deleteSessionToken(userId);
		verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
	}
}
