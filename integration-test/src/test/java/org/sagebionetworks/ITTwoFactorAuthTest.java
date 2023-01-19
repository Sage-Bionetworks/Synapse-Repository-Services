package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorState;

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
		assertEquals(TwoFactorState.DISABLED, synapseClient.get2faStatus().getStatus());
		
		TotpSecret secret = synapseClient.init2Fa();
		
		TwoFactorAuthStatus status = synapseClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(secret.getSecretId())
			.setTotp(generateTotpCode(secret.getSecret()))
		);
		
		assertEquals(TwoFactorState.ENABLED, status.getStatus());
		
		// Now generates a new secret
		TotpSecret newSecret = synapseClient.init2Fa();
		
		// 2FA is still enabled
		assertEquals(TwoFactorState.ENABLED, synapseClient.get2faStatus().getStatus());
				
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
		
		synapseClient.disable2fa();
		
		assertEquals(TwoFactorState.DISABLED, synapseClient.get2faStatus().getStatus());
	}
	
	private String generateTotpCode(String secret) throws TimeProviderException, CodeGenerationException {
		return totpGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));
	}
	
}
