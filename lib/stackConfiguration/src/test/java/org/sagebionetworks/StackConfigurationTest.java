package org.sagebionetworks;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

public class StackConfigurationTest {

	@Test
	public void testGetDefaultCrowdEndpoint() {
		assertEquals("https://crowd-dev.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
		URL testPropertiesLocation = StackConfiguration.class.getResource("/someBrandNewStack.properties"); 
		System.setProperty(StackConfiguration.STACK_PROPERTY_FILE_URL, testPropertiesLocation.toString());
		StackConfiguration.reloadStackConfiguration();
		assertEquals("someBrandNewStack", StackConfiguration.getStack());
		assertEquals("https://crowd.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
	}


}
