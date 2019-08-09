package org.sagebionetworks.securitytools;

import java.io.UnsupportedEncodingException;

public interface EncryptionUtils {
	
	/**
	 * Encrypt a string using the stack's encryption key.  Encrypted bytes are Base64 encoded.
	 * Note:  This cannot be used for data that has to persist across stacks/instances.
	 * @param s
	 * @return
	 */
	String encryptStringWithStackKey(String s) throws UnsupportedEncodingException;
	
	/**
	 * Decrypt a String encoded using the stack's encryption key.
	 * Note:  This cannot be used to decrypt data encrypted on another stack/instance.
	 * @param s
	 * @return
	 */
	String decryptStackEncryptedString(String s) throws UnsupportedEncodingException;

}
