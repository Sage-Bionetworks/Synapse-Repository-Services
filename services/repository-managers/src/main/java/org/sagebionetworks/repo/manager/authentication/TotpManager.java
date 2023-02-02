package org.sagebionetworks.repo.manager.authentication;

import java.util.Arrays;
import java.util.List;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

public class TotpManager {
	
	public static int SECRET_LENGHT = 32;
	public static int DIGITS_COUNT = 6;
	public static int PERIOD = 30;
	public static HashingAlgorithm HASH_ALG = HashingAlgorithm.SHA1;
	public static int RECOVERY_CODES_COUNT = 10;
	
	private SecretGenerator secretGenerator;
	private CodeVerifier codeVerifier;
	private RecoveryCodeGenerator recoveryCodesGenerator;

	public TotpManager(SecretGenerator secretGenerator, CodeVerifier codeVerifier, RecoveryCodeGenerator recoveryCodesGenerator) {
		this.secretGenerator = secretGenerator;
		this.codeVerifier = codeVerifier;
		this.recoveryCodesGenerator = recoveryCodesGenerator;
	}
	
	public String generateTotpSecret() {
		return secretGenerator.generate();
	}
	
	public boolean isTotpValid(String secret, String otp) {
		return codeVerifier.isValidCode(secret, otp);
	}
	
	public List<String> generateRecoveryCodes() {
		return Arrays.asList(recoveryCodesGenerator.generateCodes(RECOVERY_CODES_COUNT));
	}

}
