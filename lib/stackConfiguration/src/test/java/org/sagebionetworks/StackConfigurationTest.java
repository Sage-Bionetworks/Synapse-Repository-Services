package org.sagebionetworks;

import static org.junit.Assert.*;

import org.junit.Test;

public class StackConfigurationTest {

	@Test
	public void testGetDefaultCrowdEndpoint() {
		System.setProperty("org.sagebionetworks.stack", "someBrandNewStack");
		StackConfiguration.reloadStackConfiguration();
		assertEquals("https://crowd-dev.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
	}

	@Test
	public void testGetAlphaCrowdEndpoint() {
		System.setProperty("org.sagebionetworks.stack", "alpha");
		StackConfiguration.reloadStackConfiguration();
		assertEquals("https://crowd.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
	}

}
