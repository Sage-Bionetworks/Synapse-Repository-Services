package org.sagebionetworks.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class SynapseProfileProxyTest {

	@Test
	public void testProxy(){
		SynapseInt proxy = SynapseProfileProxy.createProfileProxy(new Synapse());
		String token = "123";
		proxy.setSessionToken(token);
		String result = proxy.getCurrentSessionToken();
		assertEquals(token, result);
	}
}
