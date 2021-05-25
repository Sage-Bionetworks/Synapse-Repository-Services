package org.sagebionetworks;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Run this integration test to ensure CORS filter is applied
 * if a throttled response is returned. See PLFM-5982.
 *
 *
 * @author michael
 *
 */
public class ITSimpleCORSFilterTest {

    private static SimpleHttpClient simpleHttpClient;
    private static SynapseClient synapse;
    private static SynapseAdminClient adminSynapse;
    private static Long userToDelete;
    private static int THROTTLED_REQUEST_COUNT = 1000;

    @BeforeClass
    public static void beforeClass() throws Exception {
        simpleHttpClient = new SimpleHttpClientImpl();
        adminSynapse = new SynapseAdminClientImpl();
        SynapseClientHelper.setEndpoints(adminSynapse);
        adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
        adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
        synapse = new SynapseClientImpl();
        // Some throttles only occur for authenticated users
        userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
    }

    @Before
    public void before() throws SynapseException{
        adminSynapse.clearAllLocks();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        adminSynapse.deleteUser(userToDelete);
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
