package org.sagebionetworks.repo.manager.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.authentication.AuthenticationManagerImpl.AUTHENTICATION_RECEIPT_LIMIT;

import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserCredentialValidator;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidatorImpl;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithCurrentPassword;
import org.sagebionetworks.repo.model.auth.ChangePasswordWithToken;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.AuthenticationReceiptDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationManagerImplUnitTest {

	@InjectMocks
	private AuthenticationManagerImpl authManager;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private AuthenticationReceiptDAO mockAuthReceiptDAO;
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


	@Before
	public void setUp() throws Exception {
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(true);
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(true);


		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(userId);

		when(mockPrincipalAliasDAO.findPrincipalWithAlias(username, AliasType.USER_EMAIL, AliasType.USER_NAME)).thenReturn(principalAlias);
		when(mockAuthReceiptDAO.createNewReceipt(userId)).thenReturn(receipt);

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
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(true);

		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
	}

	@Test
	public void testGetSessionToken() throws Exception {
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
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (TermsOfUseException e) {
		}

		// Nothing matches the token
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(null);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("invalid"));
		}

		// Token matches, but has expired
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("expired"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnseeTermsOfUse() throws Exception {
		authManager.setTermsOfUseAcceptance(userId, null);
	}

	@Test
	public void testSetPasswordWithInvalidPassword() {
		String bannedPassword = "password123";
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(bannedPassword);
		try {
			authManager.setPassword(userId, bannedPassword);
			fail();
		} catch (InvalidPasswordException e) {
			//expected
		}

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
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);


		authManager.login(loginRequest);

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockAuthReceiptDAO).replaceReceipt(userId, receipt);
	}

	///////////////////////////////////////////////////////////
	// getLoginResponseAfterSuccessfulPasswordAuthentication ()
	///////////////////////////////////////////////////////////
	@Test
	public void testGetLoginResponseAfterSuccessfulAuthentication_validReciept(){
		String newReceipt = "uwu";
		when(mockAuthReceiptDAO.replaceReceipt(userId, receipt)).thenReturn(newReceipt);

		//method under test
		LoginResponse loginResponse = authManager.getLoginResponseAfterSuccessfulPasswordAuthentication(userId, receipt);

		assertEquals(newReceipt, loginResponse.getAuthenticationReceipt());
		assertEquals(synapseSessionToken, loginResponse.getSessionToken());

		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockAuthReceiptDAO).replaceReceipt(userId, receipt);
	}

	@Test
	public void testGetLoginResponseAfterSuccessfulAuthentication_nullReciept_underReceiptLimit(){
		//method under test
		LoginResponse loginResponse = authManager.getLoginResponseAfterSuccessfulPasswordAuthentication(userId, null);

		assertEquals(receipt, loginResponse.getAuthenticationReceipt());
		assertEquals(synapseSessionToken, loginResponse.getSessionToken());

		verify(mockAuthReceiptDAO).createNewReceipt(userId);
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(),anyString());
	}

	@Test
	public void testGetLoginResponseAfterSuccessfulAuthentication_nullReciept_overReceiptLimit(){
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);

		//method under test
		LoginResponse loginResponse = authManager.getLoginResponseAfterSuccessfulPasswordAuthentication(userId, null);

		assertEquals(null, loginResponse.getAuthenticationReceipt());
		assertEquals(synapseSessionToken, loginResponse.getSessionToken());

		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
	}

	///////////////////////////////////////////
	// validateAuthReceiptAndCheckPassword()
	////////////////////////////////////////////

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithoutReceipt() {
		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, null);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithInvalidReceipt() {
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);

		//method under test
		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithInvalidReceiptAndWrongPassword() {
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(false);


		try {
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);
			fail("expected exception to be thrown");
		} catch (UnauthenticatedException e) {
			//expected the exception to be thrown
		}

		verify(mockUserCredentialValidator, never()).checkPassword(userId, password);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(anyLong());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithValidReceipt() {
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);


		authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WithValidReceiptAndWrongPassword() {
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
		when(mockUserCredentialValidator.checkPassword(userId, password)).thenReturn(false);


		try {
			//method under test
			authManager.validateAuthReceiptAndCheckPassword(userId, password, receipt);
			fail("expected exception to be thrown");
		} catch (UnauthenticatedException e) {
			//expected the exception to be thrown
		}

		verify(mockUserCredentialValidator).checkPassword(userId, password);
		verify(mockUserCredentialValidator, never()).checkPasswordWithThrottling(userId, password);
		verify(mockAuthReceiptDAO, never()).createNewReceipt(anyLong());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WeakPassword_NotUsersActualPassword(){
		//case where someone tries to brute force a weak password such as "password123", but is not the user's actual password

		when(mockUserCredentialValidator.checkPasswordWithThrottling(userId, password)).thenReturn(false);

		try {
			authManager.validateAuthReceiptAndCheckPassword(userId, password, null);
			fail("expected exception to be thrown");
		} catch (UnauthenticatedException e){
			//expected
		}

		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockPassswordValidator, never()).validatePassword(password);
	}

	@Test
	public void testValidateAuthReceiptAndCheckPassword_WeakPassword_PassPasswordCheck(){
		//case where someone's actual password is a weak password such as "password123"

		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(password);

		try {
			authManager.validateAuthReceiptAndCheckPassword(userId, password, null);
			fail("expected exception to be thrown");
		} catch (PasswordResetViaEmailRequiredException e){
			//expected
			assertEquals("You must change your password via email reset.", e.getMessage());
		}

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

		try{
			authManager.findUserIdForAuthentication(username);
			fail("Expected UnauthenticatedException to be thrown");
		} catch (UnauthenticatedException e){
			//expected
			assertEquals(UnauthenticatedException.MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT, e.getMessage());
		}
	}


	/////////////////////////////////////////////////////////////
	// validateChangePassword(ChangePasswordWithCurrentPassword)
	/////////////////////////////////////////////////////////////
	@Test(expected = IllegalArgumentException.class)
	public void testValidateChangePassword_withCurrentPassword_nullUsername(){
		changePasswordWithCurrentPassword.setUsername(null);

		//method under test
		authManager.validateChangePassword(changePasswordWithCurrentPassword);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateChangePassword_withCurrentPassword_nullCurrentPassword(){
		changePasswordWithCurrentPassword.setCurrentPassword(null);

		//method under test
		authManager.validateChangePassword(changePasswordWithCurrentPassword);
	}

	@Test
	public void testValidateChangePassword_withCurrentPassword_valid(){

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

	@Test (expected = UnauthenticatedException.class)
	public void testValidateChangePassword_withToken_invalidToken(){
		when(mockPasswordResetTokenGenerator.isValidToken(passwordResetSignedToken)).thenReturn(false);

		authManager.validateChangePassword(changePasswordWithToken);
	}

	@Test
	public void testValidateChangePassword_withToken_valid(){
		//method under test
		Long validatedUserId = authManager.validateChangePassword(changePasswordWithToken);

		assertEquals(validatedUserId, userId);

		verify(mockPasswordResetTokenGenerator).isValidToken(passwordResetSignedToken);
	}


	////////////////////
	// changePassword()
	////////////////////

	@Test(expected = IllegalArgumentException.class)
	public void testChangePassword_NullNewPassword(){
		changePasswordWithCurrentPassword.setNewPassword(null);
		authManager.changePassword(changePasswordWithCurrentPassword);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testChangePassword_unknownImplementation(){
		//use anonymous implementation
		authManager.changePassword(new ChangePasswordInterface() {
			@Override
			public String getNewPassword() {
				return "";
			}

			@Override
			public void setNewPassword(String newPassword) {

			}

			@Override
			public String getConcreteType() {
				return null;
			}

			@Override
			public void setConcreteType(String concreteType) {

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
	}

	@Test
	public void testChangePassword_withCurrentPassword(){
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
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(newChangedPassword);

		try {
			//method under test
			Long changedPasswordUserId = authManager.changePassword(changePasswordWithCurrentPassword);
			fail();
		} catch (InvalidPasswordException e){
			//expected
		}

		verify(mockPassswordValidator).validatePassword(newChangedPassword);
		verifyZeroInteractions(mockPasswordResetTokenGenerator);
		verify(mockUserCredentialValidator).checkPasswordWithThrottling(userId, password);
		verify(mockAuthDAO, never()).deleteSessionToken(userId);
		verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
	}
}
