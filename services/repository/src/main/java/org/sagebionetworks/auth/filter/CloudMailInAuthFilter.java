package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("cloudMailInAuthFilter")
public class CloudMailInAuthFilter extends BasicAuthenticationFilter {

	private String cloudMailInUser;
	private String cloudMailInPassword;

	@Autowired
	public CloudMailInAuthFilter(StackConfiguration config, Consumer consumer) {
		super(config, consumer);
		cloudMailInUser = config.getCloudMailInUser();
		cloudMailInPassword = config.getCloudMailInPassword();
	}
	
	@Override
	protected boolean credentialsRequired() {
		return true;
	}
	
	@Override
	protected boolean reportBadCredentialsMetric() {
		return true;
	}
	
	@Override
	protected boolean validCredentials(UserNameAndPassword credentials) {
		return cloudMailInUser.equals(credentials.getUserName()) && cloudMailInPassword.equals(credentials.getPassword());
	}

}
