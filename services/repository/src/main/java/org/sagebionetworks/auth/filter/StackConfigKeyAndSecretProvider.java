package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Default implementation for a {@link ServiceKeyAndSecretProvider} that grabs
 * the key/secret pair from the stack configuration and caches the hashed secrets
 * 
 * @author Marco Marasca
 *
 */
public class StackConfigKeyAndSecretProvider implements ServiceKeyAndSecretProvider {

	private String serviceName;
	private String serviceKeyHash;
	private String serviceSecretHash;

	public StackConfigKeyAndSecretProvider(StackConfiguration config, String serviceName) {
		this.serviceName = serviceName;
		this.initServiceKeyAndSecretHash(config);
	}
	
	private void initServiceKeyAndSecretHash(StackConfiguration config) {
		this.serviceKeyHash = hashSecret(config.getServiceAuthKey(serviceName), null);
		this.serviceSecretHash = hashSecret(config.getServiceAuthSecret(serviceName), null);	
	}
	
	@Override
	public String getServiceName() {
		return serviceName;
	}
	
	@Override
	public boolean validate(String key, String secret) {
		ValidateArgument.requiredNotBlank(key, "key");
		ValidateArgument.requiredNotBlank(secret, "secret");
		
		String keyHash = hashSecret(key, serviceKeyHash);
		String secretHash = hashSecret(secret, serviceSecretHash);
		
		return serviceKeyHash.equals(keyHash) && serviceSecretHash.equals(secretHash);
	}

	// For testing
	
	String getServiceKeyHash() {
		return serviceKeyHash;
	}
	
	String getServiceSecretHash() {
		return serviceSecretHash;
	}
	
	static String hashSecret(String secret, String saltContainer) {
		byte[] salt = null;
		
		if (saltContainer != null) {
			salt = PBKDF2Utils.extractSalt(saltContainer);
		}
		
		return PBKDF2Utils.hashPassword(secret, salt);
	}

}
