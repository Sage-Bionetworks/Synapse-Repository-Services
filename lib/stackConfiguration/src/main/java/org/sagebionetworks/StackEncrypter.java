package org.sagebionetworks;

public interface StackEncrypter {
	
	/**
	 * Encrypt a string using the stack's encryption key.  Encrypted bytes are Base64 encoded.
	 * Note:  This cannot be used for data that has to persist across stacks/instances.
	 * @param s
	 * @return
	 */
	String encryptStringWithStackKey(String s);
	
	/**
	 * Decrypt a String encoded using the stack's encryption key.
	 * Note:  This cannot be used to decrypt data encrypted on another stack/instance.
	 * @param s
	 * @return
	 */
	String decryptStackEncryptedString(String s);

	/**
	 * Get the decrypted (plaintext) value for a given property key.
	 * 
	 * @param propertyKey
	 * @return The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 *         order for the decrypted value to be returned. If the property is not
	 *         set, the unencrypted value of the property will be returned
	 */
	public String getDecryptedProperty(String propertyKey);

}
