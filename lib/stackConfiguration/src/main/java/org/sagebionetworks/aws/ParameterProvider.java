package org.sagebionetworks.aws;

/**
 * Abstraction for a Parameter Provider.
 *
 */
public interface ParameterProvider {
	
	/**
	 * Get the decrypted value of the parameter for the provided key.
	 * 
	 * @param key
	 * @return
	 */
	public String getDecryptedValue(String key);
	
	/**
	 * Get the parameter value for the provided key.
	 * @param key
	 * @return
	 */
	public String getValue(String key);

}
