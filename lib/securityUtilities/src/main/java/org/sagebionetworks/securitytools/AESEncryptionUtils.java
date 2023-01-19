package org.sagebionetworks.securitytools;


import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class AESEncryptionUtils {
	
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
	private static final String AES_ALG = "AES";
	private static final String AES_GCM_NOPADDING_ALG =  "AES/GCM/NoPadding";
	private static final String PBKDF2_SHA256_ALG = "PBKDF2WithHmacSHA256";
	
	private static final int GCM_AUTH_TAG_LENGTH = 128;
	private static final int GCM_IV_BYTES_COUNT = 12;
	
	private static final int DEFAULT_KEY_LENGHT = 128;
	private static final int DERIVED_KEY_LENGHT = 256;
	
	private static final int PBKDF2_IT_COUNT = 65536;
	
	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

	private static String encodeSecretKeyAsString(Key key) {
	    return Base64.encodeBase64URLSafeString(key.getEncoded());
	}
	
	private static SecretKey decodeSecretKeyFromString(String s) throws InvalidKeyException {
		byte[] bytes = Base64.decodeBase64(s);
		return new SecretKeySpec(bytes, 0, bytes.length, AES_ALG); 
	}

	/**
	 * Create an encryption key (AES, 128bit) for symmetric encryption of sensitive data to be sent to the client.
	 * Note:  The use case is different than that of the stack encyrption key as the encryption
	 * must be consistent for an indefinite period.  The key is not meant to be shared with the client.
	 * 
	 * @return
	 */
	public static String newSecretKey() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALG);
			keyGen.init(DEFAULT_KEY_LENGHT, SECURE_RANDOM);
			SecretKey key = keyGen.generateKey();
		    return encodeSecretKeyAsString(key);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param password
	 * @param salt
	 * @return A new secret key (AES, 256bit) for symmetric encryption derived from the given password and salt using PBKDF2WithHmacSHA256.
	 */
	public static String newSecretKeyFromPassword(String password, String salt) {
		try {
		    SecretKeyFactory secretFactory = SecretKeyFactory.getInstance(PBKDF2_SHA256_ALG);
		    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(UTF_8_CHARSET), PBKDF2_IT_COUNT, DERIVED_KEY_LENGHT);
		    SecretKey secret = new SecretKeySpec(secretFactory.generateSecret(spec).getEncoded(), AES_ALG);
		    return encodeSecretKeyAsString(secret);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Encrypts the given plain text string with the given key, using AES/GCM/NoPadding (No padding is required for GCM). 
	 * 
	 * A random 12 byte IV is used that is prepended to the cipher text before base64 encoding.
	 * 
	 * This method can be used to transmit data to clients.
	 * 
	 * @param plaintext The plain text to encrypt
	 * @param key The encryption key
	 * @return A base64 encoded encrypted version of the given plain text
	 */
	public static String encryptWithAESGCM(String plaintext, String key) {
		try {
			Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING_ALG);
			
			byte[] iv = new byte[GCM_IV_BYTES_COUNT];
			SECURE_RANDOM.nextBytes(iv);
			
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
			
			cipher.init(Cipher.ENCRYPT_MODE, decodeSecretKeyFromString(key), parameterSpec);
			
			byte[] cipherText = cipher.doFinal(plaintext.getBytes(UTF_8_CHARSET));
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
			
			// The IV vector is not a secret
			byteBuffer.put(iv);
			byteBuffer.put(cipherText);

			byte[] cipherMessage = byteBuffer.array();
			
			return Base64.encodeBase64URLSafeString(cipherMessage);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}	
	}
	
	/**
	 * Decrypts the given encrypted string (assumed base64 encoded) with the given key, using AES/GCM/NoPadding (No padding is required for GCM). 
	 * 
	 * The first 12 byte should contain the IV vector.
	 * 
	 * @param encrypted Base64 encoded IV + cipher text
	 * @param key Encryption key
	 * @return The decrypted text using the given key. Assumed the input to be base64 encoded and prepending a random 12 byte IV.
	 */
	public static String decryptWithAESGCM(String encrypted, String key) {
		try {
			byte[] cipherMessage = Base64.decodeBase64(encrypted);
			
			Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING_ALG);
			// Uses the first 12 bytes for iv
			AlgorithmParameterSpec gcmIv = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, cipherMessage, 0, GCM_IV_BYTES_COUNT);
			cipher.init(Cipher.DECRYPT_MODE, decodeSecretKeyFromString(key), gcmIv);
			// Use everything from 12 bytes on as the cipher text
			byte[] plainText = cipher.doFinal(cipherMessage, GCM_IV_BYTES_COUNT, cipherMessage.length - GCM_IV_BYTES_COUNT);
			
			return new String(plainText, UTF_8_CHARSET);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
		
	/**
	 * Encrypt and base 64 encode a plain text string. Note that it uses a deterministic algorithm that always produces the same output for the same input (no IV is used).
	 * 
	 * @deprecated Does not specify mode or padding, and will use the provider defaults. From the docs
	 *             (https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#Cipher):
	 *             "It is recommended to use a transformation that fully specifies the algorithm, mode,
	 *             and padding. By not doing so, the provider will use a default. For example, the
	 *             SunJCE and SunPKCS11 providers use ECB as the default mode, and PKCS5Padding as the
	 *             default padding for many symmetric ciphers."
	 * @param plaintext the text to encrypt
	 * @param key       the encryption key
	 * @return
	 */
	@Deprecated
	public static String encryptDeterministic(String plaintext, String key) {
		try {
			Cipher desCipher = Cipher.getInstance(AES_ALG);
			desCipher.init(Cipher.ENCRYPT_MODE, decodeSecretKeyFromString(key));
			byte[] encryptedBytes = desCipher.doFinal(plaintext.getBytes(UTF_8_CHARSET));
			return Base64.encodeBase64URLSafeString(encryptedBytes); 
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}		    
	}

	/**
	 * Base64 decode and decrypt a String encrypted by the encryptDeterministic() function. Note that no IV is used.
	 * 
	 * @deprecated Does not specify mode or padding, and will use the provider defaults. From the docs
	 *             (https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#Cipher):
	 *             "It is recommended to use a transformation that fully specifies the algorithm, mode,
	 *             and padding. By not doing so, the provider will use a default. For example, the
	 *             SunJCE and SunPKCS11 providers use ECB as the default mode, and PKCS5Padding as the
	 *             default padding for many symmetric ciphers."
	 * @param encrypted the encrypted text
	 * @param key       the encryption key
	 * @return
	 */
	@Deprecated
	public static String decryptDeterministic(String encrypted, String key) {
		try {
			Cipher desCipher = Cipher.getInstance(AES_ALG);
			desCipher.init(Cipher.DECRYPT_MODE, decodeSecretKeyFromString(key));
			byte[] plaintextBytes = desCipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(plaintextBytes, UTF_8_CHARSET); 
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
