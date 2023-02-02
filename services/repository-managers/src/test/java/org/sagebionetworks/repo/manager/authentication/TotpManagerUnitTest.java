package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

@ExtendWith(MockitoExtension.class)
public class TotpManagerUnitTest {
	
	@Mock
	private SecretGenerator mockSecretGenerator;
	
	@Mock
	private CodeVerifier mockCodeVerifier;
	
	@Mock
	private RecoveryCodeGenerator mockRecoveryCodesGen;
	
	@InjectMocks
	private TotpManager manager;

	@Test
	public void testGenerateTotpSecret() {
		String expected = "secret";
		
		when(mockSecretGenerator.generate()).thenReturn(expected);
		
		// Call under test
		String result = manager.generateTotpSecret();
		
		assertEquals(expected, result);
		
		verify(mockSecretGenerator).generate();
		
	}
	
	@Test
	public void testIsValidCode() {
		String secret = "secret";
		String code = "12345";
		
		when(mockCodeVerifier.isValidCode(any(), any())).thenReturn(true);
		
		boolean result = manager.isTotpValid(secret, code);
		
		assertTrue(result);
		
		verify(mockCodeVerifier).isValidCode(secret, code);
	}
	
	@Test
	public void testIsValidCodeFalse() {
		String secret = "secret";
		String code = "12345";
		
		when(mockCodeVerifier.isValidCode(any(), any())).thenReturn(false);
		
		boolean result = manager.isTotpValid(secret, code);
		
		assertFalse(result);
		
		verify(mockCodeVerifier).isValidCode(secret, code);
	}
	
	@Test
	public void testGenerateRecoveryCodes() {
		String[] codes = new String[] {"codeOne", "codeTwo"};
		
		when(mockRecoveryCodesGen.generateCodes(anyInt())).thenReturn(codes);
		
		// Call under test
		List<String> result = manager.generateRecoveryCodes();
		
		assertEquals(Arrays.asList(codes), result);
		
		verify(mockRecoveryCodesGen).generateCodes(TotpManager.RECOVERY_CODES_COUNT);		
	}
}
