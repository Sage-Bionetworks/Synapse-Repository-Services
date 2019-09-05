package org.sagebionetworks.securitytools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncryptionUtilsTest {
	
	private static final String PLAINTEXT = "some content to encrypt";

	@Test
	public void testEncryptionRoundTrip() {
		String key = EncryptionUtils.newSecretKey();
		String encrypted = EncryptionUtils.encrypt(PLAINTEXT, key);
		String decrypted = EncryptionUtils.decrypt(encrypted, key);
		assertEquals(PLAINTEXT, decrypted);
	}

}
