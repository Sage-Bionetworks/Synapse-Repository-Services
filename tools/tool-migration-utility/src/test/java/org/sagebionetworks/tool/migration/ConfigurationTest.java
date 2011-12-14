package org.sagebionetworks.tool.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationTest {
	
	@BeforeClass
	public static void before() throws IOException{
		// Load the test config file
		String file = ConfigurationTest.class.getClassLoader().getResource("configuation-test.properties").getFile().replaceAll("%20", " ");
		Configuration.loadConfigurationFile(file);
	}

	
	@Test
	public void testConfiguration() throws IOException{

		// Test the source
		SynapseConnectionInfo info = Configuration.getSourceConnectionInfo();
		assertNotNull(info);
		assertEquals("sourceAuth", info.getAuthenticationEndPoint());
		assertEquals("sourceRepo", info.getRepositoryEndPoint());
		assertEquals("sourceUser", info.getAdminUsername());
		assertEquals("sourcePassword", info.getAdminPassword());
		
		// Test the destination
		info = Configuration.getDestinationConnectionInfo();
		assertNotNull(info);
		assertEquals("destAuth", info.getAuthenticationEndPoint());
		assertEquals("destRepo", info.getRepositoryEndPoint());
		assertEquals("destUser", info.getAdminUsername());
		assertEquals("destPassword", info.getAdminPassword());
	}
	
	@Test
	public void testMaxThreads(){
		assertEquals(10, Configuration.getMaximumNumberThreads());
	}
	
	@Test
	public void testMaxBatchSize(){
		assertEquals(100, Configuration.getMaximumBatchSize());
	}
	
	@Test
	public void testWorkerTimeout(){
		assertEquals(30*1000, Configuration.getWorkerTimeoutMs());
	}
}
