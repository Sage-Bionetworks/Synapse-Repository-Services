package org.sagebionetworks;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.net.URL;

import org.junit.Test;

import java.util.List;

public class StackConfigurationTest {
	
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
	
	@Test
	public void testIsDevelop(){
		assertFalse(StackConfiguration.isDevelopStack("prod"));
		assertTrue(StackConfiguration.isDevelopStack("dev"));
	}
	
	@Test
	public void testIsHudson(){
		assertFalse(StackConfiguration.isHudsonStack("prod"));
		assertFalse(StackConfiguration.isHudsonStack("dev"));
		assertTrue(StackConfiguration.isHudsonStack("hud"));
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
	
	@Test
	public void testDatabaseInTablesCluster(){
		StackConfiguration confg = new StackConfiguration();
		int count = confg.getTablesDatabaseCount();
		assertTrue(count > 0);
		// We should find a schema and endpoint for each database
		for(int i=0; i<count; i++){
			String endpoint = confg.getTablesDatabaseEndpointForIndex(i);
			assertNotNull(endpoint);
			String schema = confg.getTablesDatabaseSchemaForIndex(i);
			assertNotNull(schema);
			System.out.println("Endpoint: "+endpoint+" schema: "+schema);
		}
	}
	
	@Test
	public void testGetDockerRegistryHosts() {
		StackConfiguration cfg = new StackConfiguration();
		List<String> regHosts = cfg.getDockerRegistryHosts();
		for (String h: regHosts) {
			assertEquals(h.trim(), h);
		}
	}
	
	@Test
	public void testGetDockerReservedRegistryHosts() {
		StackConfiguration cfg = new StackConfiguration();
		List<String> regHosts = cfg.getDockerReservedRegistryHosts();
		for (String h: regHosts) {
			assertEquals(h.trim(), h);
		}
	}
}
