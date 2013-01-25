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
	
	@Test
	public void validateFileMemoryPercentages(){
		StackConfiguration confg = new StackConfiguration();
		double transferPercent = confg.getFileTransferMemoryPercentOfMax();
		double previewPercent = confg.getFilePreivewMemoryPercentOfMax();
		assertTrue("fileTransferMemoryPercentOfMax + filePreivewMemoryPercentOfMax cannot exceed 90%",transferPercent + previewPercent <= 0.9);
	}
	
	@Test
	public void testTransferMemoryAndgTransferBufferSizeBytes(){
		StackConfiguration confg = new StackConfiguration();
		long totalTransferBytes = confg.getMaxFileTransferMemoryPoolBytes();
		long bufferBytes = confg.getFileTransferBufferSizeBytes();
		assertTrue("The maxFileTransferMemoryPoolBytes must be greater than the fileTransferBufferSizeBytes", totalTransferBytes >= bufferBytes);
	}
	
	@Test
	public void testMemoryAllocation(){
		StackConfiguration confg = new StackConfiguration();
		long maxTransferBytes = confg.getMaxFileTransferMemoryPoolBytes();
		System.out.println("Max Transfer bytes: "+maxTransferBytes);
		long maxPreviewByes = confg.getMaxFilePreviewMemoryPoolBytes();
		System.out.println("Max preview bytes: "+maxPreviewByes);
		long maxtBytes = Runtime.getRuntime().maxMemory();
		assertTrue("The maxTransferBytes + maxPreviewByes must be less than or equal to 90% of the max memory",(maxTransferBytes + maxPreviewByes) <= ((long)maxtBytes*0.9d) );
	}

}
