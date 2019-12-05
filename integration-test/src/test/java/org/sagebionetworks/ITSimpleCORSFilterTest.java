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
        requestHeaders.put("sessionToken", synapse.getCurrentSessionToken());
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

}
