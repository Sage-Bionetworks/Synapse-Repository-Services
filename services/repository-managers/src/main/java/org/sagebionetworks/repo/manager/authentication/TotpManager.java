package org.sagebionetworks.repo.manager.authentication;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.SecretGenerator;

public class TotpManager {
	
	public static int SECRET_LENGHT = 32;
	public static int DIGITS_COUNT = 6;
	public static int PERIOD = 30;
	public static HashingAlgorithm HASH_ALG = HashingAlgorithm.SHA1;
	
	private SecretGenerator secretGenerator;
	private CodeVerifier codeVerifier;

	public TotpManager(SecretGenerator secretGenerator, CodeVerifier codeVerifier) {
		this.secretGenerator = secretGenerator;
		this.codeVerifier = codeVerifier;
	}
	
	public String generateTotpSecret() {
		return secretGenerator.generate();
	}
	
	public boolean isTotpValid(String secret, String otp) {
		return codeVerifier.isValidCode(secret, otp);
	}

}
