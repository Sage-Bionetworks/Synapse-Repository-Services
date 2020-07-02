package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.stereotype.Component;

/**
 * Filter used for administrative services
 * 
 * @author Marco Marasca
 *
 */
@Component("adminServiceAuthFilter")
public class AdminServiceAuthFilter extends BasicAuthServiceFilter {

	public AdminServiceAuthFilter(StackConfiguration config, Consumer consumer) {
		super(config, consumer, new StackConfigKeyAndSecretProvider(config, StackConfiguration.SERVICE_ADMIN));
	}
	
	// Injects the admin user automatically
	@Override
	protected boolean isAdminService() {
		return true;
	}

}
