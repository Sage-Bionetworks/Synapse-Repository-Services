package org.sagebionetworks.web.server;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * The purpose of this class it to setup the RestTemplate singleton in a
 * thread-safe manner. Guice will inject the configuration properties
 * 
 * @see <a href="http://hc.apache.org/httpclient-3.x/threading.html">HttpClient
 *      threading</a>.
 * 
 * @author jmhill
 * 
 */
public class RestTemplateProviderImpl implements RestTemplateProvider {

	RestTemplate tempalteSingleton = null;

	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 */
	@Inject
	public RestTemplateProviderImpl(
			@Named("org.sagebionetworks.rest.template.connection.timout") int connectionTimeout,
			@Named("org.sagebionetworks.rest.template.max.total.connections") int maxTotalConnections) {
		// This connection manager allows us to have multiple thread
		// making http calls.
		// For now use the default values.
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		HttpConnectionManagerParams connectionManagerParams = new HttpConnectionManagerParams();
		connectionManagerParams.setConnectionTimeout(connectionTimeout);
		connectionManagerParams.setMaxTotalConnections(maxTotalConnections);
		connectionManager.setParams(connectionManagerParams);
		HttpClient client = new HttpClient(connectionManager);
		// We can now use this manager to create our rest template
		CommonsClientHttpRequestFactory factory = new CommonsClientHttpRequestFactory(
				client);
		tempalteSingleton = new RestTemplate(factory);
	}

	@Override
	public RestTemplate getTemplate() {
		return tempalteSingleton;
	}

}
