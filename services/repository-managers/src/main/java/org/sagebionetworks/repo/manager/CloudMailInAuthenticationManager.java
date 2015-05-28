package org.sagebionetworks.repo.manager;

import org.sagebionetworks.StackConfiguration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
		if (!authentication.getName().equals(cloudMailInUser) || 
				!authentication.getCredentials().equals(cloudMailInPassword)) {
			throw new BadCredentialsException("The user name or password is incorrect.");
		}
		authentication.setAuthenticated(true);
			return authentication;
	}

}
