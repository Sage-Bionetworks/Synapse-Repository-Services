package org.sagebionetworks.repo.manager;

import org.sagebionetworks.StackConfiguration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class CloudMailInAuthenticationManager implements AuthenticationManager {
	
	private String cloudMailInUser;
	private String cloudMailInPassword;
	
	public CloudMailInAuthenticationManager() {
		cloudMailInUser = StackConfiguration.getCloudMailInUser();
		cloudMailInPassword = StackConfiguration.getCloudMailInPassword();
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
			authentication.setAuthenticated(authentication.getName().equals(cloudMailInUser) && 
					authentication.getCredentials().equals(cloudMailInPassword));
			return authentication;
	}

}
