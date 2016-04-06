package org.sagebionetworks.table.cluster.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.util.csv.CsvNullReader;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * @author jmhill
 * 
 */
public class TableModelUtilsTest {
	
	List<ColumnModel> validModel;
	RawRowSet validRowSet;
	RawRowSet validRowSet2;
	StringWriter outWritter;
	CSVWriter out;
	
	@Before
	public void before() {
		validModel = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("one");
		cm.setId("1");
		cm.setColumnType(ColumnType.BOOLEAN);
		validModel.add(cm);
		cm = new ColumnModel();
		cm.setName("two");
		cm.setId("2");
		cm.setColumnType(ColumnType.INTEGER);
		validModel.add(cm);

		List<Long> ids = Lists.newArrayList(2L, 1L);
		List<Row> rows = new LinkedList<Row>();
		// row one
		Row row = new Row();
		row.setRowId(new Long(456));
		row.setVersionNumber(new Long(2));
		List<String> values = new LinkedList<String>();
		values.add("9999");
		values.add("true");
		row.setValues(values);
		rows.add(row);
		// row two
		row = new Row();
		row.setRowId(new Long(457));
		row.setVersionNumber(new Long(2));
		values = new LinkedList<String>();
		values.add("0");
		values.add("false");
		row.setValues(values);
		rows.add(row);
		validRowSet = new RawRowSet(ids, null, null, rows);

		outWritter = new StringWriter();
		out = new CSVWriter(outWritter);

		// Create a second set that has the same order as the schema.
		String tableId = "456";
		ids = Lists.newArrayList(1L, 2L);

		rows = new LinkedList<Row>();
		// row one
		row = new Row();
		row.setRowId(new Long(456));
		row.setVersionNumber(new Long(2));
		values = new LinkedList<String>();
		values.add("true");
		values.add("123");
		row.setValues(values);
		rows.add(row);
		// row two
		row = new Row();
		row.setRowId(new Long(457));
		row.setVersionNumber(new Long(2));
		values = new LinkedList<String>();
		values.add("false");
		values.add("0");
		row.setValues(values);
		rows.add(row);
		// Create the set
		validRowSet2 = new RawRowSet(ids, null, tableId, rows);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullModel() {
		TableModelUtils.validateAndWriteToCSV(null, validRowSet, out);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullRowSet() {
		TableModelUtils.validateAndWriteToCSV(validModel, null, out);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullOut() {
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullHeaders() {
		RawRowSet invalidRowSet = new RawRowSet(null, validRowSet.getEtag(), validRowSet.getTableId(), validRowSet.getRows());
		TableModelUtils.validateAndWriteToCSV(validModel, invalidRowSet, out);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullRows() {
		RawRowSet invalidRowSet = new RawRowSet(validRowSet.getIds(), validRowSet.getEtag(), validRowSet.getTableId(), null);
		TableModelUtils.validateAndWriteToCSV(validModel, invalidRowSet, out);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVRowsEmpty() {
		RawRowSet invalidRowSet = new RawRowSet(validRowSet.getIds(), validRowSet.getEtag(), validRowSet.getTableId(),
				new LinkedList<Row>());
		TableModelUtils.validateAndWriteToCSV(validModel, invalidRowSet, out);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVModelsEmpty() {
		TableModelUtils.validateAndWriteToCSV(new LinkedList<ColumnModel>(), validRowSet, out);
	}

	@Test
	public void testValidateAndWriteToCSVWrongValueSize() {
		try {
			validRowSet.getRows().get(0).getValues().add("too many");
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals(
					"Row.value size must be equal to the number of columns in the table.  The table has :2 columns and the passed Row.value has: 3 for row number: 0",
					e.getMessage());
		}
	}

	@Test
	public void testValidateAndWriteToCSVRowIdNull() {
		try {
			validRowSet.getRows().get(0).setRowId(null);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Row.rowId cannot be null for row number: 0", e.getMessage());
		}
	}

	@Test
	public void testValidateAndWriteToCSVRowVersionNull() {
		try {
			validRowSet.getRows().get(1).setVersionNumber(null);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Row.versionNumber cannot be null for row number: 1", e.getMessage());
		}
	}

	@Test
	public void testValidateAndWriteToCSVHeaderMissmatch() {
		try{
			validRowSet.getIds().remove(0);
			validRowSet.getIds().add(0, 3L);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("The Table's ColumnModels includes: name=two with id=2 but 2 was not found in the headers of the RowResults",
					e.getMessage());
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testValidateAndWriteToCSVEmptyValues() {
		validRowSet.getRows().get(1).setValues(Lists.<String> newArrayList());
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
	}

	@Test
	public void testHappyCase() throws IOException {
		// Write the following data
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
		String csv = outWritter.toString();
		System.out.println(csv);
		StringReader reader = new StringReader(csv);
		// There should be two rows
		CsvNullReader in = new CsvNullReader(reader);
		List<String[]> results = in.readAll();
		in.close();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertArrayEquals(new String[] { "456", "2", "true", "9999" }, results.get(0));
		assertArrayEquals(new String[] { "457", "2", "false", "0" }, results.get(1));
	}

	@Test
	public void testHappyCaseWithTrailingLineBreak() throws IOException {
		// Write the following data
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
		outWritter.write('\n');
		String csv = outWritter.toString();
		StringReader reader = new StringReader(csv);
		// There should be two rows
		CsvNullReader in = new CsvNullReader(reader);
		List<String[]> results = in.readAll();
		in.close();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertArrayEquals(new String[] { "456", "2", "true", "9999" }, results.get(0));
		assertArrayEquals(new String[] { "457", "2", "false", "0" }, results.get(1));
	}

	@Test
	public void testHappyDeleteNullValues() throws IOException {
		validRowSet.getRows().get(1).setValues(null);
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
		String csv = outWritter.toString();
		System.out.println(csv);
		StringReader reader = new StringReader(csv);
		// There should be two rows
		CsvNullReader in = new CsvNullReader(reader);
		List<String[]> results = in.readAll();
		in.close();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertArrayEquals(new String[] { "456", "2", "true", "9999" }, results.get(0));
		assertArrayEquals(new String[] { "457", "2" }, results.get(1));
	}

	@Test
	public void testHappyDeleteEmptyValues() throws IOException {
		validRowSet.getRows().get(1).setValues(null);
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
		String csv = outWritter.toString();
		System.out.println(csv);
		StringReader reader = new StringReader(csv);
		// There should be two rows
		CsvNullReader in = new CsvNullReader(reader);
		List<String[]> results = in.readAll();
		in.close();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertArrayEquals(new String[] { "456", "2", "true", "9999" }, results.get(0));
		assertArrayEquals(new String[] { "457", "2" }, results.get(1));
	}

	@Test
	public void testValidateBoolean() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		assertEquals(Boolean.FALSE.toString(), TableModelUtils.validateRowValue("FalSE", cm, 0, 0));
		assertEquals(Boolean.TRUE.toString(), TableModelUtils.validateRowValue("true", cm, 1, 1));
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		assertEquals(null, TableModelUtils.validateRowValue("", cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue(Boolean.TRUE.toString());
		assertEquals(Boolean.TRUE.toString(), TableModelUtils.validateRowValue(null, cm, 2, 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateBooleanFail() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		TableModelUtils.validateRowValue("some string", cm, 0, 0);
	}

	@Test
	public void testValidateLong() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid INTEGER. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateFileHandleId() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.FILEHANDLEID);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid FILEHANDLEID. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateEntityId() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.ENTITYID);
		assertEquals("syn123.33", TableModelUtils.validateRowValue("syn123.33", cm, 0, 0));
		assertEquals("syn123", TableModelUtils.validateRowValue("syn123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid ENTITYID. Malformed entity ID (should be syn123 or syn 123.4): true", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("syn345.6");
		assertEquals("syn345.6", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateDate() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.DATE);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid DATE. Invalid format: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateDouble() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		assertEquals("123.1", TableModelUtils.validateRowValue("123.1", cm, 0, 0));
		assertEquals("NaN", Double.toString(Double.NaN));
		assertEquals("-Infinity", Double.toString(Double.NEGATIVE_INFINITY));
		assertEquals("Infinity", Double.toString(Double.POSITIVE_INFINITY));
		assertEquals("NaN", TableModelUtils.validateRowValue("NaN", cm, 0, 0));
		assertEquals("NaN", TableModelUtils.validateRowValue("nan", cm, 0, 0));
		assertEquals("NaN", TableModelUtils.validateRowValue("NAN", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-Infinity", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-infinity", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-INFINITY", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-inf", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-INF", cm, 0, 0));
		assertEquals("-Infinity", TableModelUtils.validateRowValue("-\u221E", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+Infinity", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+infinity", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+INFINITY", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+inf", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+INF", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("+\u221E", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("Infinity", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("infinity", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("INFINITY", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("inf", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("INF", cm, 0, 0));
		assertEquals("Infinity", TableModelUtils.validateRowValue("\u221E", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid DOUBLE. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("-89.3e12");
		assertEquals("-89.3e12", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
	
	@Test
	public void testValidateString() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(555L);
		assertEquals("some string", TableModelUtils.validateRowValue("some string", cm, 0, 0));
		char[] tooLarge = new char[(int) (cm.getMaximumSize() + 1)];
		Arrays.fill(tooLarge, 'b');
		try {
			TableModelUtils.validateRowValue(new String(tooLarge), cm, 1, 4);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,4] was not a valid STRING. String '" + new String(tooLarge)
					+ "' exceeds the maximum length of 555 characters. Consider using a FileHandle to store large strings.", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("-89.3e12");
		assertEquals("-89.3e12", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
	
	@Test
	public void testValidateLink() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LINK);
		cm.setMaximumSize(555L);
		assertEquals("some link", TableModelUtils.validateRowValue("some link", cm, 0, 0));
		char[] tooLarge = new char[(int) (cm.getMaximumSize() + 1)];
		Arrays.fill(tooLarge, 'b');
		try {
			TableModelUtils.validateRowValue(new String(tooLarge), cm, 1, 4);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,4] was not a valid LINK. Link '" + new String(tooLarge)
					+ "' exceeds the maximum length of 555 characters.", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("-89.3e12");
		assertEquals("-89.3e12", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateEnum() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(555L);
		cm.setEnumValues(Lists.newArrayList("aa", "bb", "cc"));
		assertEquals("aa", TableModelUtils.validateRowValue("aa", cm, 0, 0));
		assertEquals("bb", TableModelUtils.validateRowValue("bb", cm, 0, 0));
		assertEquals("cc", TableModelUtils.validateRowValue("cc", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("dd", cm, 1, 4);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,4] was not a valid STRING. 'dd' is not a valid value for this column. Valid values are: aa, bb, cc.",
					e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		cm.setDefaultValue("aa");
		assertEquals("aa", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}

	@Test
	public void testValidateStringColumnEmptyString() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(555L);
		assertEquals("", TableModelUtils.validateRowValue("", cm, 0, 0));
	}

	@Test
	public void testValidateStringColumnEmptyLink() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LINK);
		cm.setMaximumSize(555L);
		assertEquals("", TableModelUtils.validateRowValue("", cm, 0, 0));
	}
	
	@Test
	public void testValidateLargTextColumn() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LARGETEXT);
		assertEquals("", TableModelUtils.validateRowValue("", cm, 0, 0));
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 0, 0));
		assertEquals("basic", TableModelUtils.validateRowValue("basic", cm, 0, 0));
	}
	
	@Test
	public void testValidateLargTextColumnTooBig() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LARGETEXT);
		char[] chars = new char[(int) (ColumnConstants.MAX_LARGE_TEXT_CHARACTERS+1)];
		Arrays.fill(chars, 'a');
		String valueTooBig = new String(chars);
		// call under test
		try {
			TableModelUtils.validateRowValue(valueTooBig, cm, 0, 0);
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [0,0] was not a valid LARGETEXT. Exceeds the maximum number of characters: 349525", e.getMessage());
		}
	}

	@Test
	public void testValidateNonColumnEmptyString() {
		for (ColumnType type : ColumnType.values()) {
			ColumnModel cm = new ColumnModel();
			// String are allowed to be empty
			if (ColumnType.STRING.equals(type))
				continue;
			if (ColumnType.LINK.equals(type))
				continue;
			if (ColumnType.LARGETEXT.equals(type))
				continue;
			cm.setColumnType(type);
			cm.setMaximumSize(555L);
			cm.setDefaultValue(null);
			assertEquals("Value of an empty string for a non-string should be treated as null", null,
					TableModelUtils.validateRowValue("", cm, 0, 0));
		}
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsNone() {
		assertEquals(0, TableModelUtils.countEmptyOrInvalidRowIds(validRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsNull() {
		validRowSet.getRows().get(0).setRowId(null);
		assertEquals(1, TableModelUtils.countEmptyOrInvalidRowIds(validRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsInvalid() {
		validRowSet.getRows().get(0).setRowId(-1l);
		assertEquals(1, TableModelUtils.countEmptyOrInvalidRowIds(validRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsMixed() {
		validRowSet.getRows().get(0).setRowId(-1l);
		validRowSet.getRows().get(1).setRowId(null);
		assertEquals(2, TableModelUtils.countEmptyOrInvalidRowIds(validRowSet));
	}
	
	@Test
	public void testAssignRowIdsAndVersionNumbers_NoRowIdsNeeded() {
		IdRange range = new IdRange();
		range.setMaximumId(null);
		range.setMinimumId(null);
		range.setMaximumUpdateId(999l);
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
		// Validate each row was assigned a version number
		for (Row row : validRowSet.getRows()) {
			assertEquals(versionNumber, row.getVersionNumber());
		}
	}
	
	@Test
	public void testAssignRowIdsAndVersionNumbers_MixedRowIdsNeeded() {
		IdRange range = new IdRange();
		range.setMaximumId(new Long(457));
		range.setMinimumId(new Long(457));
		range.setMaximumUpdateId(new Long(456));
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		validRowSet.getRows().get(1).setRowId(null);
		TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
		// Validate each row was assigned a version number
		for (Row row : validRowSet.getRows()) {
			assertEquals(versionNumber, row.getVersionNumber());
		}
		assertEquals(new Long(456), validRowSet.getRows().get(0).getRowId());
		// The second row should have been provided
		assertEquals(new Long(457), validRowSet.getRows().get(1).getRowId());
	}
	
	@Test
	public void testAssignRowIdsAndVersionNumbers_AllNeeded() {
		IdRange range = new IdRange();
		range.setMaximumId(new Long(101));
		range.setMinimumId(new Long(100));
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		// Clear all the row ids
		validRowSet.getRows().get(0).setRowId(null);
		validRowSet.getRows().get(1).setRowId(null);
		TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
		// Validate each row was assigned a version number
		for (Row row : validRowSet.getRows()) {
			assertEquals(versionNumber, row.getVersionNumber());
		}
		assertEquals(new Long(100), validRowSet.getRows().get(0).getRowId());
		// The second row should have been provided
		assertEquals(new Long(101), validRowSet.getRows().get(1).getRowId());
	}
	
	@Test
	public void testAssignRowIdsAndVersionNumbers_NoneAllocatedButNeeded() {
		IdRange range = new IdRange();
		// no ids allocated
		range.setMaximumId(null);
		range.setMinimumId(null);
		range.setMaximumUpdateId(999l);
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		// Clear all the row ids
		validRowSet.getRows().get(1).setRowId(null);
		try {
			TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
			fail("should have failed");
		} catch (IllegalStateException e) {
			assertEquals("RowSet required at least one row ID but none were allocated.", e.getMessage());
		}
	}
	
	@Test
	public void testAssignRowIdsAndVersionNumbers_NotEnoutAllocated() {
		IdRange range = new IdRange();
		// only allocate on id
		range.setMaximumId(3l);
		range.setMinimumId(3l);
		range.setMaximumUpdateId(2l);
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		// Clear all the row ids
		validRowSet.getRows().get(0).setRowId(null);
		validRowSet.getRows().get(1).setRowId(null);
		try {
			TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
			fail("should have failed");
		} catch (IllegalStateException e) {
			assertEquals("RowSet required more row IDs than were allocated.", e.getMessage());
		}
	}
	
	@Test
	public void testAssignRowIdsInvalidUpdateRowId() {
		IdRange range = new IdRange();
		// only allocate on id
		range.setMaximumId(null);
		range.setMinimumId(null);
		range.setMaximumUpdateId(1l);
		Long versionNumber = new Long(4);
		range.setVersionNumber(versionNumber);
		// Clear all the row ids
		validRowSet.getRows().get(0).setRowId(0l);
		validRowSet.getRows().get(1).setRowId(2l);
		try {
			TableModelUtils.assignRowIdsAndVersionNumbers(validRowSet, range);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Cannot update row: 2 because it does not exist.", e.getMessage());
		}
	}

	@Test
	public void testCSVRoundTrip() throws IOException {
		// Write this to a string
		StringWriter writer = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(writer);
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet2, csvWriter);
		StringReader reader = new StringReader(writer.toString());
		CsvNullReader csvReader = new CsvNullReader(reader);
		List<Row> cloneRows = TableModelUtils.readFromCSV(csvReader);
		assertNotNull(cloneRows);
		assertEquals(validRowSet2.getRows(), cloneRows);
	}

	@Test
	public void testCSVGZipRoundTrip() throws IOException {
		// Write this to a string
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TableModelUtils.validateAnWriteToCSVgz(validModel, validRowSet2, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		List<Row> cloneRows = TableModelUtils.readFromCSVgzStream(in);
		assertNotNull(cloneRows);
		assertEquals(validRowSet2.getRows(), cloneRows);
	}
	
	@Test
	public void testGetIds() {
		List<Long> expected = Lists.newArrayList(1L, 2L);
		List<Long> ids = TableModelUtils.getIds(validModel);
		assertNotNull(ids);
		assertEquals(expected, ids);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetIdsNullId() {
		validModel.get(0).setId(null);
		TableModelUtils.getIds(validModel);
	}
	
	@Test
	public void testDelimitedStringRoundTrip() {
		List<Long> ids = TableModelUtils.getIds(validModel);
		String del = TableModelUtils.createDelimitedColumnModelIdString(ids);
		assertNotNull(del);
		System.out.println(del);
		List<Long> result = TableModelUtils.readColumnModelIdsFromDelimitedString(del);
		assertEquals(ids, result);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDistictVersionsNull() {
		TableModelUtils.getDistictVersions(null);
	}
	
	@Test
	public void testDistictVersions() {
		List<RowReference> refs = new LinkedList<RowReference>();
		RowReference ref = new RowReference();
		ref.setVersionNumber(100l);
		refs.add(ref);
		ref = new RowReference();
		ref.setVersionNumber(100l);
		refs.add(ref);
		ref = new RowReference();
		ref.setVersionNumber(101l);
		refs.add(ref);
		Set<Long> expected = new HashSet<Long>();
		expected.add(101l);
		expected.add(100l);
		assertEquals(expected, TableModelUtils.getDistictVersions(refs));
	}
	
	@Test
	public void testDistictRowIds() {
		List<Row> refs = new LinkedList<Row>();
		Row ref = new Row();
		ref.setRowId(100l);
		ref.setVersionNumber(500L);
		ref.setValues(new LinkedList<String>());
		refs.add(ref);
		
		ref = new Row();
		ref.setRowId(101l);
		ref.setVersionNumber(501L);
		ref.setValues(new LinkedList<String>());
		refs.add(ref);
		
		ref = new Row();
		ref.setRowId(null);
		refs.add(ref);
		ref = new Row();
		ref.setRowId(-1l);
		refs.add(ref);
		Map<Long, Long> expected = Maps.newHashMap();
		expected.put(101l, 501L);
		expected.put(100l, 500L);
		assertEquals(expected, TableModelUtils.getDistictValidRowIds(refs));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateRowIds() {
		List<Row> refs = new LinkedList<Row>();
		Row ref = new Row();
		ref.setRowId(100l);
		ref.setVersionNumber(500L);
		ref.setValues(new LinkedList<String>());
		refs.add(ref);
		
		ref = new Row();
		ref.setRowId(101l);
		ref.setVersionNumber(501L);
		ref.setValues(new LinkedList<String>());
		refs.add(ref);
		
		ref = new Row();
		ref.setRowId(100l);
		ref.setVersionNumber(600L);
		ref.setValues(new LinkedList<String>());
		refs.add(ref);
		
		ref = new Row();
		ref.setRowId(null);
		refs.add(ref);
		ref = new Row();
		ref.setRowId(-1l);
		refs.add(ref);
		TableModelUtils.getDistictValidRowIds(refs);
	}

	@Test
	public void testConvertToSchemaAndMerge() {
		// Create two columns
		List<ColumnModel> models = Lists.newArrayList(TableModelTestUtils.createColumn(1L, "first", ColumnType.STRING),
				TableModelTestUtils.createColumn(2L, "second", ColumnType.STRING));

		// Create some data for this model
		List<Row> v1Rows = TableModelTestUtils.createRows(models, 2);
		RawRowSet v1Set = new RawRowSet(TableModelUtils.getIds(models), null, null, v1Rows);
		IdRange range = new IdRange();
		range.setVersionNumber(0l);
		range.setMinimumId(0l);
		range.setMaximumId(1l);
		TableModelUtils.assignRowIdsAndVersionNumbers(v1Set, range);
		// now remove column two
		models.remove(1);
		// Now add back two new columns one with a default value and one without
		// three
		models.add(TableModelTestUtils.createColumn(3L, "third", ColumnType.BOOLEAN));
		// four
		models.add(TableModelTestUtils.createColumn(4L, "fourth", ColumnType.STRING));

		// Create some more data with the new schema
		List<Row> v2Rows = TableModelTestUtils.createRows(models, 2);
		RawRowSet v2Set = new RawRowSet(TableModelUtils.getIds(models), null, null, v2Rows);
		range = new IdRange();
		range.setVersionNumber(1l);
		range.setMinimumId(2l);
		range.setMaximumId(3l);
		TableModelUtils.assignRowIdsAndVersionNumbers(v2Set, range);

		// Now request the data in a different order
		List<ColumnModel> newOrder = new LinkedList<ColumnModel>();
		newOrder.add(models.get(2));
		newOrder.add(models.get(0));
		newOrder.add(models.get(1));
		List<RawRowSet> all = Lists.newArrayList(v1Set, v2Set);
		// Now get a single result set that contains all data in this new form
		RowSet converted = TableModelUtils.convertToSchemaAndMerge(all, TableModelUtils.createColumnModelColumnMapper(newOrder, false),
				"syn123", null);
		// System.out.println(converted.toString());
		// This is what we expect to come back
		List<Row> expectedRows = new LinkedList<Row>();
		// one
		Row row = new Row();
		row.setRowId(0l);
		row.setVersionNumber(0l);
		row.setValues(Arrays.asList(new String[] { null, "string0", null }));
		expectedRows.add(row);
		// two
		row = new Row();
		row.setRowId(1l);
		row.setVersionNumber(0l);
		row.setValues(Arrays.asList(new String[] { null, "string1", null }));
		expectedRows.add(row);
		// three
		row = new Row();
		row.setRowId(2l);
		row.setVersionNumber(1l);
		row.setValues(Arrays.asList(new String[] { "string200000", "string0", "false" }));
		expectedRows.add(row);
		// four
		row = new Row();
		row.setRowId(3l);
		row.setVersionNumber(1l);
		row.setValues(Arrays.asList(new String[] { "string200001", "string1", "true" }));
		expectedRows.add(row);

		RawRowSet expected = new RawRowSet(TableModelUtils.getIds(newOrder), null, "syn123", expectedRows);
		assertEquals(expected.getIds().size(), converted.getHeaders().size());
		for (int i = 0; i < expected.getIds().size(); i++) {
			assertEquals(expected.getIds().get(i).toString(), converted.getHeaders().get(i).getId());
		}
		assertEquals(expected.getTableId(), converted.getTableId());
		assertEquals(expected.getEtag(), converted.getEtag());
		assertEquals(expected.getRows().size(), converted.getRows().size());
		for (int i = 0; i < expected.getRows().size(); i++) {
			assertEquals(expected.getRows().get(i), converted.getRows().get(i));
		}
	}
	
	@Test
	public void testCalculateMaxSizeForTypeString() throws UnsupportedEncodingException {
		long maxSize = 444;
		char[] array = new char[(int) maxSize];
		Arrays.fill(array, Character.MAX_VALUE);
		int expected = new String(array).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, maxSize));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLink() throws UnsupportedEncodingException {
		long maxSize = 444;
		char[] array = new char[(int) maxSize];
		Arrays.fill(array, Character.MAX_VALUE);
		int expected = new String(array).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.LINK, maxSize));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeBoolean() throws UnsupportedEncodingException {
		int expected = new String("false").getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.BOOLEAN, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLong() throws UnsupportedEncodingException {
		int expected = new String(Long.toString(-1111111111111111111l)).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeDate() throws UnsupportedEncodingException {
		int expected = new String(Long.toString(-1111111111111111111l)).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.DATE, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeDouble() throws UnsupportedEncodingException {
		double big = -1.123456789123456789e123;
		int expected = Double.toString(big).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.DOUBLE, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeFileHandle() throws UnsupportedEncodingException {
		int expected = new String(Long.toString(-1111111111111111111l)).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.FILEHANDLEID, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeEntityId() throws UnsupportedEncodingException {
		int expected = new String("syn" + Long.toString(-1111111111111111111l) + "." + Long.toString(-1111111111111111111l))
				.getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.ENTITYID, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLargeText() throws UnsupportedEncodingException {
		long maxSize = 1000;
		char[] array = new char[(int) maxSize];
		Arrays.fill(array, Character.MAX_VALUE);
		int expected = new String(array).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.LARGETEXT, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeAll() throws UnsupportedEncodingException {
		// The should be a size for each type.
		for (ColumnType ct : ColumnType.values()) {
			Long maxSize = null;
			if (ColumnType.STRING == ct) {
				maxSize = 14L;
			}
			if (ColumnType.LINK == ct) {
				maxSize = 32L;
			}
			TableModelUtils.calculateMaxSizeForType(ct, maxSize);
		}
	}
	
	@Test
	public void testCalculateActualRowSize(){
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(456L);
		row.setValues(Lists.newArrayList("one",null,"muchLonger"));
		int expectedBytes = 79;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}

	@Test
	public void testCalculateActualRowSizeNullValues(){
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(456L);
		row.setValues(null);
		int expectedBytes = 40;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void testCalculateMaxRowSize() {
		ColumnMapper all = TableModelTestUtils.createMapperForOneOfEachType();
		int allBytes = TableModelUtils.calculateMaxRowSizeForColumnModels(all);
		assertEquals(3414, allBytes);
	}

	@Test
	public void testIsRequestWithinMaxBytePerRequest() {
		ColumnMapper all = TableModelTestUtils.createMapperForOneOfEachType();
		int allBytes = TableModelUtils.calculateMaxRowSizeForColumnModels(all);
		// Set the max to be 100 rows
		int maxBytes = allBytes * 100;
		// So 100 rows should be within limit but not 101;
		assertTrue(TableModelUtils.isRequestWithinMaxBytePerRequest(all, 100, maxBytes));
		assertFalse(TableModelUtils.isRequestWithinMaxBytePerRequest(all, 101, maxBytes));
	}

	@Test
	public void testGetTableSemaphoreKey() {
		assertEquals("TALBE-LOCK-123", TableModelUtils.getTableSemaphoreKey("syn123"));
		assertEquals("TALBE-LOCK-456", TableModelUtils.getTableSemaphoreKey("456"));
	}
	
	@Test
	public void createColumnIdToIndexMapFromFirstRow() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		List<String> names = new LinkedList<String>();
		for (ColumnModel cm : all) {
			names.add(cm.getName());
		}
		Collections.shuffle(names);
		Map<Long, Integer> map = TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(names.toArray(new String[names.size()]), all);
		assertNotNull(map);
		assertEquals(all.size(), map.size());
		Map<String, Long> nameToIdMap = TableModelUtils.createNameToIDMap(all);
		// Check the reverse
		for (Long columnId : map.keySet()) {
			Integer index = map.get(columnId);
			String name = names.get(index);
			assertEquals(columnId, nameToIdMap.get(name));
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void createColumnIdToIndexMapFromNotFirstRow() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		List<String> names = new LinkedList<String>();
		for (ColumnModel cm : all) {
			names.add(cm.getName() + "not");
		}
		Collections.shuffle(names);
		TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(names.toArray(new String[names.size()]), all);
	}
	
	@Test
	public void testCreateColumnNameHeaderWithoutRowId() {
		List<SelectColumn> schema = Lists.newArrayList();
		schema.add(TableModelTestUtils.createSelectColumn(123L, "three", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(345L, "two", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(567L, "count(*)", ColumnType.STRING));
		boolean includeRowIdAndVersion = false;
		String[] results = TableModelUtils.createColumnNameHeader(schema, includeRowIdAndVersion);
		String[] expected = new String[] { "three", "two", "count(*)" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}
	
	@Test
	public void testCreateColumnNameHeaderWithRowId() {
		List<SelectColumn> schema = Lists.newArrayList();
		schema.add(TableModelTestUtils.createSelectColumn(123L, "three", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(345L, "two", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(567L, "COUNT(*)", ColumnType.STRING));
		boolean includeRowIdAndVersion = true;
		String[] results = TableModelUtils.createColumnNameHeader(schema, includeRowIdAndVersion);
		String[] expected = new String[] { ROW_ID, ROW_VERSION, "three", "two", "COUNT(*)" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testWriteRowToStringArrayIncludeRowId() {
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(2L);
		row.setValues(Arrays.asList("a", "b", "c"));
		boolean includeRowIdAndVersion = true;
		String[] results = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersion);
		String[] expected = new String[] { "123", "2", "a", "b", "c" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testWriteRowToStringArrayWihtoutRowId() {
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(2L);
		row.setValues(Arrays.asList("a", "b", "c"));
		boolean includeRowIdAndVersion = false;
		String[] results = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersion);
		String[] expected = new String[] { "a", "b", "c" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testTranslateFromQuery() {
		assertEquals("false", TableModelUtils.translateRowValueFromQuery("0", ColumnType.BOOLEAN));
		assertEquals("true", TableModelUtils.translateRowValueFromQuery("1", ColumnType.BOOLEAN));
		assertEquals("something else", TableModelUtils.translateRowValueFromQuery("something else", ColumnType.BOOLEAN));
		assertEquals("0", TableModelUtils.translateRowValueFromQuery("0", null));

		// for all other types
		for (ColumnType type : ColumnType.values()) {
			if (type == ColumnType.BOOLEAN) {
				continue;
			}
			assertEquals("anything", TableModelUtils.translateRowValueFromQuery("anything", type));
		}
	}
	
	@Test
	public void testIsNullOrEmpty(){
		assertTrue(TableModelUtils.isNullOrEmpty(null));
		assertTrue(TableModelUtils.isNullOrEmpty(""));
		assertTrue(TableModelUtils.isNullOrEmpty(" "));
		assertFalse(TableModelUtils.isNullOrEmpty("a"));
	}

	@Test
	public void testGetFileHandleIdsInRowSet(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.STRING));
		cols.add(TableModelTestUtils.createSelectColumn(2L, "b", ColumnType.FILEHANDLEID));
		cols.add(TableModelTestUtils.createSelectColumn(3L, "c", ColumnType.STRING));
		cols.add(TableModelTestUtils.createSelectColumn(4L, "c", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5","6","7","8"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "9",null,"7",""));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		Set<Long> expected = Sets.newHashSet(2L, 4L, 6L, 8L);
		Set<Long> results = TableModelUtils.getFileHandleIdsInRowSet(rowset);
		assertEquals(expected, results);
 	}
	
	@Test
	public void testGetFileHandleIdsInRowSetWithIgnoredColumns(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(1L, "a", ColumnType.STRING));
		cols.add(TableModelTestUtils.createSelectColumn(2L, "b", ColumnType.FILEHANDLEID));
		// a null column means values in this column should be ignored.
		cols.add(null);
		cols.add(TableModelTestUtils.createSelectColumn(4L, "c", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1","2","ignore","4"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "5","6","ignore","8"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "9",null,"ignore",""));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		Set<Long> expected = Sets.newHashSet(2L, 4L, 6L, 8L);
		Set<Long> results = TableModelUtils.getFileHandleIdsInRowSet(rowset);
		assertEquals(expected, results);
 	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdsInRowSetNotLongs(){
		List<SelectColumn> cols = new ArrayList<SelectColumn>();
		cols.add(TableModelTestUtils.createSelectColumn(2L, "b", ColumnType.FILEHANDLEID));
		
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, 0L, "1"));
		rows.add(TableModelTestUtils.createRow(1L, 0L, "not a number"));
		
		RowSet rowset = new RowSet();
		rowset.setHeaders(cols);
		rowset.setRows(rows);
		
		// should fail.
		Set<Long> results = TableModelUtils.getFileHandleIdsInRowSet(rowset);
 	}
	
	@Test
	public void testValidateRowVersionsHappy(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionsNoMatch(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, 98L, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionsNull(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, null, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionsEmpty(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionsListNull(){
		Long versionNumber = 99L;
		List<Row> rows = null;
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionNull(){
		Long versionNumber = null;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testValidateRowVersionPassedNull(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, null);
	}
	
	@Test
	public void testConvertLongToString(){
		List<Long> in = Lists.newArrayList(3L,4L, null);
		Set<String> out = Sets.newHashSet();
		TableModelUtils.convertLongToString(in, out);
		Set<String> expected = Sets.newHashSet("3","4");
		assertEquals(expected, out);
	}
	
	@Test
	public void testConvertStringToLong(){
		List<String> in = Lists.newArrayList("2","4", null);
		Set<Long> out = Sets.newHashSet();
		TableModelUtils.convertStringToLong(in, out);
		Set<Long> expected = Sets.newHashSet(2L, 4L);
		assertEquals(expected, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testConvertStringToLongNotANumber(){
		List<String> in = Lists.newArrayList("2","not a number");
		Set<Long> out = Sets.newHashSet();
		// should fail.
		TableModelUtils.convertStringToLong(in, out);
	}
	
	@Test
	public void testCreateSchemaMD5Hex(){
		List<Long> ids = Lists.newArrayList(1L,2L,3L);
		String md5Hex = TableModelUtils.createSchemaMD5Hex(ids);
		assertEquals("c6fc24807df697dd54e1b891a432fe94", md5Hex);
	}
	
	@Test
	public void testCreateSchemaMD5HexCM(){
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		String md5Hex = TableModelUtils.createSchemaMD5HexCM(models);
		assertEquals("e01b96910d0eb4b107ebc34eae2bc44c", md5Hex);
	}
	
}
