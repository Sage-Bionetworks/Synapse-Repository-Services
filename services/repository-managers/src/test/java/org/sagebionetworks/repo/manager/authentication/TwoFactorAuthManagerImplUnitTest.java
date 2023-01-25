package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;
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
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.dbo.otp.DBOOtpSecret;
import org.sagebionetworks.repo.model.dbo.otp.OtpSecretDao;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.AESEncryptionUtils;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class TwoFactorAuthManagerImplUnitTest {

	@Mock
	private TotpManager mockTotpManager;
	
	@Mock
	private OtpSecretDao mockOtpSecretDao;
	
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	@Spy
	private TwoFactorAuthManagerImpl manager;
	
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
		
		verifyNoMoreInteractions(mockOtpSecretDao);		
	}
	
	@Test
	public void testEnable2FaWith2FaWithExistingActiveSecret() {
		doNothing().when(manager).assertValidUser(any());
		
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
		
		verifyZeroInteractions(mockOtpSecretDao);
		verifyZeroInteractions(mockTotpManager);
	}
	
	@Test
	public void testGet2FAStatus() {
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(false);
		
		TwoFactorAuthStatus expected = new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED);
		
		// Call under test
		TwoFactorAuthStatus result = manager.get2FaStatus(user);
		
		assertEquals(expected, result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
	}
	
	@Test
	public void testGet2FAStatusWithActiveSecret() {
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(true);
		
		TwoFactorAuthStatus expected = new TwoFactorAuthStatus().setStatus(TwoFactorState.ENABLED);
		
		// Call under test
		TwoFactorAuthStatus result = manager.get2FaStatus(user);
		
		assertEquals(expected, result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
	}
	
	@Test
	public void testDisabled2Fa() {
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(true);
		
		// Call under test
		manager.disable2Fa(user);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
		verify(mockOtpSecretDao).deleteSecrets(user.getId());
	}
	
	@Test
	public void testDisabled2FaWithNoActiveSecret() {
		doNothing().when(manager).assertValidUser(any());
		
		when(mockOtpSecretDao.hasActiveSecret(any())).thenReturn(false);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.disable2Fa(user);
		}).getMessage();
		
		assertEquals("Two factor authentication is not enabled", result);
		
		verify(mockOtpSecretDao).hasActiveSecret(user.getId());
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
	public void testIs2FaOtpCodeValid() {
		doNothing().when(manager).assertValidUser(any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.of(dbSecret));
		doReturn(true).when(manager).isTotpValid(any(), any(), any());
		
		// Call under test
		boolean result = manager.is2FaCodeValid(user, TwoFactorAuthOtpType.TOTP, "12345");
		
		assertTrue(result);
		
		verify(manager).assertValidUser(user);
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
		verify(manager).isTotpValid(user, dbSecret, "12345");
	}
	
	@Test
	public void testIs2FaOtpCodeWithUnsupportedOtpType() {
		doNothing().when(manager).assertValidUser(any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.of(dbSecret));
		
		String result = assertThrows(UnsupportedOperationException.class, () -> {			
			// Call under test
			manager.is2FaCodeValid(user, TwoFactorAuthOtpType.RECOVERY_CODE, "12345");
		}).getMessage();
		
		assertEquals("2FA code type RECOVERY_CODE not supported yet.", result);
		
		verify(manager).assertValidUser(user);
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
	}
	
	@Test
	public void testIs2FaOtpCodeValidWithInvalid() {
		doNothing().when(manager).assertValidUser(any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.of(dbSecret));
		doReturn(false).when(manager).isTotpValid(any(), any(), any());
		
		// Call under test
		boolean result = manager.is2FaCodeValid(user, TwoFactorAuthOtpType.TOTP, "12345");
		
		assertFalse(result);
		
		verify(manager).assertValidUser(user);
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
		verify(manager).isTotpValid(user, dbSecret, "12345");
	}
	
	@Test
	public void testIs2FaOtpCodeValidWith2FaDisabled() {
		doNothing().when(manager).assertValidUser(any());
		when(mockOtpSecretDao.getActiveSecret(any())).thenReturn(Optional.empty());
		
		String result = assertThrows(IllegalArgumentException.class, () -> { 			
			// Call under test
			manager.is2FaCodeValid(user, TwoFactorAuthOtpType.TOTP, "12345");
		}).getMessage();
		
		assertEquals("Two factor authentication is not enabled", result);
		
		verify(manager).assertValidUser(user);
		verify(mockOtpSecretDao).getActiveSecret(user.getId());
	}
	
	@Test
	public void testIs2FaOtpCodeValidWithNoCode() {
		doNothing().when(manager).assertValidUser(any());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.is2FaCodeValid(user, TwoFactorAuthOtpType.TOTP, null);
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
	public void testIs2FaLoginTokenValid() {
		doNothing().when(manager).assertValidUser(any());
		doNothing().when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		boolean result = manager.is2FaLoginTokenValid(user, encodeToken(token));
		
		assertTrue(result);
		
		verify(mockTokenGenerator).validateToken(token);
	}
	
	@Test
	public void testIs2FaLoginTokenValidWithDifferentUser() {
		doNothing().when(manager).assertValidUser(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(4567L)
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		boolean result = manager.is2FaLoginTokenValid(user, encodeToken(token));
		
		assertFalse(result);
		
		verifyZeroInteractions(mockTokenGenerator);
	}
	
	@Test
	public void testIs2FaLoginTokenValidWithNoToken() {
		doNothing().when(manager).assertValidUser(any());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			manager.is2FaLoginTokenValid(user, null);
		}).getMessage();

		assertEquals("The token is required and must not be the empty string.", result);
		
		verifyZeroInteractions(mockTokenGenerator);
	}
	
	@Test
	public void testIs2FaLoginTokenValidWithUnauthorizedException() {
		doNothing().when(manager).assertValidUser(any());
		doThrow(UnauthorizedException.class).when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		boolean result = manager.is2FaLoginTokenValid(user, encodeToken(token));
		
		assertFalse(result);
		
		verify(mockTokenGenerator).validateToken(token);
	}
	
	@Test
	public void testIs2FaLoginTokenValidWithIllegalArgException() {
		doNothing().when(manager).assertValidUser(any());
		doThrow(IllegalArgumentException.class).when(mockTokenGenerator).validateToken(any());
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(new Date(12345))
			.setExpiresOn(new Date(12345 + TwoFactorAuthManagerImpl.TWO_FA_TOKEN_DURATION_MINS * 60 * 1000));
		
		boolean result = manager.is2FaLoginTokenValid(user, encodeToken(token));
		
		assertFalse(result);
		
		verify(mockTokenGenerator).validateToken(token);
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
