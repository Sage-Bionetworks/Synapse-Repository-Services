package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ColumnModelUtlisTest {
	
	ColumnModel original;
	
	@Before
	public void before(){
		original = new ColumnModel();
		original.setId("123");
		original.setName("Name");
		original.setDefaultValue("DefaultValue");
		original.setColumnType(ColumnType.FILEHANDLEID);
		original.setEnumValues(new LinkedList<String>());
		original.getEnumValues().add("Fox");
		original.getEnumValues().add("Trot");
		original.getEnumValues().add("Alpha");
	}
	
	@Test
	public void testNormalize() throws JSONObjectAdapterException{
		// The expected normalized
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("defaultvalue");
		expected.setColumnType(ColumnType.FILEHANDLEID);
		expected.setEnumValues(new LinkedList<String>());
		expected.getEnumValues().add("alpha");
		expected.getEnumValues().add("fox");
		expected.getEnumValues().add("trot");
		
		// Normalize
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized == original);
		assertEquals(expected, normlaized);
	}
	
	@Test
	public void testCalculateHash() throws JSONObjectAdapterException{
		// Create two copies of the original
		ColumnModel clone = ColumnModelUtlis.createNormalizedClone(original);
		clone.setId("999");
		clone.setName(clone.getName().toUpperCase());
		Collections.shuffle(clone.getEnumValues());
		// The clone and the original should produce the same hash.
		String originalHash = ColumnModelUtlis.calculateHash(original);
		String cloneHash = ColumnModelUtlis.calculateHash(clone);
		System.out.println(cloneHash);
		assertEquals("The two objects have the same normalized from so they should have the same hash.",originalHash, cloneHash);
		// Now changing anything should give a new hash
		clone.setDefaultValue("newDefaultValueForTheClone");
		String cloneHash2 = ColumnModelUtlis.calculateHash(clone);
		System.out.println(cloneHash2);
		assertFalse(cloneHash2.equals(cloneHash));
	}
	
	@Test
	public void testRoundTrip() {
		// first calculate the hash of the original object
		String originalHash = ColumnModelUtlis.calculateHash(original);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		normlaized.setId("123");
		// Now write to DTO
		DBOColumnModel dbo = ColumnModelUtlis.createDBOFromDTO(original);
		assertEquals(new Long(123), dbo.getId());
		assertEquals(originalHash, dbo.getHash());
		// Now make a clone
		ColumnModel clone = ColumnModelUtlis.createDTOFromDBO(dbo);
		assertEquals(normlaized, clone);
	}
	
	@Test
	public void testPrepareNewBoundColumnsAllNew(){
		long objectId = 123l;
		Set<String> toAdd = new HashSet<String>();
		toAdd.add("9");
		toAdd.add("4");
		toAdd.add("1");
		// Nothing on the existing list
		List<DBOBoundColumn> existing = new LinkedList<DBOBoundColumn>();
		List<DBOBoundColumn> results = ColumnModelUtlis.prepareNewBoundColumns(objectId, existing, toAdd);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Validate the order and state of each row.
		List<DBOBoundColumn> expected = new LinkedList<DBOBoundColumn>();
		expected.add(createBound(objectId, 1l, true));
		expected.add(createBound(objectId, 4l, true));
		expected.add(createBound(objectId, 9l, true));
		assertEquals(expected, results);
	}
	
	@Test
	public void testPrepareNewBoundColumnsOldAndNew(){
		long objectId = 123l;
		Set<String> toAdd = new HashSet<String>();
		toAdd.add("9");
		toAdd.add("4");
		toAdd.add("1");
		List<DBOBoundColumn> existing = new LinkedList<DBOBoundColumn>();
		// 2 is not part of the add and current
		existing.add(createBound(objectId, 2L, true));
		// 3 is not part of the add and not current
		existing.add(createBound(objectId, 3L, false));
		// 9 is part of the add and not current
		existing.add(createBound(objectId, 9l, false));
		// 4 is part of the add and current
		existing.add(createBound(objectId, 4l, true));
		// Run the prepare
		List<DBOBoundColumn> results = ColumnModelUtlis.prepareNewBoundColumns(objectId, existing, toAdd);
		assertNotNull(results);
		assertEquals(5, results.size());
		// Validate the order and state of each row.
		List<DBOBoundColumn> expected = new LinkedList<DBOBoundColumn>();
		expected.add(createBound(objectId, 1l, true));
		expected.add(createBound(objectId, 2l, false));
		expected.add(createBound(objectId, 3l, false));
		expected.add(createBound(objectId, 4l, true));
		expected.add(createBound(objectId, 9l, true));
		assertEquals(expected, results);
	}
	
	private DBOBoundColumn createBound(Long objectId, Long columnId, Boolean current){
		DBOBoundColumn dbo = new DBOBoundColumn();
		dbo.setColumnId(columnId);
		dbo.setObjectId(objectId);
		dbo.setIsCurrent(current);
		return dbo;
	}
	

}
