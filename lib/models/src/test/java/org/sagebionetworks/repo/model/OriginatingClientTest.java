package org.sagebionetworks.repo.model;

import org.junit.Assert;
import org.junit.Test;

public class OriginatingClientTest {

	@Test
	public void nullUserAgentReturnsSynapseClient() {
		OriginatingClient client = OriginatingClient.getClientFromOriginClientParam(null);
		Assert.assertEquals("null returns Synapse client", OriginatingClient.SYNAPSE, client);
	}
	@Test
	public void emptyStringUserAgentReturnsSynapseClient() {
		OriginatingClient client = OriginatingClient.getClientFromOriginClientParam("");
		Assert.assertEquals("empty string returns Synapse client", OriginatingClient.SYNAPSE, client);
	}
	@Test
	public void synapseUserAgentReturnsSynapseClient() {
		OriginatingClient client = OriginatingClient.getClientFromOriginClientParam("synapse"); // or anything but bridge
		Assert.assertEquals("non-Bridge strings returns Synapse client", OriginatingClient.SYNAPSE, client);
	}
	@Test
	public void bridgeUserAgentReturnsSynapseClient() {
		OriginatingClient client = OriginatingClient.getClientFromOriginClientParam("Bridge"); // or bridge
		Assert.assertEquals("Bridge user agent string returns Synapse client", OriginatingClient.BRIDGE, client);
	}

}
