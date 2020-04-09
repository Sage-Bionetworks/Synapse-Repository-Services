package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dockerRegistryAuthFilter")
public class DockerRegistryAuthFilter extends BasicAuthenticationFilter {

	private String dockerRegistryUser;
	private String dockerRegistryPassword;

	@Autowired
	public DockerRegistryAuthFilter(StackConfiguration config, Consumer consumer) {
		super(config, consumer);
		dockerRegistryUser = config.getDockerRegistryUser();
		dockerRegistryPassword = config.getDockerRegistryPassword();
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
		return dockerRegistryUser.equals(credentials.getUserName()) && dockerRegistryPassword.equals(credentials.getPassword());
	}

}
