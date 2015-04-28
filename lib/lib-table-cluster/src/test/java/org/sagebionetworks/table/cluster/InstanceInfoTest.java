package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstanceInfoTest {

	@Test
	public void testCreate(){
		InstanceInfo info = new InstanceInfo("endpoint", "schema");
		assertEquals("endpoint", info.getEndpoint());
		assertEquals("schema", info.getSchema());
		assertEquals("jdbc:mysql://endpoint/schema?rewriteBatchedStatements=true", info.getUrl());
	}
}
