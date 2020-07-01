package org.sagebionetworks.auth.filter;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dockerRegistryAuthFilter")
public class DockerRegistryAuthFilter extends BasicAuthServiceFilter {

	@Autowired
	public DockerRegistryAuthFilter(StackConfiguration config, Consumer consumer) {
		super(config, consumer, new StackConfigKeyAndSecretProvider(config, StackConfiguration.SERVICE_DOCKER_REGISTRY));
	}

}
