package org.sagebionetworks.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class BridgeProfileProxyTest {

	@Test
	public void testProxy(){
        BridgeClient proxy = BridgeProfileProxy.createProfileProxy(new BridgeClientImpl());
		String token = "123";
		proxy.setSessionToken(token);
		String result = proxy.getCurrentSessionToken();
		assertEquals(token, result);
	}
}
