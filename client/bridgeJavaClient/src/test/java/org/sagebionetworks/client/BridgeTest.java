package org.sagebionetworks.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Unit test for Bridge.
 * 
 * @author jmhill
 * 
 */
public class BridgeTest {
	
	HttpClientProvider mockProvider = null;
	HttpResponse mockResponse;
	
    BridgeClientImpl bridge;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		bridge = new BridgeClientImpl(mockProvider);
	}
	
	@Test
	public void testUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		BridgeVersionInfo info = new BridgeVersionInfo();
		info.setVersion("someversion");
		when(mockResponse.getEntity()).thenReturn(new StringEntity(EntityFactory.createJSONStringForEntity(info)));
		BridgeStubHttpClientProvider stubProvider = new BridgeStubHttpClientProvider(mockResponse);
        bridge = new BridgeClientImpl(stubProvider);
		// Make a call and ensure 
        bridge.getBridgeVersionInfo();
		// Validate that the User-Agent was sent
		Map<String, String> sentHeaders = stubProvider.getSentRequestHeaders();
		String value = sentHeaders.get("User-Agent");
		assertNotNull(value);
        assertTrue(value.startsWith(BridgeClientImpl.BRIDGE_JAVA_CLIENT));
 	}
	
	@Test
	public void testAppendUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		BridgeVersionInfo info = new BridgeVersionInfo();
		info.setVersion("someversion");
		when(mockResponse.getEntity()).thenReturn(new StringEntity(EntityFactory.createJSONStringForEntity(info)));
		BridgeStubHttpClientProvider stubProvider = new BridgeStubHttpClientProvider(mockResponse);
        bridge = new BridgeClientImpl(stubProvider);
		// Append some user agent data
		String appended = "Appended to the User-Agent";
        bridge.appendUserAgent(appended);
		// Make a call and ensure 
        bridge.getBridgeVersionInfo();
		// Validate that the User-Agent was sent
		Map<String, String> sentHeaders = stubProvider.getSentRequestHeaders();
		String value = sentHeaders.get("User-Agent");
		System.out.println(value);
		assertNotNull(value);
        assertTrue(value.startsWith(BridgeClientImpl.BRIDGE_JAVA_CLIENT));
		assertTrue("Failed to append data to the user agent",value.indexOf(appended) > 0);
 	}
}
