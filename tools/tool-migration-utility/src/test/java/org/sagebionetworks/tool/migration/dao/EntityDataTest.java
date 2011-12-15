package org.sagebionetworks.tool.migration.dao;

import static org.junit.Assert.*;

import org.junit.Test;

public class EntityDataTest {
	
	@Test
	public void testClone(){
		EntityData source = new EntityData("12", "456", "798");
		EntityData clone = new EntityData(source);
		assertEquals(source, clone);
	}

}
