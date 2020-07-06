package org.sagebionetworks.auth.filter;

/**
 * Abstraction for key/secret pair validation
 * @author Marco Marasca
 */
public interface ServiceKeyAndSecretProvider {
	
	/**
	 * @return The service name
	 */
	String getServiceName();

	/**
	 * @return True if the given key and secret are valid for this provider
	 */
	boolean validate(String key, String secret);

}
