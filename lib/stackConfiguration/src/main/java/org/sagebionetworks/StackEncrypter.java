package org.sagebionetworks;

public interface StackEncrypter {
	
	/**
	 * Encrypt a string using the stack's encryption key.  Encrypted bytes are Base64 encoded.
	 * 
	 * The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 * order for the returned value to be encrypted. If the property is not
	 * set, the Base64 encoded value of the input will be returned
	 * 
	 * @param s
	 * @return
	 */
	String encryptAndBase64EncodeStringWithStackKey(String s);
	
	/**
	 * Decrypt a String encoded using the stack's encryption key.
	 * 
	 * The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 * order for the decrypted value to be returned. If the property is not
	 * set, the Base64 decoded value of the input will be returned
	 * 
	 * @param s
	 * @return
	 */
	String decryptStackEncryptedAndBase64EncodedString(String s);

	/**
	 * Reencrypt a String, Base64 decoding the content before reencryption and Base64 encoding the result
	 * 
	 * The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 * order for the value to be reencrypted.  If the property is not set
	 * then the input is simply returned.
	 * 
	 * @param encryptedValueBase64
	 * @return
	 */
	String reEncryptStackEncryptedAndBase64EncodedString(String encryptedValueBase64);

	/**
	 * Get the decrypted (plaintext) value for a given property key.
	 * 
	 * The property 'org.sagebionetworks.stack.cmk.alias' must be set in
	 * order for the decrypted value to be returned. If the property is not
	 * set, the Base64 encoded value of the property will be returned
	 * 
	 * @param propertyKey
	 * @return The decrypted property value
	 */
	public String getDecryptedProperty(String propertyKey);

}
