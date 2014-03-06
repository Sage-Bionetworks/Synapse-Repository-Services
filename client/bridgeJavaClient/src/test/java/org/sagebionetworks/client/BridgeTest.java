package org.sagebionetworks.client;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

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
	
	private SharedClientConnection mockSharedClientConnection;
	
    BridgeClientImpl bridge;
	
	@Before
	public void before() throws Exception{
		mockSharedClientConnection = Mockito.mock(SharedClientConnection.class);
		bridge = new BridgeClientImpl(mockSharedClientConnection);
	}
	
	@Test
	public void testUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		BridgeVersionInfo info = new BridgeVersionInfo();
		info.setVersion("someversion");
		// the three param' in the following are endpoint, uri, user agent
		when(mockSharedClientConnection.getJson(anyString(), anyString(), startsWith(BridgeClientImpl.BRIDGE_JAVA_CLIENT))).
			thenReturn(EntityFactory.createJSONObjectForEntity(info));
        bridge = new BridgeClientImpl(mockSharedClientConnection);
		// Make a call and ensure 
        bridge.getBridgeVersionInfo();
        verify(mockSharedClientConnection).getJson(anyString(), anyString(), startsWith(BridgeClientImpl.BRIDGE_JAVA_CLIENT));
 	}
	
	@Test
	public void testAppendUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		BridgeVersionInfo info = new BridgeVersionInfo();
		info.setVersion("someversion");
		// Append some user agent data
		String appended = "Appended to the User-Agent";
        bridge.appendUserAgent(appended);
        String expectedUserAgent = bridge.getUserAgent();
        assertTrue(expectedUserAgent.startsWith(BridgeClientImpl.BRIDGE_JAVA_CLIENT));
        assertTrue(expectedUserAgent.endsWith(appended));
		// the three param' in the following are endpoint, uri, user agent
		when(mockSharedClientConnection.getJson(anyString(), anyString(), eq(expectedUserAgent))).
			thenReturn(EntityFactory.createJSONObjectForEntity(info));
		// Make a call and ensure 
        bridge.getBridgeVersionInfo();
		// Validate that the User-Agent was sent
        verify(mockSharedClientConnection).getJson(anyString(), anyString(), eq(expectedUserAgent));

 	}
}
