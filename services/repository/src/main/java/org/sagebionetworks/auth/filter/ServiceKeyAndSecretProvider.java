package org.sagebionetworks.auth.filter;

/**
 * Abstraction for key/secret pair provider
 * @author Marco Marasca
 */
public interface ServiceKeyAndSecretProvider {
	
	/**
	 * @return The service name
	 */
	String getServiceName();
	
	/**
	 * @return The service key
	 */
	String getServiceKey();
	
	/**
	 * @return The service secret
	 */
	String getServiceSecret();

}
