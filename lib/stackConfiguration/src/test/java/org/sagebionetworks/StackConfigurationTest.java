package org.sagebionetworks;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

public class StackConfigurationTest {

	@Test
	public void testGetDefaultCrowdEndpoint() {
		assertEquals("https://dev-crowd.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
		URL testPropertiesLocation = StackConfiguration.class.getResource("/someBrandNewStack.properties"); 
		System.setProperty(StackConstants.STACK_PROPERTY_FILE_URL, testPropertiesLocation.toString());
		System.setProperty(StackConstants.STACK_PROPERTY_NAME, "some");
		System.setProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME, "Brand");
		StackConfiguration.reloadStackConfiguration();
		assertEquals("some", StackConfiguration.getStack());
		assertEquals("https://crowd.sagebase.org:8443", StackConfiguration
				.getCrowdEndpoint());
	}


}
