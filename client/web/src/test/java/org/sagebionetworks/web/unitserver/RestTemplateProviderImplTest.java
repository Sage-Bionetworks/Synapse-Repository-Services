package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.junit.Test;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Unit test for RestTemplateProviderImpl.
 * 
 * @author jmhill
 *
 */
public class RestTemplateProviderImplTest {
	
	@Test
	public void testConstructor(){
		// Create a new provide passing in sample configuration paramters
		int timeout = 12345; // 2 seconds
		int maxConnections = 12;
		RestTemplateProviderImpl provider = new RestTemplateProviderImpl(timeout, maxConnections);
		assertNotNull(provider);
		// Make sure we can get a template
		RestTemplate template = provider.getTemplate();
		assertNotNull(template);
		// Make sure the factory has been setup correctly
		ClientHttpRequestFactory factory = template.getRequestFactory();
		assertNotNull(factory);
		assertTrue(factory instanceof CommonsClientHttpRequestFactory);
		CommonsClientHttpRequestFactory commonsFactory = (CommonsClientHttpRequestFactory) factory;
		// Make sure we can get a connection
		HttpClient client = commonsFactory.getHttpClient();
		assertNotNull(client);
		HttpConnectionManager manager = client.getHttpConnectionManager();
		assertNotNull(manager);
		// Make sure it is the correct kind.
		assertTrue(manager instanceof MultiThreadedHttpConnectionManager);
		MultiThreadedHttpConnectionManager multiThreadManager = (MultiThreadedHttpConnectionManager)manager;
		// Make sure it received the expected config
		HttpConnectionManagerParams params = multiThreadManager.getParams();
		assertNotNull(params);
		assertEquals(timeout, params.getConnectionTimeout());
		assertEquals(maxConnections,params.getMaxTotalConnections());
	}

}
