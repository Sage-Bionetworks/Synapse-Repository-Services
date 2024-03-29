package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/**
 * Run this integration test to ensure CORS filter is applied
 * if a throttled response is returned. See PLFM-5982.
 *
 *
 * @author michael
 *
 */
@ExtendWith(ITTestExtension.class)
public class ITSimpleCORSFilterTest {

    private static SimpleHttpClient simpleHttpClient;
    private static int THROTTLED_REQUEST_COUNT = 1000;
    
    private SynapseClient synapse;
    
    public ITSimpleCORSFilterTest(SynapseClient synapse) {
    	this.synapse = synapse;
	}

    @BeforeAll
    public static void beforeClass() throws Exception {
        simpleHttpClient = new SimpleHttpClientImpl();
    }

    @Test
    public void testResponseHasCORSOnThrottle() throws Exception {
        StringBuilder urlBuilder = new StringBuilder(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
        urlBuilder.append("/version");
        SimpleHttpRequest request = new SimpleHttpRequest();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer "+synapse.getAccessToken());
        request.setHeaders(requestHeaders);
        request.setUri(urlBuilder.toString());

        // Make enough calls to trigger throttled response
        SimpleHttpResponse response = null;
        for(int i = 0; i < THROTTLED_REQUEST_COUNT; i++) {
            response = simpleHttpClient.get(request);
        }

        assertNotNull(response);
        assertEquals(429, response.getStatusCode());
        assertNotNull(response.getFirstHeader("access-control-allow-origin"));
        assertEquals("*", response.getFirstHeader("access-control-allow-origin").getValue());
    }
    
    // make sure the CORS request does not check authorization (PLFM-6756)
    @Test
    public void testNotAuthorizationOnCORSRequest() throws Exception {
        StringBuilder urlBuilder = new StringBuilder(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
        urlBuilder.append("/version");
        SimpleHttpRequest request = new SimpleHttpRequest();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer not-a-valid-access-token");
        // the CORS filter requires this header to recognize the request as a preflight request
        requestHeaders.put("Access-Control-Request-Method", "GET"); 
        
        request.setHeaders(requestHeaders);
        request.setUri(urlBuilder.toString());
    	
        SimpleHttpResponse response = simpleHttpClient.options(request);
        
        // Previously we would return a 401 response.
        // We now return a 200 response with the expected CORS/preflight response headers
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getFirstHeader("access-control-allow-origin"));
        assertNotNull(response.getFirstHeader("Access-Control-Allow-Methods"));
        
    }

}
