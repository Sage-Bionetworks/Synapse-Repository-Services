package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;

import com.google.common.collect.Lists;

public class ColumnModelUtlisTest {
	
	ColumnModel original;
	
	@Before
	public void before(){
		original = new ColumnModel();
		original.setId("123");
		original.setName("Name ");
		original.setDefaultValue("Alpha ");
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
		expected.setDefaultValue("Alpha");
		expected.setColumnType(ColumnType.STRING);
		expected.setEnumValues(new LinkedList<String>());
		expected.getEnumValues().add("Alpha");
		expected.getEnumValues().add("Fox");
		expected.getEnumValues().add("Trot");
		expected.setMaximumSize(444L);
		
		// Normalize
		ColumnModel normlaized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
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
		expected.setMaximumSize(ColumnModelUtils.DEFAULT_MAX_STRING_SIZE);
		//input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		// Setting this to null should result in the default size.
		original.setMaximumSize(null);
		ColumnModel normlaized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
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
		original.setMaximumSize(ColumnConstants.MAX_ALLOWED_STRING_SIZE+1);
		try {
			ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
			fail("Should have failed as the size is too large");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(ColumnConstants.MAX_ALLOWED_STRING_SIZE.toString()));
		}
	}
	
	@Test
	public void testNormalizedStringColumnJustRight(){
		ColumnModel expected = new ColumnModel();
		expected.setId(null);
		expected.setName("name");
		expected.setDefaultValue("123");
		expected.setColumnType(ColumnType.STRING);
		expected.setMaximumSize(ColumnModelUtils.DEFAULT_MAX_STRING_SIZE-1);
		// input
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(null);
		original.setDefaultValue("123");
		original.setMaximumSize(ColumnModelUtils.DEFAULT_MAX_STRING_SIZE-1);
		ColumnModel normlaized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
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

		ColumnModel normalized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		assertNull(normalized.getEnumValues());
	}

	@Test
	public void testNormalizedEmptyDoubleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.newArrayList("", "  "));
		original.setDefaultValue("123");

		ColumnModel normalized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		assertNull(normalized.getEnumValues());
	}

	@Test
	public void testNormalizedTrimmerEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.newArrayList(" aa ", "bb ", "  "));
		original.setDefaultValue("aa");
		original.setMaximumSize(12L);

		ColumnModel normalized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		assertEquals(Lists.newArrayList("", "aa", "bb"), normalized.getEnumValues());
	}

	@Test
	public void testNormalizedDoubleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.newArrayList(" 234 ", "1.123 ", "  "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModel normalized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		assertEquals(Lists.newArrayList("1.123", "234.0"), normalized.getEnumValues());
	}

	@Test(expected = NumberFormatException.class)
	public void testIncompatibleEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.DOUBLE);
		original.setEnumValues(Lists.newArrayList("1.0", "not a number", "  "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test(expected = NumberFormatException.class)
	public void testIncompatibleIntegerEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.INTEGER);
		original.setEnumValues(Lists.newArrayList(" 234 ", "1.123 "));
		original.setDefaultValue("123");
		original.setMaximumSize(12L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncompatibleStringEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.newArrayList(" string too long "));
		original.setDefaultValue("123");
		original.setMaximumSize(5L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidStringEnum() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Lists.newArrayList("aaa"));
		original.setDefaultValue("123");
		original.setMaximumSize(5L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test
	public void testMaxEnums() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Collections.nCopies(StackConfigurationSingleton.singleton().getTableMaxEnumValues(), "aaa"));
		original.setDefaultValue(null);
		original.setMaximumSize(5L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyEnums() {
		original.setName("name");
		original.setColumnType(ColumnType.STRING);
		original.setEnumValues(Collections.nCopies(StackConfigurationSingleton.singleton().getTableMaxEnumValues() + 1, "aaa"));
		original.setDefaultValue(null);
		original.setMaximumSize(5L);

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLargeText_DisallowDefaultValue(){
		original.setName("name");
		original.setColumnType(ColumnType.LARGETEXT);
		original.setDefaultValue("value");

		ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
	}

	@Test
	public void testEnumMaxCountDocumentation() throws IOException {
		String schema = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(ColumnModel.class);
		// make sure the documentation is in agreement with the stack configuration
		Matcher m = Pattern.compile("maximum number of entries for an enum is (\\d+)\"").matcher(schema);
		assertTrue(m.find());
		assertEquals("" + StackConfigurationSingleton.singleton().getTableMaxEnumValues(), m.group(1));
	}

	@Test
	public void testCalculateHash() throws JSONObjectAdapterException{
		// Create two copies of the original
		ColumnModel clone = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		clone.setId("999");
		clone.setName(clone.getName());
		Collections.shuffle(clone.getEnumValues());
		// The clone and the original should produce the same hash.
		String originalHash = ColumnModelUtils.calculateHash(ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton()
				.getTableMaxEnumValues()));
		ColumnModel normalizedClone1 = ColumnModelUtils.createNormalizedClone(clone, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		String cloneHash = ColumnModelUtils.calculateHash(normalizedClone1);
		assertEquals("The two objects have the same normalized from so they should have the same hash.",originalHash, cloneHash);
		// Now changing anything should give a new hash
		clone.setDefaultValue("  Trot  ");
		ColumnModel normalizedClone2 = ColumnModelUtils.createNormalizedClone(clone, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		String cloneHash2 = ColumnModelUtils.calculateHash(normalizedClone2);
		assertFalse(cloneHash2.equals(cloneHash));
	}
	
	@Test
	public void testRoundTrip() {
		// first calculate the hash of the original object
		ColumnModel normalized = ColumnModelUtils.createNormalizedClone(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		String originalHash = ColumnModelUtils.calculateHash(normalized);
		normalized.setId("123");
		// Now write to DTO
		DBOColumnModel dbo = ColumnModelUtils.createDBOFromDTO(original, StackConfigurationSingleton.singleton().getTableMaxEnumValues());
		assertEquals(new Long(123), dbo.getId());
		assertEquals(originalHash, dbo.getHash());
		// Now make a clone
		ColumnModel clone = ColumnModelUtils.createDTOFromDBO(dbo);
		assertEquals(normalized, clone);
	}
	
	
	@Test
	public void testSchemaChangeToFromGzip() throws IOException{
		ColumnChange add = new ColumnChange();
		add.setOldColumnId(null);
		add.setNewColumnId("123");
		
		ColumnChange delete = new ColumnChange();
		delete.setOldColumnId("456");
		delete.setNewColumnId(null);
		
		ColumnChange update = new ColumnChange();
		update.setOldColumnId("777");
		update.setNewColumnId("888");
		
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		changes.add(add);
		changes.add(delete);
		changes.add(update);
		
		//write
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ColumnModelUtils.writeSchemaChangeToGz(changes, out);
		
		// read
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		List<ColumnChange> results = ColumnModelUtils.readSchemaChangeFromGz(in);
		assertEquals(changes, results);
	}

}
