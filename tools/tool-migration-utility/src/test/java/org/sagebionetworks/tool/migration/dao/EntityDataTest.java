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
	
	@Test
	public void testPreProcessEntityData(){
		EntityData toPrepare = new EntityData("123", "456", "789");
		EntityData expected = new EntityData(QueryRunner.ENTITY_ID_PREFIX+"123", "456", QueryRunner.ENTITY_ID_PREFIX+"789");
		EntityData results = QueryRunnerImpl.preProcessEntityData(toPrepare);
		assertEquals(expected, results);
	}

	@Test
	public void testPreProcessEntityDataNullParent(){
		EntityData toPrepare = new EntityData("123", "456", null);
		EntityData expected = new EntityData(QueryRunner.ENTITY_ID_PREFIX+"123", "456", null);
		EntityData results = QueryRunnerImpl.preProcessEntityData(toPrepare);
		assertEquals(expected, results);
	}
}
