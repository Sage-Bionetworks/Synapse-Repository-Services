package org.sagebionetworks.securitytools;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;

public class PBKDF2Utils {
	private static final String HASH_PREFIX = "{PKCS5S2}";
	private static final String HASHING_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int HASHING_ITERATIONS = 10000;
	private static final int HASHING_SALTLENGTH = 16; // bytes
	private static final int HASHING_KEYLENGTH = 32; // bytes
	private static final int PBEKEYSPEC_KEYLENGTH = HASHING_KEYLENGTH * 8; // bits
	
	private static final Random RANDOM = new SecureRandom();

	/**
	 * Returns the salt used to generate the password hash
	 * @param passHash Must be exactly 73 characters long (9 for the prefix, 64 for the salt and checksum)
	 */
	public static byte[] extractSalt(String passHash) {
		if (passHash == null 
				|| passHash.length() != (HASH_PREFIX.length() + (HASHING_SALTLENGTH + HASHING_KEYLENGTH) * 4 / 3)
				|| !passHash.startsWith(HASH_PREFIX)) {
			throw new IllegalArgumentException("Password hash format is unrecognized"); 
		}
		String hashBody = passHash.substring(HASH_PREFIX.length());
		byte[] hashBytes = Base64.decodeBase64(hashBody.getBytes());
		return Arrays.copyOfRange(hashBytes, 0, HASHING_SALTLENGTH);
	}
	
	/**
	 * Hashes the password with the given salt
	 * @param salt If null, a random salt is generated
	 * @return A string with LDAP password format {PREFIX}[data]
	 *   where [data] is a 64 character, base64 encoded string containing a salt and password checksum 
	 */
	public static String hashPassword(String password, byte[] salt) {
		if (password == null) {
			// Note: the hashing scheme allows null passwords
			throw new IllegalArgumentException("Password may not be null");
		}
		if (salt == null) {
			SecureRandom rand = new SecureRandom();
			salt = new byte[HASHING_SALTLENGTH];
			rand.nextBytes(salt);
		}
		if (salt.length != HASHING_SALTLENGTH) {
			throw new IllegalArgumentException("Salt must be 16 bytes");
		}
		
		SecretKey hash;
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(HASHING_ALGORITHM);
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASHING_ITERATIONS, PBEKEYSPEC_KEYLENGTH);
			hash = factory.generateSecret(spec);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		
		// Append the password hash to the salt
		byte[] checksum = hash.getEncoded();
		byte[] mashup = new byte[HASHING_SALTLENGTH + HASHING_KEYLENGTH];
		for (int i = 0; i < HASHING_SALTLENGTH; i++) {
			mashup[i] = salt[i];
		}
		for (int i = 0; i < HASHING_KEYLENGTH; i++) {
			mashup[HASHING_SALTLENGTH + i] = checksum[i];
		}
		
		// Convert to a string and add the prefix
		return HASH_PREFIX + new String(Base64.encodeBase64(mashup));
	}

	/**
	 * Generates a 256-bit Base64 encoded string using {@link SecureRandom}
	 * @return
	 */
	public static String generateSecureRandomString() {
		byte[] randomBytes = new byte[32];
		RANDOM.nextBytes(randomBytes);
		return Base64.encodeBase64URLSafeString(randomBytes);
	}
}
