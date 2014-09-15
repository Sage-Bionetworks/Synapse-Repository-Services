package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.common.collect.Lists;

public class ColumnModelUtlisTest {
	
	ColumnModel original;
	
	@Before
	public void before(){
		original = new ColumnModel();
		original.setId("123");
		original.setName("Name ");
		original.setDefaultValue(" DefaultValue");
		original.setMaximumSize(444l);
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(new LinkedList<String>());
		original.getEnumValues().add(" Fox");
		original.getEnumValues().add("Trot ");
		original.getEnumValues().add(" Alpha ");
	}
	
	@Test
	public void testNormalize() throws JSONObjectAdapterException{
		// The expected normalized
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("Name");
		expected.setDefaultValue("DefaultValue");
		expected.setColumnType(ColumnType.STRING);
		expected.setEnumValues(new LinkedList<String>());
		expected.getEnumValues().add("Alpha");
		expected.getEnumValues().add("Fox");
		expected.getEnumValues().add("Trot");
		expected.setMaximumSize(444L);
		
		// Normalize
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized, original);
		assertEquals(expected.toString(), normlaized.toString());
	}
	
	@Test
	public void testNormalizedStringColumnNullSize(){
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("123");
		expected.setColumnType(ColumnType.STRING);
		expected.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE);
		//input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		// Setting this to null should result in the default size.
		original.setMaximumSize(null);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized, original);
		assertEquals(expected, normlaized);
	}
	
	@Test
	public void testNormalizedStringColumnSizeTooBig(){
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		original.setMaximumSize(ColumnModelUtlis.MAX_ALLOWED_STRING_SIZE+1);
		try {
			ColumnModelUtlis.createNormalizedClone(original);
			fail("Should have failed as the size is too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(ColumnModelUtlis.MAX_ALLOWED_STRING_SIZE.toString()));
		}
	}
	
	@Test
	public void testNormalizedStringColumnJustRight(){
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("123");
		expected.setColumnType(ColumnType.STRING);
		expected.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE-1);
		// input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		original.setMaximumSize(ColumnModelUtlis.DEFAULT_MAX_STRING_SIZE-1);
		ColumnModel normlaized = ColumnModelUtlis.createNormalizedClone(original);
		assertNotNull(normlaized);
		assertNotSame("A new object should have been created", normlaized, original);
		assertEquals(expected, normlaized);
	}
	
	@Test
	public void testNormalizedEmptyEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.<String> newArrayList());
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModel normalized = ColumnModelUtlis.createNormalizedClone(original);
		assertNull(normalized.getEnumValues());
	}

	@Test
	public void testNormalizedEmptyDoubleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.<String> newArrayList("", "  "));
		original.setDefaultValue("123");

		ColumnModel normalized = ColumnModelUtlis.createNormalizedClone(original);
		assertNull(normalized.getEnumValues());
	}

	@Test
	public void testNormalizedTrimmerEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.<String> newArrayList(" aa ", "bb ", "  "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModel normalized = ColumnModelUtlis.createNormalizedClone(original);
		assertEquals(Lists.newArrayList("", "aa", "bb"), normalized.getEnumValues());
	}

	@Test
	public void testNormalizedDoubleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.<String> newArrayList(" 234 ", "1.123 ", "  "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModel normalized = ColumnModelUtlis.createNormalizedClone(original);
		assertEquals(Lists.newArrayList("1.123", "234.0"), normalized.getEnumValues());
	}

	@Test(expected = NumberFormatException.class)
	public void testIncompatibleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.<String> newArrayList("1.0", "not a number", "  "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModelUtlis.createNormalizedClone(original);
	}

	@Test(expected = NumberFormatException.class)
	public void testIncompatibleIntegerEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.INTEGER);
		original.setEnumValues(Lists.<String> newArrayList(" 234 ", "1.123 "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModelUtlis.createNormalizedClone(original);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncompatibleStringEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.<String> newArrayList(" string too long "));
		original.setDefaultValue("123");
		original.setMaximumSize(5L);

		ColumnModelUtlis.createNormalizedClone(original);
	}

	@Test
	public void testCalculateHash() throws JSONObjectAdapterException{
		// Create two copies of the original
		ColumnModel clone = ColumnModelUtlis.createNormalizedClone(original);
		clone.setId("999");
		clone.setName(clone.getName());
		Collections.shuffle(clone.getEnumValues());
		// The clone and the original should produce the same hash.
		String originalHash = ColumnModelUtlis.calculateHash(ColumnModelUtlis.createNormalizedClone(original));
		String cloneHash = ColumnModelUtlis.calculateHash(ColumnModelUtlis.createNormalizedClone(clone));
		assertEquals("The two objects have the same normalized from so they should have the same hash.",originalHash, cloneHash);
		// Now changing anything should give a new hash
		clone.setDefaultValue("newDefaultValueForTheClone");
		String cloneHash2 = ColumnModelUtlis.calculateHash(ColumnModelUtlis.createNormalizedClone(clone));
		assertFalse(cloneHash2.equals(cloneHash));
	}
	
	@Test
	public void testRoundTrip() {
		// first calculate the hash of the original object
		ColumnModel normalized = ColumnModelUtlis.createNormalizedClone(original);
		String originalHash = ColumnModelUtlis.calculateHash(normalized);
		normalized.setId("123");
		// Now write to DTO
		DBOColumnModel dbo = ColumnModelUtlis.createDBOFromDTO(original);
		assertEquals(new Long(123), dbo.getId());
		assertEquals(originalHash, dbo.getHash());
		// Now make a clone
		ColumnModel clone = ColumnModelUtlis.createDTOFromDBO(dbo);
		assertEquals(normalized, clone);
	}
	

}
