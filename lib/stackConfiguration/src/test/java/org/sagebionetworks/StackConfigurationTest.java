package org.sagebionetworks;

import static org.junit.Assert.*;

import java.math.BigInteger;
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

	@Test
	public void testBootstrapData() {
		assertEquals("/root", StackConfiguration.getRootFolderEntityPathStatic());
		assertEquals("4489", StackConfiguration.getRootFolderEntityIdStatic());
		assertEquals("/root/trash", StackConfiguration.getTrashFolderEntityPathStatic());
		assertEquals("1681355", StackConfiguration.getTrashFolderEntityIdStatic());
		StackConfiguration config = new StackConfiguration();
		assertEquals("/root", config.getRootFolderEntityPath());
		assertEquals("4489", config.getRootFolderEntityId());
		assertEquals("/root/trash", config.getTrashFolderEntityPath());
		assertEquals("1681355", config.getTrashFolderEntityId());
	}
	
	@Test
	public void testIsProd(){
		assertFalse("Tests are never run against the prod stack!!!!!",StackConfiguration.isProductionStack());
		assertTrue(StackConfiguration.isProduction("prod"));
		assertFalse(StackConfiguration.isProduction("dev"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetStackInstanceNumberProdNotNumeric(){
		// Prod stacks must have a numeric instance
		StackConfiguration.getStackInstanceNumber("abc", true);
	}
	
	@Test
	public void testGetStackInstanceNumberProd(){
		// Prod stacks must have a numeric instance
		assertEquals(12, StackConfiguration.getStackInstanceNumber("12", true));
	}
	
	@Test
	public void testGetStackInstanceNumberDev(){
		// Dave stacks have names as stack-instance values. 
		//These name must be converted to their numeric representation (base 256 chars string to base 10)
		assertEquals(new BigInteger("hoff".getBytes()).intValue(), StackConfiguration.getStackInstanceNumber("hoff", false));
		assertEquals(new BigInteger("hill".getBytes()).intValue(), StackConfiguration.getStackInstanceNumber("hill", false));
		assertEquals(new BigInteger("wu".getBytes()).intValue(), StackConfiguration.getStackInstanceNumber("wu", false));
	}
	
	
}
