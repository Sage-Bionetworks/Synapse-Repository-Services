package org.sagebionetworks.securitytools;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


/**
 * This class addresses PLFM-192: http://sagebionetworks.jira.com/browse/PLFM-192
 *
 */
public class HMACUtils {
	
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	/**
	 * Returns a BASE64-ENCODED HMAC-SHA1 key
	 */
	public static String newHMACSHA1Key() {
		try {
		    // Generate a key for the HMAC-SHA1 keyed-hashing algorithm
		    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
		    SecretKey key = keyGen.generateKey();
		    return new String(Base64.encodeBase64(key.getEncoded()));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
    /**
     * 
     * @param username
     * @param uri, e.g. /repo/v1/dataset
     * @param date in ISO 8601 format:  yyyy-mm-ddTHH:MM:SS.SSS
     * Encodes data using a given BASE-64 Encoded HMAC-SHA1 secret key, base-64 encoding the result
     */
    public static String generateHMACSHA1Signature(
    		String username,
    		String uri,
    		String date,
    		String base64EncodedSecretKey) {

    	return new String(generateHMACSHA1SignatureFromBase64EncodedKey(username+uri+date, base64EncodedSecretKey));
    }
	   
    /**
     * Encodes data using a given BASE-64 Encoded HMAC-SHA1 secret key, base-64 encoding the result
     */
	public static byte[] generateHMACSHA1SignatureFromBase64EncodedKey(String data, String base64EncodedSecretKey) {
		byte[] secretKey = Base64.decodeBase64(base64EncodedSecretKey.getBytes());
		return generateHMACSHA1SignatureFromRawKey(data, secretKey);
    }
	
	public static byte[] generateHMACSHA1SignatureFromRawKey(String data, byte[] secretKey) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(new SecretKeySpec(secretKey, HMAC_SHA1_ALGORITHM));
			return Base64.encodeBase64(mac.doFinal(data.getBytes("UTF-8")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	
    }
}
