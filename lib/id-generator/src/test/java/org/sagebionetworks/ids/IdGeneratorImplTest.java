package org.sagebionetworks.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class IdGeneratorImplTest {
	
	@Autowired
	IdGenerator idGenerator;
		
	@Test
	public void testNewId(){
		assertNotNull(idGenerator);
		// Create a few IDs
		Set<Long> unique = new HashSet<Long>();
		int toCreate = 1;
		for(int i=0; i<toCreate;i++){
			long start = System.currentTimeMillis();
			Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
			long end = System.currentTimeMillis();
			assertTrue(unique.add(id));
			assertTrue(id > IdType.ENTITY_ID.getStartingId(), "All IDs must be larger than the starting ID.");
			System.out.println("ID: "+id+" in "+(end-start)+" ms");
		}
	}
	
	@Test
	public void testReserveId(){
		// Start with the current ID.
		Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
		// Reserve this ID + 10
		Long reserved = id+10;
		idGenerator.reserveId(reserved, IdType.ENTITY_ID);
		// Now get make sure the next ID is greater than the reserve
		Long next = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(next.longValue(), reserved.longValue()+1);
	}
	
	@Test
	public void testReserveIdLessThan(){
		// Start with the current ID.
		Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
		// Reserve this ID
		Long reserved = id;
		// This time the ID is already reserved so this method should be a wash.
		idGenerator.reserveId(reserved, IdType.ENTITY_ID);
		// The next ID should just be the ID + 1
		Long next = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(next.longValue(), id.longValue()+1);
	}
	
	@Test
	public void testGetMaxValueForTypeWithValue() {
		// activity will always have a value
		IdType type = IdType.ACTIVITY_ID;
		long startValue = type.startingId;
		long maxValue = idGenerator.getMaxValueForType(type);
		assertTrue(maxValue >= startValue);
	}
	
	@Test
	public void testGetMaxValueForTypeMissingValue() {
		// forum does not have a start value
		IdType type = IdType.FORUM_ID;
		long startValue = 0L;
		long maxValue = idGenerator.getMaxValueForType(type);
		assertTrue(maxValue >= startValue);
	}
	
	@Test
	public void testCreateRestoreScriptSingle() {
		IdType type = IdType.ACCESS_APPROVAL_ID;
		StringBuilder builder = new StringBuilder();
		idGenerator.createRestoreScript(builder, type);
		String result = builder.toString();
		String[] split = result.split("\n");
		assertEquals(3, split.length);
		assertEquals("# ACCESS_APPROVAL_ID", split[0]);
		assertEquals("CREATE TABLE IF NOT EXISTS ACCESS_APPROVAL_ID ("
				+ " ID BIGINT NOT NULL AUTO_INCREMENT"
				+ ", CREATED_ON BIGINT NOT NULL"
				+ ", PRIMARY KEY (ID)) ENGINE=InnoDB AUTO_INCREMENT=0;", split[1]);
		long maxValue = idGenerator.getMaxValueForType(type);
		// should insert the max value
		assertEquals("INSERT IGNORE INTO ACCESS_APPROVAL_ID"
				+ " (ID, CREATED_ON) VALUES ("+maxValue+", UNIX_TIMESTAMP()*1000);", split[2]);
	}

	@Test
	public void testCreateRestoreScript() {
		String export = idGenerator.createRestoreScript();
		assertNotNull(export);
		String[] split = export.split("\n");
		assertEquals(IdType.values().length*3, split.length,"Should be three rows for each type.");
	}
	
	@Test
	public void testCleanupType() {
		IdType type = IdType.ACCESS_APPROVAL_ID;
		long lastId = -1L;
		long startCount = idGenerator.getRowCount(type);
		// Allocate some rows
		for(int i=0; i<5; i++) {
			lastId = idGenerator.generateNewId(type);
		}
		long expectedCount = startCount+5;
		assertEquals(expectedCount, idGenerator.getRowCount(type));
		// Call under test
		idGenerator.cleanupType(type, 2L);
		expectedCount -= 2;
		assertEquals(expectedCount, idGenerator.getRowCount(type));
		// clear the rest
		idGenerator.cleanupType(type, Long.MAX_VALUE);
		// should only be one row left
		assertEquals(1L, idGenerator.getRowCount(type));
		// max value should not change
		assertEquals(lastId, idGenerator.getMaxValueForType(type));
		// cleanup should not break the sequence
		assertEquals(new Long(lastId+1L), idGenerator.generateNewId(type));
	}
}
