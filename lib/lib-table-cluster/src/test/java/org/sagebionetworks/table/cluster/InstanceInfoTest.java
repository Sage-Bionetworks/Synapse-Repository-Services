package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstanceInfoTest {

	@Test
	public void testCreate(){
		boolean useSSL = false;
		InstanceInfo info = new InstanceInfo("endpoint", "schema", useSSL);
		assertEquals("endpoint", info.getEndpoint());
		assertEquals("schema", info.getSchema());
		assertEquals("jdbc:mysql://endpoint/schema?rewriteBatchedStatements=true", info.getUrl());
	}
	
	@Test
	public void testCreateWithSSL(){
		boolean useSSL = true;
		InstanceInfo info = new InstanceInfo("endpoint", "schema", useSSL);
		assertEquals("endpoint", info.getEndpoint());
		assertEquals("schema", info.getSchema());
		assertEquals("jdbc:mysql://endpoint/schema?rewriteBatchedStatements=true&verifyServerCertificate=false&useSSL=true&requireSSL=true", info.getUrl());
	}
}
