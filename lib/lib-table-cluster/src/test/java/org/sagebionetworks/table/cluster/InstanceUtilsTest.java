package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InstanceUtilsTest {

	@Test
	public void testCreateDatabaseInstanceIdentifier(){
		assertEquals("prod-2-table-101", InstanceUtils.createDatabaseInstanceIdentifier("prod", "2", 101));
	}
	
	@Test
	public void testCreateDatabaseSchemaName(){
		assertEquals("devtester", InstanceUtils.createDatabaseSchemaName("dev", "tester"));
	}
	
	@Test
	public void testCreateDatabaseConnectionURL(){
		assertEquals("jdbc:mysql://endpoint/schema?rewriteBatchedStatements=true",
				InstanceUtils.createDatabaseConnectionURL("endpoint", "schema"));
	}
}
