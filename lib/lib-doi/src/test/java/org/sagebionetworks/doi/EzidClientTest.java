package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

public class EzidClientTest {

	@Test
	public void testConstructor() throws Exception {
		DoiClient client = new EzidClient();
		assertNotNull(client);
		RetryableHttpClient retryableClient = (RetryableHttpClient)ReflectionTestUtils.getField(client, "writeClient");
		assertNotNull(retryableClient);
		DefaultHttpClient httpClient = (DefaultHttpClient)ReflectionTestUtils.getField(retryableClient, "httpClient");
		assertNotNull(httpClient);
		assertEquals(Integer.valueOf(9000), httpClient.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
		assertEquals("Synapse", httpClient.getParams().getParameter(CoreProtocolPNames.USER_AGENT));
		AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "EZID", AuthPolicy.BASIC);
		Credentials credentials = httpClient.getCredentialsProvider().getCredentials(authScope);
		assertNotNull(credentials);
		assertEquals(StackConfiguration.getEzidUsername(), credentials.getUserPrincipal().getName());
		assertEquals(StackConfiguration.getEzidPassword(), credentials.getPassword());
		retryableClient = (RetryableHttpClient)ReflectionTestUtils.getField(client, "readClient");
		assertNotNull(retryableClient);
		httpClient = (DefaultHttpClient)ReflectionTestUtils.getField(retryableClient, "httpClient");
		assertNotNull(httpClient);
		assertEquals(Integer.valueOf(9000), httpClient.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
		assertEquals("Synapse", httpClient.getParams().getParameter(CoreProtocolPNames.USER_AGENT));
	}

	@Test
	public void testCreate() throws Exception {
		
	}
}
