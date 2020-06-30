package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;

/**
 * Default implementation for a {@link ServiceKeyAndSecretProvider} that grabs
 * the key/secret pair from the stack configuration
 * 
 * @author Marco Marasca
 *
 */
public class StackConfigKeyAndSecretProvider implements ServiceKeyAndSecretProvider {

	private String serviceName;
	private String serviceKey;
	private String serviceSecret;

	public StackConfigKeyAndSecretProvider(StackConfiguration config, String serviceName) {
		this.serviceName = serviceName;
		this.serviceKey = config.getServiceAuthKey(serviceName);
		this.serviceSecret = config.getServiceAuthSecret(serviceName);
	}
	
	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String getServiceKey() {
		return serviceKey;
	}

	@Override
	public String getServiceSecret() {
		return serviceSecret;
	}

}
