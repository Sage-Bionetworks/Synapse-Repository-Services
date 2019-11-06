package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Non-mocked stack configuration test.
 *
 */
public class StackConfigurationTest {
	
	StackConfigurationImpl config;
	
	@Before
	public void before() {
		this.config = (StackConfigurationImpl) StackConfigurationSingleton.singleton();
	}
	
	@Test
	public void validateFileMemoryPercentages(){
		double transferPercent = config.getFileTransferMemoryPercentOfMax();
		double previewPercent = config.getFilePreivewMemoryPercentOfMax();
		assertTrue("fileTransferMemoryPercentOfMax + filePreivewMemoryPercentOfMax cannot exceed 90%",transferPercent + previewPercent <= 0.9);
	}
	
	@Test
	public void testTransferMemoryAndgTransferBufferSizeBytes(){
		long totalTransferBytes = config.getMaxFileTransferMemoryPoolBytes();
		long bufferBytes = config.getFileTransferBufferSizeBytes();
		assertTrue("The maxFileTransferMemoryPoolBytes must be greater than the fileTransferBufferSizeBytes", totalTransferBytes >= bufferBytes);
	}
	
	@Test
	public void testMemoryAllocation(){
		long maxTransferBytes = config.getMaxFileTransferMemoryPoolBytes();
		System.out.println("Max Transfer bytes: "+maxTransferBytes);
		long maxPreviewByes = config.getMaxFilePreviewMemoryPoolBytes();
		System.out.println("Max preview bytes: "+maxPreviewByes);
		long maxtBytes = Runtime.getRuntime().maxMemory();
		assertTrue("The maxTransferBytes + maxPreviewByes must be less than or equal to 90% of the max memory",(maxTransferBytes + maxPreviewByes) <= ((long)maxtBytes*0.9d) );
	}

	@Test
	public void testBootstrapData() {
		assertEquals("/root", config.getRootFolderEntityPath());
		assertEquals("4489", config.getRootFolderEntityId());
		assertEquals("/root/trash", config.getTrashFolderEntityPath());
		assertEquals("1681355", config.getTrashFolderEntityId());
		assertEquals("/root", config.getRootFolderEntityPath());
		assertEquals("4489", config.getRootFolderEntityId());
		assertEquals("/root/trash", config.getTrashFolderEntityPath());
		assertEquals("1681355", config.getTrashFolderEntityId());
	}
	
	@Test
	public void testIsProd(){
		assertFalse("Tests are never run against the prod stack!!!!!",config.isProductionStack());
		assertTrue(config.isProduction("prod"));
		assertFalse(config.isProduction("dev"));
	}
	
	@Test
	public void testIsDevelop(){
		assertFalse(config.isDevelopStack("prod"));
		assertTrue(config.isDevelopStack("dev"));
	}
	
	@Test
	public void testIsHudson(){
		assertFalse(config.isHudsonStack("prod"));
		assertFalse(config.isHudsonStack("dev"));
		assertTrue(config.isHudsonStack("hud"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetStackInstanceNumberProdNotNumeric(){
		// Prod stacks must have a numeric instance
		config.getStackInstanceNumber("abc", true);
	}
	
	@Test
	public void testGetStackInstanceNumberProd(){
		// Prod stacks must have a numeric instance
		assertEquals(12, config.getStackInstanceNumber("12", true));
	}
	
	@Test
	public void testGetStackInstanceNumberDev(){
		// Dave stacks have names as stack-instance values. 
		//These name must be converted to their numeric representation (base 256 chars string to base 10)
		assertEquals(new BigInteger("hoff".getBytes()).intValue(), config.getStackInstanceNumber("hoff", false));
		assertEquals(new BigInteger("hill".getBytes()).intValue(), config.getStackInstanceNumber("hill", false));
		assertEquals(new BigInteger("wu".getBytes()).intValue(), config.getStackInstanceNumber("wu", false));
	}
	
	@Test
	public void testDatabaseInTablesCluster(){
		int count = config.getTablesDatabaseCount();
		assertTrue(count > 0);
		// We should find a schema and endpoint for each database
		for(int i=0; i<count; i++){
			String endpoint = config.getTablesDatabaseEndpointForIndex(i);
			assertNotNull(endpoint);
			String schema = config.getTablesDatabaseSchemaForIndex(i);
			assertNotNull(schema);
			System.out.println("Endpoint: "+endpoint+" schema: "+schema);
		}
	}
	
	@Test
	public void testGetDockerRegistryHosts() {
		List<String> regHosts = config.getDockerRegistryHosts();
		for (String h: regHosts) {
			assertEquals(h.trim(), h);
		}
	}
	
	@Test
	public void testGetDockerReservedRegistryHosts() {
		List<String> regHosts = config.getDockerReservedRegistryHosts();
		for (String h: regHosts) {
			assertEquals(h.trim(), h);
		}
	}
}
