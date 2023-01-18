package org.sagebionetworks.securitytools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

public class AESEncryptionUtilsTest {
	
	private static final String PLAINTEXT = "some content to encrypt";
	
	@Test
	public void testNewSecretKey() {
		String key = AESEncryptionUtils.newSecretKey();
		
	 	assertEquals(16, Base64.decodeBase64(key).length);
	}
	
	@Test
	public void testNewSecretKeyFromPassword() {
		String password = "1234";
		String salt = "456";
		
		String key = AESEncryptionUtils.newSecretKeyFromPassword(password, salt);

	 	assertEquals(32, Base64.decodeBase64(key).length);
		
		String key2 = AESEncryptionUtils.newSecretKeyFromPassword(password, salt);
		
		assertEquals(key, key2);
		
		String salt2 = "789";
		
		String key3 = AESEncryptionUtils.newSecretKeyFromPassword(password, salt2);
		
		assertNotEquals(key, key3);
		
	}

	@Test
	public void testEncryptionRoundTrip() {
		String key = AESEncryptionUtils.newSecretKey();
		String encrypted = AESEncryptionUtils.encryptDeterministic(PLAINTEXT, key);
		String encrypted2 = AESEncryptionUtils.encryptDeterministic(PLAINTEXT, key);
		
		assertEquals(encrypted, encrypted2);
		
		String decrypted = AESEncryptionUtils.decryptDeterministic(encrypted, key);
		
		assertEquals(PLAINTEXT, decrypted);
	}
	
	@Test
	public void testEncryptionWithAESGCMRoundTrip() {
		String key = AESEncryptionUtils.newSecretKey();
		
		String encrypted = AESEncryptionUtils.encryptWithAESGCM(PLAINTEXT, key);
		String encrypted2 = AESEncryptionUtils.encryptWithAESGCM(PLAINTEXT, key);
		
		assertNotEquals(encrypted, encrypted2);
		
		assertEquals(PLAINTEXT, AESEncryptionUtils.decryptWithAESGCM(encrypted, key));
		assertEquals(PLAINTEXT, AESEncryptionUtils.decryptWithAESGCM(encrypted2, key));
	}

}
