package org.sagebionetworks.table.cluster.utils;

import static org.junit.Assert.*;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
	SparseChangeSet validSparseRowSet;
	RawRowSet validRowSet2;
	StringWriter outWritter;
	CSVWriter out;
	
	@Before
	public void before() {
		validModel = new LinkedList<ColumnModel>();
		ColumnModel cm1 = new ColumnModel();
		cm1.setName("one");
		cm1.setId("1");
		cm1.setColumnType(ColumnType.BOOLEAN);
		validModel.add(cm1);
		ColumnModel cm2 = new ColumnModel();
		cm2.setName("two");
		cm2.setId("2");
		cm2.setColumnType(ColumnType.INTEGER);
		validModel.add(cm2);

		List<String> ids = Lists.newArrayList("2", "1");
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
		validRowSet = new RawRowSet(ids, null, "syn123", rows);
		
		validSparseRowSet = TableModelUtils.createSparseChangeSet(validRowSet, Lists.newArrayList(cm2, cm1));

		outWritter = new StringWriter();
		out = new CSVWriter(outWritter);

		// Create a second set that has the same order as the schema.
		String tableId = "456";
		ids = Lists.newArrayList("1", "2");

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
			validRowSet.getIds().add(0, "3");
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
		CSVReader in = new CSVReader(reader);
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
		CSVReader in = new CSVReader(reader);
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
		CSVReader in = new CSVReader(reader);
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
		CSVReader in = new CSVReader(reader);
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
	public void testValidateUserId() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.USERID);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid USERID. For input string: \"true\"", e.getMessage());
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
		assertEquals(0, TableModelUtils.countEmptyOrInvalidRowIds(validSparseRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsNull() {
		validSparseRowSet.rowIterator().iterator().next().setRowId(null);
		assertEquals(1, TableModelUtils.countEmptyOrInvalidRowIds(validSparseRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsInvalid() {
		validSparseRowSet.rowIterator().iterator().next().setRowId(-1l);
		assertEquals(1, TableModelUtils.countEmptyOrInvalidRowIds(validSparseRowSet));
	}

	@Test
	public void testCountEmptyOrInvalidRowIdsMixed() {
		Iterator<SparseRow> it = validSparseRowSet.rowIterator().iterator();
		it.next().setRowId(-1l);
		it.next().setRowId(null);
		assertEquals(2, TableModelUtils.countEmptyOrInvalidRowIds(validSparseRowSet));
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
		CSVReader csvReader = new CSVReader(reader);
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
		List<String> expected = Lists.newArrayList("1", "2");
		List<String> ids = TableModelUtils.getIds(validModel);
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
		List<String> ids = TableModelUtils.getIds(validModel);
		String del = TableModelUtils.createDelimitedColumnModelIdString(ids);
		assertNotNull(del);
		System.out.println(del);
		List<String> result = TableModelUtils.readColumnModelIdsFromDelimitedString(del);
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
		SparseChangeSet changeSet = new SparseChangeSet("syn123", validModel);
		
		SparseRow row = changeSet.addEmptyRow();
		row.setRowId(100l);
		row.setVersionNumber(500L);
		row.setCellValue("1", "true");
		
		row = changeSet.addEmptyRow();
		row.setRowId(101l);
		row.setVersionNumber(501L);
		row.setCellValue("1", "true");
		
		row = changeSet.addEmptyRow();
		row.setRowId(null);
		
		row = changeSet.addEmptyRow();
		row.setRowId(-1L);
		
		row = changeSet.addEmptyRow();
		row.setRowId(102L);
		row.setVersionNumber(502L);
		
		Map<Long, Long> expected = Maps.newHashMap();
		expected.put(101l, 501L);
		expected.put(100l, 500L);
		assertEquals(expected, TableModelUtils.getDistictValidRowIds(changeSet.rowIterator()));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateRowIds() {
		SparseChangeSet changeSet = new SparseChangeSet("syn123", validModel);
		
		SparseRow row = changeSet.addEmptyRow();
		row.setRowId(100l);
		row.setVersionNumber(500L);
		row.setCellValue("1", "true");
		
		row = changeSet.addEmptyRow();
		row.setRowId(101l);
		row.setVersionNumber(501L);
		row.setCellValue("1", "true");
		
		row = changeSet.addEmptyRow();
		row.setRowId(100l);
		row.setVersionNumber(500L);
		row.setCellValue("1", "false");
		
		Map<Long, Long> expected = Maps.newHashMap();
		expected.put(101l, 501L);
		expected.put(100l, 500L);
		assertEquals(expected, TableModelUtils.getDistictValidRowIds(changeSet.rowIterator()));
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
		RowSet converted = TableModelUtils.convertToSchemaAndMerge(all, newOrder,
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
	public void testCalculateMaxSizeForTypeUserID() throws UnsupportedEncodingException {
		int expected = new String(Long.toString(-1111111111111111111l)).getBytes("UTF-8").length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.USERID, null));
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
	public void testCalculateMaxRowSizeMap(){
		ColumnModel columnOne = TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING);
		// this select matches the schema
		SelectColumn selectOne = TableModelUtils.createSelectColumn(columnOne);
		// match should be by names
		selectOne.setId(null);
		// this select does not match the schema
		SelectColumn selectTwo = TableModelUtils.createSelectColumn("two", ColumnType.STRING, null);
		
		List<SelectColumn> select = Lists.newArrayList(selectOne, selectTwo);
		Map<String, ColumnModel> nameToSchemaMap = new HashMap<String, ColumnModel>();
		nameToSchemaMap.put(columnOne.getName(), columnOne);
		
		// call under test
		int maxSize = TableModelUtils.calculateMaxRowSize(select, nameToSchemaMap);
		// part of the size is from the select that matches the schema
		int expectedSize = TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, columnOne.getMaximumSize());
		// the other part of the size does not match the schema so the max allowed string size should be used.
		expectedSize += TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, TableModelUtils.MAX_ALLOWED_STRING_SIZE);
		assertEquals(expectedSize, maxSize);
	}
	
	@Test
	public void testCalculateActualRowSize(){
		SparseRowDto row = new SparseRowDto();
		row.setRowId(123L);
		row.setVersionNumber(456L);
		Map<String, String> values = new HashMap<String, String>();
		values.put("1", "one");
		values.put("2", null);
		values.put("3", "muchLonger");
		row.setValues(values);
		int expectedBytes = 79;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}

	@Test
	public void testCalculateActualRowSizeNullValues(){
		SparseRowDto row = new SparseRowDto();
		row.setRowId(123L);
		row.setVersionNumber(456L);
		row.setValues(null);
		int expectedBytes = 40;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void testCalculateMaxRowSize() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		int allBytes = TableModelUtils.calculateMaxRowSize(all);
		assertEquals(3434, allBytes);
	}

	@Test
	public void testIsRequestWithinMaxBytePerRequest() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		int allBytes = TableModelUtils.calculateMaxRowSize(all);
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
		TableModelUtils.getFileHandleIdsInRowSet(rowset);
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
	public void testCreateSchemaMD5Hex(){
		List<String> ids = Lists.newArrayList("1","2","3");
		String expectedMd5Hex = "c6fc24807df697dd54e1b891a432fe94";
		// call under test.
		String md5Hex = TableModelUtils.createSchemaMD5Hex(ids);
		assertEquals(expectedMd5Hex, md5Hex);
		
		// The same ids in a different order should return the same MD5
		ids = Lists.newArrayList("3","2","1");
		// call under test.
		md5Hex = TableModelUtils.createSchemaMD5Hex(ids);
		assertEquals("The MD5 should be the same regardless of order.",expectedMd5Hex, md5Hex);
	}
	
	@Test
	public void testCreateSchemaMD5HexCM(){
		List<ColumnModel> models = TableModelTestUtils.createOneOfEachType();
		String md5Hex = TableModelUtils.createSchemaMD5HexCM(models);
		assertEquals("e903d3b0a51bee1269d01669434e48ba", md5Hex);
	}
	
	@Test
	public void testGetSelectColumnsFromColumnIdsSingle(){
		ColumnModel cm = TableModelTestUtils.createColumn(12);
		List<String> columnIds = Lists.newArrayList("12");
		List<ColumnModel> schema  = Lists.newArrayList(cm);
		// call under test
		List<SelectColumn> results = TableModelUtils.getSelectColumnsFromColumnIds(columnIds, schema);
		assertNotNull(results);
		assertEquals(1, results.size());
		SelectColumn select = results.get(0);
		assertEquals(cm.getId(), select.getId());
		assertEquals(cm.getName(), select.getName());
		assertEquals(cm.getColumnType(), select.getColumnType());
	}
	
	@Test
	public void testGetSelectColumnsFromColumnIdsMultipleWithMissing(){
		ColumnModel one = TableModelTestUtils.createColumn(1);
		ColumnModel two = TableModelTestUtils.createColumn(2);
		// This controls the order of the results.
		List<String> columnIds = Lists.newArrayList("2","4","1");
		List<ColumnModel> schema  = Lists.newArrayList(one, two);
		// call under test
		List<SelectColumn> results = TableModelUtils.getSelectColumnsFromColumnIds(columnIds, schema);
		assertNotNull(results);
		assertEquals(3, results.size());
		// first should match two
		assertEquals(two.getId(), results.get(0).getId());
		// no match for 4L should result in a null
		assertEquals(null, results.get(1));
		// last should match one
		assertEquals(one.getId(), results.get(2).getId());
	}
	
	@Test
	public void testCreateSparseChangeSet(){
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "aBoolean", ColumnType.BOOLEAN);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "anInteger", ColumnType.INTEGER);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "aString", ColumnType.STRING);
		
		List<ColumnModel> rowSetSchema = Lists.newArrayList(c2,c1);
		// the current schema is not the same as the rowset schema.
		List<ColumnModel> currentScema = Lists.newArrayList(c1,c3);
		
		Long versionNumber = 45L;
		String tableId = "syn123";
		List<String> headerIds = Lists.newArrayList("2","1");
		List<SelectColumn> headers = TableModelUtils.getSelectColumnsFromColumnIds(headerIds, rowSetSchema);
		
		Row row1 = new Row();
		row1.setRowId(1L);
		row1.setVersionNumber(versionNumber);
		row1.setValues(Lists.newArrayList("1", "true"));
		
		Row row2 = new Row();
		row2.setRowId(2L);
		row2.setVersionNumber(versionNumber);
		row2.setValues(Lists.newArrayList("2", "false"));
		
		Row row3 = new Row();
		row3.setRowId(3L);
		row3.setVersionNumber(versionNumber);
		// null values will be treated as a delete.
		row3.setValues(null);
		
		Row row4 = new Row();
		row4.setRowId(4L);
		row4.setVersionNumber(versionNumber);
		// empty list should be treated as a delete;
		row4.setValues(new LinkedList<String>());
		
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(headers);
		rowSet.setEtag("etag");
		rowSet.setRows(Lists.newArrayList(row1, row2, row3, row4));
		rowSet.setTableId(tableId);
		
		// Call under test
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, currentScema);
		assertNotNull(sparse);
		assertEquals("etag", sparse.getEtag());
		assertEquals(currentScema, sparse.getSchema());
		assertEquals(4, sparse.getRowCount());
		List<SparseRow> rows = new LinkedList<SparseRow>();
		for(SparseRow row:sparse.rowIterator()){
			rows.add(row);
		}
		assertEquals(4, rows.size());
		SparseRow one = rows.get(0);
		assertEquals(row1.getRowId(), one.getRowId());
		assertEquals(row1.getVersionNumber(), one.getVersionNumber());
		assertTrue(one.hasCellValue(c1.getId()));
		assertEquals("true", one.getCellValue(c1.getId()));
		assertFalse(one.hasCellValue(c2.getId()));
		assertFalse(one.hasCellValue(c3.getId()));
	}
	
	@Test
	public void testCreateSparseChangeSetNullSelectColumns(){
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "aBoolean", ColumnType.BOOLEAN);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "anInteger", ColumnType.INTEGER);
		ColumnModel c3 = TableModelTestUtils.createColumn(3L, "aString", ColumnType.STRING);
		
		// the current schema is not the same as the rowset schema.
		List<ColumnModel> currentScema = Lists.newArrayList(c1,c3);
		
		Long versionNumber = 45L;
		String tableId = "syn123";
		List<String> headerIds = Lists.newArrayList("2","1");
		// any column ID not in the current schema will have a null SelectColumn.
		List<SelectColumn> headers = TableModelUtils.getSelectColumnsFromColumnIds(headerIds, currentScema);
		assertNotNull(headers);
		assertEquals(2, headers.size());
		// the first header should be null
		assertNull(headers.get(0));
		assertNotNull(headers.get(1));
		assertEquals(c1.getId(), headers.get(1).getId());
		
		Row row1 = new Row();
		row1.setRowId(1L);
		row1.setVersionNumber(versionNumber);
		row1.setValues(Lists.newArrayList("1", "true"));
		
		
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(headers);
		rowSet.setEtag("etag");
		rowSet.setRows(Lists.newArrayList(row1));
		rowSet.setTableId(tableId);
		
		// Call under test
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, currentScema);
		assertNotNull(sparse);
		// should have one row that only includes the columns that overlap the current schema and rowset.
		assertEquals(1, sparse.getRowCount());
		SparseRow one = sparse.rowIterator().iterator().next();
		assertEquals(row1.getRowId(), one.getRowId());
		assertEquals(row1.getVersionNumber(), one.getVersionNumber());
		assertTrue(one.hasCellValue(c1.getId()));
		assertEquals("true", one.getCellValue(c1.getId()));
		assertFalse(one.hasCellValue(c2.getId()));
		assertFalse(one.hasCellValue(c3.getId()));
	}
	

	
	@Test
	public void testwriteReadSparesChangeSetGz() throws IOException{
		SparseChangeSetDto dto = new SparseChangeSetDto();
		dto.setTableId("syn123");
		dto.setColumnIds(Lists.newArrayList("1","2","3"));
		SparseRowDto rowDto = new SparseRowDto();
		rowDto.setRowId(0L);
		rowDto.setVersionNumber(101L);
		Map<String, String> values = new HashMap<String, String>();
		values.put("1", "foo");
		values.put("2", "bar");
		rowDto.setValues(values);
		dto.setRows(Lists.newArrayList(rowDto));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// call under test
		TableModelUtils.writeSparesChangeSetToGz(dto, out);
		// read it back
		SparseChangeSetDto copy = TableModelUtils.readSparseChangeSetDtoFromGzStream(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(dto, copy);
	}
	

	@Test
	public void testValidatePartialRowString(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(null);
		partialRow.setValues(ImmutableMap.of("foo", "updated value 2"));
	
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		try {
			TableModelUtils.validatePartialRow(partialRow, columnIds);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (Exception e) {
			assertEquals("PartialRow.value.key: 'foo' is not a valid column ID for row ID: null", e.getMessage());
		}
	}
	
	@Test
	public void testValidatePartialRowNoMatch(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(999L);
		partialRow.setValues(ImmutableMap.of("789", "updated value 2"));
	
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		try {
			TableModelUtils.validatePartialRow(partialRow, columnIds);
			fail("Should have failed since a column name was used and not an ID.");
		} catch (Exception e) {
			assertEquals("PartialRow.value.key: '789' is not a valid column ID for row ID: 999", e.getMessage());
		}
	}
	
	@Test
	public void testValidatePartialRowHappy(){
		PartialRow partialRow = new PartialRow();
		partialRow.setRowId(999L);
		partialRow.setValues(ImmutableMap.of("456", "updated value 2"));
		Set<Long> columnIds = ImmutableSet.of(123l,456L);
		TableModelUtils.validatePartialRow(partialRow, columnIds);
	}
	
	@Test
	public void testCreateSparseChangeSetFromPartialRowSet(){
		Long versionNumber = 101L;
		TableRowChange lastRowChange = new TableRowChange();
		lastRowChange.setRowVersion(versionNumber);
		lastRowChange.setEtag("etag");
		PartialRowSet partialSet = new PartialRowSet();
		partialSet.setTableId("syn123");
		
		PartialRow one = new PartialRow();
		one.setRowId(1L);
		one.setValues(new HashMap<String, String>());
		one.getValues().put("11", "one");
		
		PartialRow two = new PartialRow();
		two.setRowId(2L);
		two.setValues(new HashMap<String, String>());
		two.getValues().put("22", "two");
		
		PartialRow emptyValues = new PartialRow();
		emptyValues.setRowId(3L);
		emptyValues.setValues(new HashMap<String, String>());
		
		PartialRow deleteValue = new PartialRow();
		deleteValue.setRowId(4L);
		deleteValue.setValues(null);
		
		partialSet.setRows(Lists.newArrayList(one, two, emptyValues, deleteValue));
		
		// call under test
		SparseChangeSetDto results = TableModelUtils.createSparseChangeSetFromPartialRowSet(lastRowChange, partialSet);
		assertNotNull(results);
		assertEquals(partialSet.getTableId(), results.getTableId());
		assertEquals(lastRowChange.getEtag(), results.getEtag());
		// The empty value row should not be included in the results.
		assertEquals(3, results.getRows().size());
		SparseRowDto sparseOne = results.getRows().get(0);
		SparseRowDto sparseTwo = results.getRows().get(1);
		SparseRowDto sparseDelete = results.getRows().get(2);
		
		// one
		assertEquals(one.getRowId(), sparseOne.getRowId());
		assertEquals(versionNumber, sparseOne.getVersionNumber());
		assertEquals(one.getValues(), sparseOne.getValues());
		// two
		assertEquals(two.getRowId(), sparseTwo.getRowId());
		assertEquals(versionNumber, sparseTwo.getVersionNumber());
		assertEquals(two.getValues(), sparseTwo.getValues());
		// delete
		assertEquals(deleteValue.getRowId(), sparseDelete.getRowId());
		assertEquals(versionNumber, sparseDelete.getVersionNumber());
		assertNull(sparseDelete.getValues());
	}
	
	@Test
	public void testCreateSparseChangeSetFromPartialRowSetNullLastChange(){
		// the last change can be null
		TableRowChange lastRowChange =  null;
		
		PartialRowSet partialSet = new PartialRowSet();
		partialSet.setTableId("syn123");
		
		PartialRow one = new PartialRow();
		one.setRowId(1L);
		one.setValues(new HashMap<String, String>());
		one.getValues().put("11", "one");
		
		partialSet.setRows(Lists.newArrayList(one));
		
		// call under test
		SparseChangeSetDto results = TableModelUtils.createSparseChangeSetFromPartialRowSet(lastRowChange, partialSet);
		assertNotNull(results);
		assertEquals(partialSet.getTableId(), results.getTableId());
		// etag is null when the last change is null
		assertEquals(null, results.getEtag());
		// The empty value row should not be included in the results.
		assertEquals(1, results.getRows().size());
		SparseRowDto sparseOne = results.getRows().get(0);
		
		// one
		assertEquals(one.getRowId(), sparseOne.getRowId());
		assertEquals(new Long(0), sparseOne.getVersionNumber());
		assertEquals(one.getValues(), sparseOne.getValues());

	}
	
}
