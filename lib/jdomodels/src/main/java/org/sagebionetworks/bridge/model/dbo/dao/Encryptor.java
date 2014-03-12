package org.sagebionetworks.bridge.model.dbo.dao;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

	public byte[] encrypt(byte[] clearText, String keyString, Long saltLong) throws GeneralSecurityException {
		return crypt(Cipher.ENCRYPT_MODE, clearText, keyString, saltLong);
	}

	public byte[] decrypt(byte[] ciphertext, String keyString, Long saltLong) throws GeneralSecurityException {
		return crypt(Cipher.DECRYPT_MODE, ciphertext, keyString, saltLong);
	}

	private byte[] crypt(int direction, byte[] input, String keyString, Long saltLong) throws GeneralSecurityException {
		Key key = getKey(keyString, saltLong);
		Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
		aes.init(direction, key);
		byte[] output = aes.doFinal(input);
		return output;
	}

	private Key getKey(String keyString, Long saltLong) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] salt = createSalt(saltLong);
		int iterations = 10000;
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		SecretKey tmp = factory.generateSecret(new PBEKeySpec(keyString.toCharArray(), salt, iterations, 128));
		SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");
		return key;
	}

	private byte[] createSalt(Long saltLong) {
		String saltString = saltLong.toString();
		while (saltString.length() < 8) {
			saltString += saltString;
		}
		byte[] salt = saltString.getBytes();
		return salt;
	}
}
