package org.sagebionetworks.client;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class BridgeClientTest {

	private static final String URIBASE = "https://bridge-prod.prod.sagebase.org/bridge/v1";

	BridgeClient bridgeClient;
	@Mock
	SharedClientConnection mockSharedClientConnection;
	
	@Before
	public void doBefore() throws Exception {
		MockitoAnnotations.initMocks(this);
		bridgeClient = new BridgeClientImpl(mockSharedClientConnection);
	}

	@After
	public void doAfter() {
		verifyNoMoreInteractions(mockSharedClientConnection);
	}

	@Test
	public void testGetVersionInfo() throws Exception {
		BridgeVersionInfo version = new BridgeVersionInfo();
		version.setVersion("none");

		// You cannot set domain to bridge here, because getVersionInfo() has no version
		// that takes OriginatingClient (there's no difference in the call between clients). 
		// The underlying libraries always set this to synapse (and it's ignored).
		
		expectRequest(URIBASE, "/version", EntityFactory.createJSONObjectForEntity(version));

		bridgeClient.getBridgeVersionInfo();

		verifyRequest(URIBASE,  "/version", EntityFactory.createJSONObjectForEntity(version));
	}

	private void expectRequest(String endpoint, String uri, JSONObject answer) throws Exception {
		when(mockSharedClientConnection.getJson(eq(endpoint), eq(uri), anyString())).thenReturn(answer);
	}

	private void verifyRequest(String endpoint, String uri, JSONObject answer) throws Exception {
		verify(mockSharedClientConnection).getJson(eq(endpoint), eq(uri), anyString());
	}
}
