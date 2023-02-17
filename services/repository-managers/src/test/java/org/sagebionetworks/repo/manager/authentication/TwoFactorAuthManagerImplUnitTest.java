package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.dbo.otp.DBOOtpSecret;
import org.sagebionetworks.repo.model.dbo.otp.OtpSecretDao;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.AESEncryptionUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class TwoFactorAuthManagerImplUnitTest {

	@Mock
	private TotpManager mockTotpManager;
	
	@Mock
	private OtpSecretDao mockOtpSecretDao;
	
	@Mock
	private AuthenticationDAO mockAuthDao;
	
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private Clock mockClock;
	
	@Mock
	private TemplatedMessageSender mockMessageSender;
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	@Spy
	private TwoFactorAuthManagerImpl manager;
	
	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;
	
	private UserInfo user;
	
	private String totpSecret = "totp secret";
	private String totpEncryptionPassword = "totp password";
	private String userEncryptionKey;
	private String encryptedTotpSecret;
	private DBOOtpSecret dbSecret;
	
	@Captor
	private ArgumentCaptor<String> stringCaptor;
	
	@BeforeEach
	public void before() {
		user = new UserInfo(false, 123L);
		
		userEncryptionKey = AESEncryptionUtils.newSecretKeyFromPassword(totpEncryptionPassword, user.getId().toString());
		encryptedTotpSecret = AESEncryptionUtils.encryptWithAESGCM(totpSecret, userEncryptionKey);
		
		dbSecret = new DBOOtpSecret();
		dbSecret.setId(456L);
		dbSecret.setActive(false);
		dbSecret.setSecret(encryptedTotpSecret);
		dbSecret.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		dbSecret.setEtag("etag");
		dbSecret.setUserId(user.getId());
	}
	
	@Test
	public void testInit2FA() {
		doNothing().when(manager).assertValidUser(any());
		doReturn(userEncryptionKey).when(manager).getUserEncryptionKey(any());

		when(mockTotpManager.generateTotpSecret()).thenReturn(totpSecret);
		when(mockOtpSecretDao.storeSecret(any(), any())).thenReturn(dbSecret);
		
		TotpSecret expected = new TotpSecret()
			.setAlg(TotpManager.HASH_ALG.getFriendlyName())
			.setDigits(Long.valueOf(TotpManager.DIGITS_COUNT))
			.setPeriod(Long.valueOf(TotpManager.PERIOD))
			.setSecret(totpSecret)
			.setSecretId(dbSecret.getId().toString());
			
		// Call under test
		TotpSecret result = manager.init2Fa(user);
		
		assertEquals(expected, result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getUserEncryptionKey(user);
		verify(mockTotpManager).generateTotpSecret();
		verify(mockOtpSecretDao).storeSecret(eq(user.getId()), stringCaptor.capture());
		
		assertEquals(totpSecret, AESEncryptionUtils.decryptWithAESGCM(stringCaptor.getValue(), userEncryptionKey));
	}
	
	@Test
	public void testEnable2Fa() {
		doNothing().when(manager).assertValidUser(any());
		doNothing().when(manager).send2FaStateChangeNotification(any(), any());
		
		when(mockOtpSecretDao.getSecret(any(), any())).thenReturn(Optional.of(dbSecret));
		doReturn(true).when(manager).isTotpValid(any(), any(), any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.empty());
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
			.setSecretId("789")
			.setTotp("12345");
		
		// Call under test
		manager.enable2Fa(user, request);
		
		verify(mockOtpSecretDao).getSecret(user.getId(), 789L);
		verify(manager).isTotpValid(user, dbSecret, "12345");
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
		verify(mockOtpSecretDao).activateSecret(user.getId(), 789L);
		verify(mockAuthDao).setTwoFactorAuthState(user.getId(), true);
		verify(manager).send2FaStateChangeNotification(user, TwoFactorState.ENABLED);
		
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWith2FaWithExistingActiveSecret() {
		doNothing().when(manager).assertValidUser(any());
		doNothing().when(manager).send2FaStateChangeNotification(any(), any());
		
		DBOOtpSecret activeSecret = new DBOOtpSecret();
		activeSecret.setId(654L);
		
		when(mockOtpSecretDao.getSecret(any(), any())).thenReturn(Optional.of(dbSecret));
		doReturn(true).when(manager).isTotpValid(any(), any(), any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.of(activeSecret));
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
			.setSecretId("789")
			.setTotp("12345");
		
		// Call under test
		manager.enable2Fa(user, request);
		
		verify(mockOtpSecretDao).getSecret(user.getId(), 789L);
		verify(manager).isTotpValid(user, dbSecret, "12345");
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
		verify(mockOtpSecretDao).deleteSecret(user.getId(), activeSecret.getId());
		verify(mockOtpSecretDao).activateSecret(user.getId(), 789L);
		verify(mockAuthDao).setTwoFactorAuthState(user.getId(), true);
		verify(manager).send2FaStateChangeNotification(user, TwoFactorState.ENABLED);
		
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWithInvalidSecret() {
		dbSecret.setActive(true);
		
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.getSecret(any(), any())).thenReturn(Optional.empty());
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
			.setSecretId("789")
			.setTotp("12345");
		
		String result = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("Invalid secret id", result);
		
		verify(mockOtpSecretDao).getSecret(user.getId(), 789L);
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyNoMoreInteractions(mockTotpManager);
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWithSecretEnabled() {
		dbSecret.setActive(true);
		
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.getSecret(any(), any())).thenReturn(Optional.of(dbSecret));
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
			.setSecretId("789")
			.setTotp("12345");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("Two factor authentication is already enabled with this secret", result);
		
		verify(mockOtpSecretDao).getSecret(user.getId(), 789L);
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyNoMoreInteractions(mockTotpManager);
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWithInvalidTotp() {
		doNothing().when(manager).assertValidUser(any());

		when(mockOtpSecretDao.getSecret(any(), any())).thenReturn(Optional.of(dbSecret));
		doReturn(false).when(manager).isTotpValid(any(), any(), any());
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
			.setSecretId("789")
			.setTotp("12345");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("Invalid totp code", result);
		
		verify(mockOtpSecretDao).getSecret(user.getId(), 789L);
		verify(manager).isTotpValid(user, dbSecret, "12345");
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyNoMoreInteractions(mockTotpManager);
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWithNoRequest() {
		doNothing().when(manager).assertValidUser(any());
		
		TotpSecretActivationRequest request = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyZeroInteractions(mockOtpSecretDao);
		verifyZeroInteractions(mockTotpManager);
	}
	
	@Test
	public void testEnable2FaWithNoSecretId() {
		doNothing().when(manager).assertValidUser(any());
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
				.setSecretId(null)
				.setTotp("12345");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("The secret id is required.", result);
		
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyZeroInteractions(mockOtpSecretDao);
		verifyZeroInteractions(mockTotpManager);
	}
	
	@Test
	public void testEnable2FaWithNoTotpCode() {
		doNothing().when(manager).assertValidUser(any());
		
		TotpSecretActivationRequest request = new TotpSecretActivationRequest()
				.setSecretId("789")
				.setTotp(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.enable2Fa(user, request);
		}).getMessage();
		
		assertEquals("The totp code is required.", result);
		
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyZeroInteractions(mockOtpSecretDao);
		verifyZeroInteractions(mockTotpManager);
	}
	
	@Test
	public void testGet2FAStatus() {
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(false);
		
		TwoFactorAuthStatus expected = new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED);
		
		// Call under test
		TwoFactorAuthStatus result = manager.get2FaStatus(user);
		
		assertEquals(expected, result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
	}
	
	@Test
	public void testGet2FAStatusWithActiveSecret() {	
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(true);
		
		TwoFactorAuthStatus expected = new TwoFactorAuthStatus().setStatus(TwoFactorState.ENABLED);
		
		// Call under test
		TwoFactorAuthStatus result = manager.get2FaStatus(user);
		
		assertEquals(expected, result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
	}
	
	@Test
	public void testGet2FAStatusWithAnonymousUser() {
		
		user = new UserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		TwoFactorAuthStatus expected = new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED);
		
		// Call under test
		TwoFactorAuthStatus result = manager.get2FaStatus(user);
		
		assertEquals(expected, result);
		
		verifyZeroInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testDisable2Fa() {
		doNothing().when(manager).assertValidUser(any());
		doNothing().when(manager).send2FaStateChangeNotification(any(), any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(true);
		
		// Call under test
		manager.disable2Fa(user);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
		verify(mockOtpSecretDao).deleteSecrets(user.getId());
		verify(mockAuthDao).setTwoFactorAuthState(user.getId(), false);
		verify(manager).send2FaStateChangeNotification(user, TwoFactorState.DISABLED);
	}
	
	@Test
	public void testDisable2FaWithNoActiveSecret() {
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(false);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.disable2Fa(user);
		}).getMessage();
		
		assertEquals("Two factor authentication is not enabled", result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
		verify(manager, never()).send2FaStateChangeNotification(any(), any());
		verifyZeroInteractions(mockAuthDao);
		verifyNoMoreInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testAssertValidUser() {
		// Call under test
		manager.assertValidUser(user);
	}
	
	@Test
	public void testAssertValidUserWithNull() {
		user = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.assertValidUser(user);
		}).getMessage();
		
		assertEquals("The user is required.", result);
	}
	
	@Test
	public void testAssertValidUserWithAnonymousUser() {
		user = new UserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		String result = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.assertValidUser(user);
		}).getMessage();
		
		assertEquals("You need to authenticate to perform this action", result);
	}
	
	@Test
	public void testGetUserEncryptionKey() {
		when(mockConfig.getOtpSecretsPassword()).thenReturn(totpEncryptionPassword);
		
		String expected = AESEncryptionUtils.newSecretKeyFromPassword(totpEncryptionPassword, user.getId().toString());
		
		// Call under test
		String result = manager.getUserEncryptionKey(user);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testIsTotpValid() {
		doReturn(userEncryptionKey).when(manager).getUserEncryptionKey(any());
		when(mockTotpManager.isTotpValid(any(), any())).thenReturn(true);
		
		boolean result = manager.isTotpValid(user, dbSecret, "12345");
		
		assertTrue(result);
		
		verify(manager).getUserEncryptionKey(user);
		verify(mockTotpManager).isTotpValid(totpSecret, "12345");
	}
	
	@Test
	public void testIsTotpValidWithInvalid() {
		doReturn(userEncryptionKey).when(manager).getUserEncryptionKey(any());
		when(mockTotpManager.isTotpValid(any(), any())).thenReturn(false);
		
		boolean result = manager.isTotpValid(user, dbSecret, "12345");
		
		assertFalse(result);
		
		verify(manager).getUserEncryptionKey(user);
		verify(mockTotpManager).isTotpValid(totpSecret, "12345");		
	}

	@Test
	public void testValidate2FaTotpCode() {
		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		doReturn(true).when(manager).isTotpValid(any(), any(), any());
		
		// Call under test
		boolean result = manager.validate2FaTotpCode(user, "12345");
		
		assertTrue(result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(manager).isTotpValid(user, dbSecret, "12345");
	}
	
	@Test
	public void testValidate2FaTotpCodeWithInvalid() {
		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		doReturn(false).when(manager).isTotpValid(any(), any(), any());
		
		// Call under test
		boolean result = manager.validate2FaTotpCode(user, "12345");
		
		assertFalse(result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(manager).isTotpValid(user, dbSecret, "12345");
	}
	
	@Test
	public void testValidate2FaTotpCodeWithNoCode() {
		doNothing().when(manager).assertValidUser(any());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate2FaTotpCode(user, null);
		}).getMessage();

		assertEquals("The otpCode is required and must not be the empty string.", result);
		
		verify(manager).assertValidUser(user);
		verifyZeroInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testGenerate2FaLoginToken() {
		doNothing().when(manager).assertValidUser(any());
		when(mockClock.now()).thenReturn(new Date(12345));
		doNothing().when(mockTokenGenerator).signToken(any());
		
		TwoFactorAuthToken expected = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
				
		// Call under test
		String result = manager.generate2FaLoginToken(user);
		
		assertEquals(expected, decodeLoginToken(result));
		
		verify(mockClock).now();
		verify(mockTokenGenerator).signToken(expected);
	}
	
	@Test
	public void testValidate2FaLoginToken() {
		doNothing().when(manager).assertValidUser(any());
		doNothing().when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		// Call under test
		boolean result = manager.validate2FaLoginToken(user, encodeToken(token));
		
		assertTrue(result);
		
		verify(mockTokenGenerator).validateToken(token);
	}
	
	@Test
	public void testValidate2FaLoginTokenWithDifferentUser() {
		doNothing().when(manager).assertValidUser(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(4567L)
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		// Call under test
		boolean result = manager.validate2FaLoginToken(user, encodeToken(token));
		
		assertFalse(result);
		
		verifyZeroInteractions(mockTokenGenerator);
	}
	
	@Test
	public void testValidate2FaLoginTokenWithNoToken() {
		doNothing().when(manager).assertValidUser(any());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate2FaLoginToken(user, null);
		}).getMessage();

		assertEquals("The token is required and must not be the empty string.", result);
		
		verifyZeroInteractions(mockTokenGenerator);
	}
	
	@Test
	public void testValidate2FaLoginTokenWithUnauthorizedException() {
		doNothing().when(manager).assertValidUser(any());
		doThrow(UnauthorizedException.class).when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		// Call under test
		boolean result = manager.validate2FaLoginToken(user, encodeToken(token));
		
		assertFalse(result);
		
		verify(mockTokenGenerator).validateToken(token);
	}

	@Test
	public void testValidate2FaLoginTokenWithIllegalArgException() {
		doNothing().when(manager).assertValidUser(any());
		doThrow(IllegalArgumentException.class).when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		// Call under test
		boolean result = manager.validate2FaLoginToken(user, encodeToken(token));
		
		assertFalse(result);
		
		verify(mockTokenGenerator).validateToken(token);
	}
	
	@Test
	public void testGetActiveSecretOrThrow() {
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.of(dbSecret));
		
		// Call under test
		DBOOtpSecret result = manager.getActiveSecretOrThrow(user);
		
		assertEquals(dbSecret, result);
		
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
	}
	
	@Test
	public void testGetActiveSecretOrThrowWith2FaDisabled() {
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.empty());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getActiveSecretOrThrow(user);
		}).getMessage();
		
		assertEquals("Two factor authentication is not enabled", result);
		
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
	}
	
	@Test
	public void testGenerateRecoveryCodes() {
		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		doNothing().when(manager).send2FaRecoveryCodesGeneratedNotification(any());
		
		List<String> recoveryCodes = List.of("one", "two");
		
		when(mockTotpManager.generateRecoveryCodes()).thenReturn(recoveryCodes);
		
		TwoFactorAuthRecoveryCodes expected = new TwoFactorAuthRecoveryCodes().setCodes(recoveryCodes);
		
		// Call under test
		TwoFactorAuthRecoveryCodes result = manager.generate2FaRecoveryCodes(user);
		
		assertEquals(expected, result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(mockTotpManager).generateRecoveryCodes();
		verify(mockOtpSecretDao).deleteRecoveryCodes(dbSecret.getId());
		verify(mockOtpSecretDao).storeRecoveryCodes(eq(dbSecret.getId()), stringListCaptor.capture());
		
		List<String> storedCodes = stringListCaptor.getValue();
		
		for (int i=0; i<recoveryCodes.size(); i++) {
			String storedCode = storedCodes.get(i);
			String actualCode = recoveryCodes.get(i);
			byte[] salt = PBKDF2Utils.extractSalt(storedCode);
			assertEquals(PBKDF2Utils.hashPassword(actualCode, salt), storedCode);
		}
		
		verify(mockOtpSecretDao).touchSecret(dbSecret.getId());
		verify(manager).send2FaRecoveryCodesGeneratedNotification(user);
	}
	
	@Test
	public void testValidate2FaRecoveryCode() {
		String recoveryCode = "someCode";
		String recoveryCodeHash = PBKDF2Utils.hashPassword(recoveryCode, null);
		
		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		doNothing().when(manager).send2FaRecoveryCodeUsedNotification(any(), anyInt());
		when(mockOtpSecretDao.getRecoveryCodes(any())).thenReturn(List.of(PBKDF2Utils.hashPassword("anotherCode", null), recoveryCodeHash));
		when(mockOtpSecretDao.deleteRecoveryCode(any(), any())).thenReturn(true);
		
		// Call under test
		boolean result = manager.validate2FaRecoveryCode(user, recoveryCode);
		
		assertTrue(result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(mockOtpSecretDao).getRecoveryCodes(dbSecret.getId());
		verify(mockOtpSecretDao).deleteRecoveryCode(dbSecret.getId(), recoveryCodeHash);
		verify(mockOtpSecretDao).touchSecret(dbSecret.getId());
		verify(manager).send2FaRecoveryCodeUsedNotification(user, 1);
		verifyNoMoreInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testValidate2FaRecoveryCodeWithNoMatch() {
		String recoveryCode = "someCode";

		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		when(mockOtpSecretDao.getRecoveryCodes(any())).thenReturn(List.of(PBKDF2Utils.hashPassword("anotherCode", null), PBKDF2Utils.hashPassword("yetAnother", null)));
				
		// Call under test
		boolean result = manager.validate2FaRecoveryCode(user, recoveryCode);
		
		assertFalse(result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(mockOtpSecretDao).getRecoveryCodes(dbSecret.getId());
		verifyNoMoreInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testValidate2FaRecoveryCodeWithMatchAndDeleted() {
		String recoveryCode = "someCode";
		String recoveryCodeHash = PBKDF2Utils.hashPassword(recoveryCode, null);
		
		doNothing().when(manager).assertValidUser(any());
		doReturn(dbSecret).when(manager).getActiveSecretOrThrow(any());
		when(mockOtpSecretDao.getRecoveryCodes(any())).thenReturn(List.of(PBKDF2Utils.hashPassword("anotherCode", null), recoveryCodeHash));
		// Race condition, might have been consumed already
		when(mockOtpSecretDao.deleteRecoveryCode(any(), any())).thenReturn(false);
				
		// Call under test
		boolean result = manager.validate2FaRecoveryCode(user, recoveryCode);
		
		assertFalse(result);
		
		verify(manager).assertValidUser(user);
		verify(manager).getActiveSecretOrThrow(user);
		verify(mockOtpSecretDao).getRecoveryCodes(dbSecret.getId());
		verify(mockOtpSecretDao).deleteRecoveryCode(dbSecret.getId(), recoveryCodeHash);
		verifyNoMoreInteractions(mockOtpSecretDao);
	}
	
	@Test
	public void testSend2FaStateChangeNotificationWithEnabled() {
		UserProfile profile = new UserProfile()
			.setFirstName("User")
			.setLastName("Name");
			
		when(mockUserProfileManager.getUserProfile(any())).thenReturn(profile);
		
		MessageTemplate expectedMessage = MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(false)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile("message/TwoFaEnabledNotification.html.vtl")
			.withSubject("Two-Factor Authentication Enabled")
			.withContext(Map.of("displayName", "User Name")).build();
		
		// Call under test
		manager.send2FaStateChangeNotification(user, TwoFactorState.ENABLED);
		
		verify(mockUserProfileManager).getUserProfile(user.getId().toString());
		verify(mockMessageSender).sendMessage(expectedMessage);
	}
	
	@Test
	public void testSend2FaStateChangeNotificationWithDisabled() {
		UserProfile profile = new UserProfile()
			.setFirstName("User")
			.setLastName("Name");
			
		when(mockUserProfileManager.getUserProfile(any())).thenReturn(profile);
		
		MessageTemplate expectedMessage = MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(false)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile("message/TwoFaDisabledNotification.html.vtl")
			.withSubject("Two-Factor Authentication Disabled")
			.withContext(Map.of("displayName", "User Name")).build();
		
		// Call under test
		manager.send2FaStateChangeNotification(user, TwoFactorState.DISABLED);
		
		verify(mockUserProfileManager).getUserProfile(user.getId().toString());
		verify(mockMessageSender).sendMessage(expectedMessage);
	}
	
	@Test
	public void testSend2FaRecoveryCodesGeneratedNotification() {
		UserProfile profile = new UserProfile()
				.setFirstName("User")
				.setLastName("Name");
				
		when(mockUserProfileManager.getUserProfile(any())).thenReturn(profile);
		
		MessageTemplate expectedMessage = MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(false)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile("message/TwoFaRecoveryCodesGeneratedNotification.html.vtl")
			.withSubject("Two-Factor Authentication Recovery Codes Generated")
			.withContext(Map.of("displayName", "User Name")).build();
		
		// Call under test
		manager.send2FaRecoveryCodesGeneratedNotification(user);
		
		verify(mockUserProfileManager).getUserProfile(user.getId().toString());
		verify(mockMessageSender).sendMessage(expectedMessage);
	}
	
	@Test
	public void testSend2FaRecoveryCodesUsedNotification() {
		int codesRemaining = 9;
		
		UserProfile profile = new UserProfile()
				.setFirstName("User")
				.setLastName("Name");
				
		when(mockUserProfileManager.getUserProfile(any())).thenReturn(profile);
		
		MessageTemplate expectedMessage = MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(false)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile("message/TwoFaRecoveryCodeUsedNotification.html.vtl")
			.withSubject("Two-Factor Authentication Recovery Code Used")
			.withContext(Map.of("displayName", "User Name", "codesCount", codesRemaining)).build();
		
		// Call under test
		manager.send2FaRecoveryCodeUsedNotification(user, codesRemaining);
		
		verify(mockUserProfileManager).getUserProfile(user.getId().toString());
		verify(mockMessageSender).sendMessage(expectedMessage);
	}
	
	private String encodeToken(TwoFactorAuthToken token) {
		try {
			String tokenJson = EntityFactory.createJSONStringForEntity(token);
			return new String(Base64.getEncoder().encode(tokenJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private TwoFactorAuthToken decodeLoginToken(String encodedToken) {
		String decodedToken = new String(Base64.getDecoder().decode(encodedToken.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		try {
			return EntityFactory.createEntityFromJSONString(decodedToken, TwoFactorAuthToken.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}
	
}
