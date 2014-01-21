package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import org.junit.Test;

public class InstanceUtilsTest {

	@Test
	public void test(){
		assertEquals("prod-2-table-101", InstanceUtils.createDatabaseInstanceIdentifier("prod", 2, 101));
	}
}
