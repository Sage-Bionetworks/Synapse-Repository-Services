package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SynapseProfileProxyTest {

	@Test
	public void testProxy(){
		SynapseClient proxy = SynapseProfileProxy.createProfileProxy(new SynapseClientImpl());
		String token = "123";
		proxy.setSessionToken(token);
		String result = proxy.getCurrentSessionToken();
		assertEquals(token, result);
	}
}
