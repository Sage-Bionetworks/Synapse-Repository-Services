package org.sagebionetworks.repo.model;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class OriginatingClientTest {

	@Test
	public void synapseReturnsSynapseInMap() {
		Map<String,String> map = OriginatingClient.SYNAPSE.getParameterMap();
		Assert.assertEquals("null returns Synapse client", OriginatingClient.SYNAPSE.name().toLowerCase(), map.get("originClient"));
	}
	@Test
	public void bridgeReturnsBridgeInMap() {
		Map<String,String> map = OriginatingClient.BRIDGE.getParameterMap();
		Assert.assertEquals("Bridge user agent string returns Synapse client", OriginatingClient.BRIDGE.name().toLowerCase(), map.get("originClient"));
	}

}
