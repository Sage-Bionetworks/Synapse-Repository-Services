package org.sagebionetworks.web.server;

import org.springframework.web.client.RestTemplate;

/**
 * Simple abstraction for getting a spring RestTemplate. This allows us
 * to use Guice injection to setup the template as needed.
 * 
 * @author John
 *
 */
public interface RestTemplateProvider {
	
	public RestTemplate getTemplate();

}
