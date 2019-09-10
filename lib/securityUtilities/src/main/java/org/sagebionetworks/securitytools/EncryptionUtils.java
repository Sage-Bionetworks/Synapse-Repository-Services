package org.sagebionetworks.securitytools;


import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class EncryptionUtils {
	
	private static final String AES_ALGORITHM = "AES";
	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

	private static String encodeSecretKeyAsString(Key key) {
	    return Base64.encodeBase64URLSafeString(key.getEncoded());
	}
	
	private static SecretKey decodeSecretKeyFromString(String s) throws InvalidKeyException {
		byte[] bytes = Base64.decodeBase64(s);
		return new SecretKeySpec(bytes, 0, bytes.length, AES_ALGORITHM); 
	}

	/**
	 * Create an encryption key for symmetric encryption of sensitive data to be sent to the client.
	 * Note:  The use case is different than that of the stack encyrption key as the encryption
	 * must be consistent for an indefinite period.  The key is not meant to be shared with the client.
	 * 
	 * @return
	 */
	public static String newSecretKey() {
		try {
			SecretKey key = KeyGenerator.getInstance(AES_ALGORITHM).generateKey();
		    return encodeSecretKeyAsString(key);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Encrypt and base 64 encode a plain text string.
	 * 
	 * @param plaintext the text to encrypt
	 * @param key the encryption key
	 * @return
	 */
	public static String encrypt(String plaintext, String key) {
		try {
			Cipher desCipher = Cipher.getInstance(AES_ALGORITHM);
			desCipher.init(Cipher.ENCRYPT_MODE, decodeSecretKeyFromString(key));
			byte[] encryptedBytes = desCipher.doFinal(plaintext.getBytes(UTF_8_CHARSET));
			return Base64.encodeBase64URLSafeString(encryptedBytes); 
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}		    
	}

	/**
	 * Base64 decode and decrypt a String encrypted by the encrypt() function.
	 * 
	 * @param encrypted the encrypted text
	 * @param key the encryption key
	 * @return
	 */
	public static String decrypt(String encrypted, String key) {
		try {
			Cipher desCipher = Cipher.getInstance(AES_ALGORITHM);
			desCipher.init(Cipher.DECRYPT_MODE, decodeSecretKeyFromString(key));
			byte[] plaintextBytes = desCipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(plaintextBytes, UTF_8_CHARSET); 
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
