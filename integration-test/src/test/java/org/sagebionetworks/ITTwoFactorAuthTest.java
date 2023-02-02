package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseTwoFactorAuthRequiredException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.TimeProviderException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@ExtendWith(ITTestExtension.class)
public class ITTwoFactorAuthTest {
	
	private SynapseClient synapseClient;
	private CodeGenerator totpGenerator;
	private TimeProvider timeProvider;

	public ITTwoFactorAuthTest(SynapseClient synapseClient) {
		this.synapseClient = synapseClient;
		this.totpGenerator = new DefaultCodeGenerator();
		this.timeProvider = new SystemTimeProvider();
	}

	@Test
	public void testEnable2FaRoundTrip() throws SynapseException, CodeGenerationException {
		assertEquals(TwoFactorState.DISABLED, synapseClient.get2FaStatus().getStatus());
		
		TotpSecret secret = synapseClient.init2Fa();
		
		TwoFactorAuthStatus status = synapseClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(secret.getSecretId())
			.setTotp(generateTotpCode(secret.getSecret()))
		);
		
		assertEquals(TwoFactorState.ENABLED, status.getStatus());
		
		// Now generates a new secret
		TotpSecret newSecret = synapseClient.init2Fa();
		
		// 2FA is still enabled
		assertEquals(TwoFactorState.ENABLED, synapseClient.get2FaStatus().getStatus());
				
		String message = assertThrows(SynapseException.class, () -> {
			// The previous secret is already enabled
			synapseClient.enable2Fa(new TotpSecretActivationRequest()
				.setSecretId(secret.getSecretId())
				.setTotp(generateTotpCode(secret.getSecret()))
			);
		}).getMessage();
		
		assertEquals("Two factor authentication is already enabled with this secret", message);
		
		status = synapseClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(newSecret.getSecretId())
			.setTotp(generateTotpCode(newSecret.getSecret()))
		);
		
		assertEquals(TwoFactorState.ENABLED, status.getStatus());
		
		synapseClient.disable2Fa();
		
		assertEquals(TwoFactorState.DISABLED, synapseClient.get2FaStatus().getStatus());
	}
	
	@Test
	public void testLoginWith2Fa(SynapseAdminClient adminClient) throws SynapseException, CodeGenerationException, JSONObjectAdapterException {
		// Creates a new user so that we retain user/password
		SynapseClient newSynapseClient = new SynapseClientImpl();
		
		String username = UUID.randomUUID().toString();
		String password = UUID.randomUUID().toString();
		
		Long userId = SynapseClientHelper.createUser(adminClient, newSynapseClient, username, password, true, false);
		
		// First enabled 2FA
		
		TotpSecret secret = newSynapseClient.init2Fa();
		
		TwoFactorAuthStatus status = newSynapseClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(secret.getSecretId())
			.setTotp(generateTotpCode(secret.getSecret()))
		);
		
		assertEquals(TwoFactorState.ENABLED, status.getStatus());
		
		// Try the normal login
		LoginRequest loginRequest = new LoginRequest().setUsername(username).setPassword(password);
		
		SynapseTwoFactorAuthRequiredException twoFaResponse = assertThrows(SynapseTwoFactorAuthRequiredException.class, () -> {
			newSynapseClient.loginForAccessToken(loginRequest);
		});
		
		// Now authenticate through 2fa
		String totp = generateTotpCode(secret.getSecret());
		
		TwoFactorAuthLoginRequest twoFaLoginRequest = new TwoFactorAuthLoginRequest()
			.setUserId(twoFaResponse.getUserId())
			.setTwoFaToken(twoFaResponse.getTwoFaToken())
			.setOtpType(TwoFactorAuthOtpType.TOTP)
			.setOtpCode(totp);
		
		LoginResponse loginResponse = newSynapseClient.loginWith2Fa(twoFaLoginRequest);
		
		assertNotNull(loginResponse.getAccessToken());
		
		// The user should still be able to perform actions
		assertEquals(TwoFactorState.ENABLED, newSynapseClient.get2FaStatus().getStatus());
		
		newSynapseClient.disable2Fa();
		
		assertEquals(TwoFactorState.DISABLED, newSynapseClient.get2FaStatus().getStatus());
		
		try {
			adminClient.deleteUser(userId);
		} catch (SynapseException e) {
			
		}
	}
	
	@Test
	public void testLoginWithRecoveryCodes(SynapseAdminClient adminClient) throws SynapseException, CodeGenerationException, JSONObjectAdapterException {
		// Creates a new user so that we retain user/password
		SynapseClient newSynapseClient = new SynapseClientImpl();
		
		String username = UUID.randomUUID().toString();
		String password = UUID.randomUUID().toString();
		
		Long userId = SynapseClientHelper.createUser(adminClient, newSynapseClient, username, password, true, false);
		
		// First enabled 2FA
		
		TotpSecret secret = newSynapseClient.init2Fa();
		
		TwoFactorAuthStatus status = newSynapseClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(secret.getSecretId())
			.setTotp(generateTotpCode(secret.getSecret()))
		);
		
		assertEquals(TwoFactorState.ENABLED, status.getStatus());
		
		// Generate a new set of recovery codes
		TwoFactorAuthRecoveryCodes recoveryCodes = newSynapseClient.generate2FaRecoveryCodes();
		
		// Try the normal login
		LoginRequest loginRequest = new LoginRequest().setUsername(username).setPassword(password);
		
		SynapseTwoFactorAuthRequiredException twoFaResponse = assertThrows(SynapseTwoFactorAuthRequiredException.class, () -> {
			newSynapseClient.loginForAccessToken(loginRequest);
		});
		
		// Try one code		
		LoginResponse loginResponse = newSynapseClient.loginWith2Fa(new TwoFactorAuthLoginRequest()
			.setUserId(twoFaResponse.getUserId())
			.setTwoFaToken(twoFaResponse.getTwoFaToken())
			.setOtpType(TwoFactorAuthOtpType.RECOVERY_CODE)
			.setOtpCode(recoveryCodes.getCodes().get(0))
		);
		
		assertNotNull(loginResponse.getAccessToken());
		
		// Regenerate a new set of codes
		recoveryCodes = newSynapseClient.generate2FaRecoveryCodes();
		
		// Now authenticate through 2fa, using all the recovery codes
		for (String recoveryCode : recoveryCodes.getCodes()) {
			TwoFactorAuthLoginRequest twoFaLoginRequest = new TwoFactorAuthLoginRequest()
					.setUserId(twoFaResponse.getUserId())
					.setTwoFaToken(twoFaResponse.getTwoFaToken())
					.setOtpType(TwoFactorAuthOtpType.RECOVERY_CODE)
					.setOtpCode(recoveryCode);
				
			loginResponse = newSynapseClient.loginWith2Fa(twoFaLoginRequest);
			
			assertNotNull(loginResponse.getAccessToken());
			
			// It should not be possible to reuse the recovery code
			SynapseUnauthorizedException ex = assertThrows(SynapseUnauthorizedException.class, () -> {				
				newSynapseClient.loginWith2Fa(twoFaLoginRequest);
			});
			
			assertEquals("The provided code is invalid.", ex.getMessage());
		}
		
		try {
			adminClient.deleteUser(userId);
		} catch (SynapseException e) {
			
		}
	}
	
	private String generateTotpCode(String secret) throws TimeProviderException, CodeGenerationException {
		return totpGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));
	}
	
}
