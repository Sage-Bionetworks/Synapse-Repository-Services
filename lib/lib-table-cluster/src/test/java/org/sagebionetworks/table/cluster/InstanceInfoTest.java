package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class InstanceInfoTest {

	@Test
	public void testCreate(){
		// call under test
		InstanceInfo info = new InstanceInfo("endpoint", "schema");
		assertEquals("endpoint", info.getEndpoint());
		assertEquals("schema", info.getSchema());
		assertEquals("jdbc:mysql://endpoint/schema?rewriteBatchedStatements=true&sslMode=REQUIRED", info.getUrl());
	}
}
