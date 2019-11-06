package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.http.HttpStatus;

/**
 * Test class created to make API calls that we don't want to extend to the
 * Synapse Java Client.
 */
public class ITUnsupportedMethodTest {

	private static SynapseClient synapse;
	private static SimpleHttpClient simpleHttpClient;

	@BeforeClass
	public static void beforeClass() {
		synapse = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapse);
		simpleHttpClient = new SimpleHttpClientImpl();
	}

	@Test
	public void testUnsupportedMethodWrongVerb() throws Exception {
		// For more info, see PLFM-3574
		SimpleHttpRequest invalidMethodRequest = new SimpleHttpRequest();
		// Create a request that exists, but use the wrong verb (POST /version)
		invalidMethodRequest.setUri(synapse.getRepoEndpoint() + "/version");

		// Call under test
		SimpleHttpResponse response = simpleHttpClient.post(invalidMethodRequest, "body");
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getStatusCode());
	}

	@Test
	public void testUnsupportedMethodCallUnknownUri() throws Exception {
		SimpleHttpRequest invalidMethodRequest = new SimpleHttpRequest();
		// Create a request that doesn't exist with any verb
		invalidMethodRequest.setUri(synapse.getRepoEndpoint() + "nonsenserequest");

		// Call under test
		SimpleHttpResponse response = simpleHttpClient.get(invalidMethodRequest);
		assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
	}
}
