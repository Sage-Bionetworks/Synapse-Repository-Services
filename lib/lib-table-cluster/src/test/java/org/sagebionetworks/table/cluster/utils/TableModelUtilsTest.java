package org.sagebionetworks.table.cluster.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
import java.util.StringJoiner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.ColumnTypeInfo;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVWriter;

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
	
	ColumnModel columnOne;
	ColumnModel columnTwo;
	
	@BeforeEach
	public void before() {
		validModel = new LinkedList<ColumnModel>();
		columnOne = new ColumnModel();
		columnOne.setName("one");
		columnOne.setId("1");
		columnOne.setColumnType(ColumnType.BOOLEAN);
		validModel.add(columnOne);
		columnTwo = new ColumnModel();
		columnTwo.setName("two");
		columnTwo.setId("2");
		columnTwo.setColumnType(ColumnType.INTEGER);
		validModel.add(columnTwo);

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
		
		validSparseRowSet = TableModelUtils.createSparseChangeSet(validRowSet, Lists.newArrayList(columnTwo, columnOne));

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

	@Test
	public void testValidateBooleanFail() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowValue("some string", cm, 0, 0);
		});
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
		assertEquals("123", TableModelUtils.validateRowValue("syn123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid ENTITYID. true is not a valid Synapse ID.", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("syn345.6");
		assertEquals("syn345.6", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
	
	@Test
	public void testValidateSubmissionId() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.SUBMISSIONID);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid SUBMISSIONID. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
	
	@Test
	public void testValidateEvaluationId() {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.EVALUATIONID);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid EVALUATIONID. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
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
			assertEquals("Value at [1,3] was not a valid DATE. Invalid format: \"true\" is malformed at \"rue\"", e.getMessage());
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
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowValue(new String(tooLarge), cm, 1, 4);
		});

		assertEquals("Value at [1,4] was not a valid STRING. String '" + new String(tooLarge)
				+ "' exceeds the maximum length of 555 characters.", exception.getMessage());
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
		String valueTooBig = createStringOfSize((int) (ColumnConstants.MAX_LARGE_TEXT_CHARACTERS+1));
		// call under test
		try {
			TableModelUtils.validateRowValue(valueTooBig, cm, 0, 0);
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [0,0] was not a valid LARGETEXT. Exceeds the maximum number of characters: 524288", e.getMessage());
		}
	}
	
	@Test
	public void testValidateStringTooBig() {
		int sizeTooLarge = (int) (ColumnConstants.MAX_ALLOWED_STRING_SIZE+1);
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(new Long(sizeTooLarge));
		String valueTooBig = createStringOfSize(sizeTooLarge);
		// call under test
		try {
			TableModelUtils.validateRowValue(valueTooBig, cm, 0, 0);
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [0,0] was not a valid STRING. Exceeds the maximum number of character: 1000", e.getMessage());
		}
	}
	/**
	 * Helper to create a string of the given size.
	 * @param size
	 * @return
	 */
	public static String createStringOfSize(int size){
		char[] chars = new char[size];
		Arrays.fill(chars, 'a');
		return new String(chars);
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
			assertNull(TableModelUtils.validateRowValue("", cm, 0, 0), "Value of an empty string for a non-string should be treated as null");
		}
	}

	@Test
	public void testValidateValue_StringList_valueListSizeTooLarge() {
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);
		//make array list of 1 over limit
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		for (int i = 0; i < ColumnConstants.MAX_ALLOWED_LIST_LENGTH + 1; i++) {
			joiner.add("\"a\"");
		}

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateValue(joiner.toString(), cm);
		});

		assertTrue(exception.getMessage().contains("value can not exceed 100 elements in list: "));
	}

	@Test
	public void testValidateValue_StringList_valueNotJsonArray(){
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateValue("i am not a list", cm);
		});

		assertEquals("Not a JSON Array: i am not a list", exception.getMessage());
	}

	@Test
	public void testValidateValue_StringList_valueListElementSizeExceeded(){
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);
		cm.setMaximumSize(4L);
		cm.setMaximumListLength(52L);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateValue("[\"1\",\"12345\", \"123\"]", cm);
		});

		assertEquals("String '12345' exceeds the maximum length of 4 characters.", exception.getMessage());
	}

	@Test
	public void testValidateValue_StringList_valueListLengthExceeded(){
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);
		cm.setMaximumSize(54L);
		cm.setMaximumListLength(2L);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateValue("[\"1\",\"12345\", \"123\"]", cm);
		});

		assertEquals("Exceeds the maximum number of list elements defined in the ColumnModel (2): \"[\"1\",\"12345\", \"123\"]\"", exception.getMessage());
	}

	@Test
	public void testValidateValue_StringList_EmptyJSONList(){
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);
		cm.setMaximumSize(54L);
		cm.setMaximumListLength(2L);

		//method under test
		assertNull(TableModelUtils.validateValue("[]", cm));
	}

	@Test
	public void testValidateValue_IntList_valueListLengthExceeded(){
		ColumnModel cm = TableModelTestUtils.createColumn(123L, "myCol", ColumnType.STRING_LIST);
		cm.setMaximumListLength(2L);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateValue("[1, 12345, 123]", cm);
		});

		assertEquals("Exceeds the maximum number of list elements defined in the ColumnModel (2): \"[1, 12345, 123]\"", exception.getMessage());
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
	public void testGetIds() {
		List<String> expected = Lists.newArrayList("1", "2");
		List<String> ids = TableModelUtils.getIds(validModel);
		assertNotNull(ids);
		assertEquals(expected, ids);
	}
	
	@Test
	public void testGetIdsNullId() {
		validModel.get(0).setId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.getIds(validModel);
		});
	}
	
	@Test
	public void testDelimitedStringRoundTrip() {
		List<String> ids = TableModelUtils.getIds(validModel);
		String del = TableModelUtils.createDelimitedColumnModelIdString(ids);
		assertNotNull(del);
		List<String> result = TableModelUtils.readColumnModelIdsFromDelimitedString(del);
		assertEquals(ids, result);
	}
	
	@Test
	public void testDistictVersionsNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.getDistictVersions(null);
		});
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
	
	@Test
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

		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.getDistictValidRowIds(changeSet.rowIterator());
		});
	}
	
	@Test
	public void testCalculateMaxSizeForTypeString(){
		long maxSize = 444;
		char[] array = new char[(int) maxSize];
		Arrays.fill(array, Character.MAX_VALUE);
		int expected = (int) (maxSize * ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8);
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, maxSize, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLink(){
		long maxSize = 444;
		char[] array = new char[(int) maxSize];
		Arrays.fill(array, Character.MAX_VALUE);
		int expected = (int) (maxSize * ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8);
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.LINK, maxSize, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeBoolean(){
		int expected = "false".getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.BOOLEAN, null, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLong(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER, null, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeDate(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.DATE, null, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeDouble(){
		double big = -1.123456789123456789e123;
		int expected = Double.toString(big).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.DOUBLE, null, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeFileHandle(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.FILEHANDLEID, null, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeUserID(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.USERID, null, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeSubmissionID(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.SUBMISSIONID, null, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeEvaluationID(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.EVALUATIONID, null, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeEntityId(){
		int expected = ("syn" + -1111111111111111111l + "." + -1111111111111111111l)
				.getBytes(StandardCharsets.UTF_8).length;
		assertEquals(expected, TableModelUtils.calculateMaxSizeForType(ColumnType.ENTITYID, null, null));
	}
	
	@Test
	public void testCalculateMaxSizeForTypeLargeText(){
		assertEquals(ColumnConstants.SIZE_OF_LARGE_TEXT_FOR_COLUMN_SIZE_ESTIMATE_BYTES,
				TableModelUtils.calculateMaxSizeForType(ColumnType.LARGETEXT, null, null));
	}

	@Test
	public void testCalculateMaxSizeForTypeStringList(){
		long maxSize = 444;
		long maxListLength = 52;
		int expected = (int) (maxSize * ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8) * 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.STRING_LIST, maxSize, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeStringList_nullMaxListLength(){
		long maxSize = 444;
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.STRING_LIST, maxSize, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeIntegerList(){
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length * 52;
		long maxListLength = 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeIntegerList_nullMaxListLength(){
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeDateList(){
		long maxListLength = 52;
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length * 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.DATE_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeDateList_nullMaxListLength(){
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.DATE_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeUserIdList(){
		long maxListLength = 52;
		int expected = Long.toString(-1111111111111111111l).getBytes(StandardCharsets.UTF_8).length * 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.USERID_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeUserIdList_nullMaxListLength(){
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.USERID_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeEntityIdList(){
		long maxListLength = 52;
		int expected = ("syn" + -1111111111111111111l + "." + -1111111111111111111l)
				.getBytes(StandardCharsets.UTF_8).length * 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.ENTITYID_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeEntityIdList_nullMaxListLength(){
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.ENTITYID_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeBooleanList(){
		long maxListLength = 52;

		int expected = "false".getBytes(StandardCharsets.UTF_8).length * 52;
		assertEquals(expected,
				TableModelUtils.calculateMaxSizeForType(ColumnType.BOOLEAN_LIST, null, maxListLength));
	}


	@Test
	public void testCalculateMaxSizeForTypeBooleanList_nullMaxListLength(){
		Long maxListLength = null;

		assertThrows(IllegalArgumentException.class, () ->
				TableModelUtils.calculateMaxSizeForType(ColumnType.BOOLEAN_LIST, null, maxListLength));
	}

	@Test
	public void testCalculateMaxSizeForTypeAll(){
		// The should be a size for each type.
		for (ColumnType ct : ColumnType.values()) {
			Long maxSize = null;
			Long maxListLength = null;
			if (ColumnType.STRING == ct || ColumnType.STRING_LIST == ct) {
				maxSize = 14L;
			}
			if(ColumnTypeListMappings.isList(ct)){
				maxListLength = 52L;
			}
			if (ColumnType.LINK == ct) {
				maxSize = 32L;
			}
			TableModelUtils.calculateMaxSizeForType(ct, maxSize, maxListLength);
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
		int expectedSize = TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, columnOne.getMaximumSize(), columnOne.getMaximumListLength());
		// the other part of the size does not match the schema so the max allowed string size should be used.
		expectedSize += TableModelUtils.calculateMaxSizeForType(ColumnType.STRING, ColumnConstants.MAX_ALLOWED_STRING_SIZE, ColumnConstants.MAX_ALLOWED_LIST_LENGTH);
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
		int expectedBytes = 464;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}

	@Test
	public void testCalculateActualRowSizeNullValues(){
		SparseRowDto row = new SparseRowDto();
		row.setRowId(123L);
		row.setVersionNumber(456L);
		row.setValues(null);
		int expectedBytes = 64;
		int actualBytes = TableModelUtils.calculateActualRowSize(row);
		assertEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void testCalculateMaxRowSize() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		int allBytes = TableModelUtils.calculateMaxRowSize(all);
		assertEquals(16066, allBytes);
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
		IdAndVersion withVersion = IdAndVersion.parse("syn123.456");
		assertEquals("TABLE-LOCK-123-456", TableModelUtils.getTableSemaphoreKey(withVersion));
		IdAndVersion noVersion = IdAndVersion.parse("456");
		assertEquals("TABLE-LOCK-456", TableModelUtils.getTableSemaphoreKey(noVersion));
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
	
	@Test
	public void createColumnIdToIndexMapFromNotFirstRow() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		List<String> names = new LinkedList<String>();
		for (ColumnModel cm : all) {
			names.add(cm.getName() + "not");
		}
		Collections.shuffle(names);
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(names.toArray(new String[names.size()]), all);
		});
	}
	
	@Test
	public void createColumnIdToIndexMapFromNullFirstRow() {
		List<ColumnModel> all = TableModelTestUtils.createOneOfEachType();
		String[] names = null;
		
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.createColumnIdToColumnIndexMapFromFirstRow(names, all);
		});
	}
	
	@Test
	public void testCreateColumnNameHeaderWithoutRowId() {
		List<SelectColumn> schema = Lists.newArrayList();
		schema.add(TableModelTestUtils.createSelectColumn(123L, "three", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(345L, "two", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(567L, "count(*)", ColumnType.STRING));
		boolean includeRowIdAndVersion = false;
		boolean includeRowEtag = false;
		String[] results = TableModelUtils.createColumnNameHeader(schema, includeRowIdAndVersion, includeRowEtag);
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
		boolean includeRowEtag = false;
		String[] results = TableModelUtils.createColumnNameHeader(schema, includeRowIdAndVersion, includeRowEtag);
		String[] expected = new String[] { ROW_ID, ROW_VERSION, "three", "two", "COUNT(*)" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}
	
	@Test
	public void testCreateColumnNameHeaderWithRowIdWithEtag() {
		List<SelectColumn> schema = Lists.newArrayList();
		schema.add(TableModelTestUtils.createSelectColumn(123L, "three", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(345L, "two", ColumnType.STRING));
		schema.add(TableModelTestUtils.createSelectColumn(567L, "COUNT(*)", ColumnType.STRING));
		boolean includeRowIdAndVersion = true;
		boolean includeRowEtag = true;
		String[] results = TableModelUtils.createColumnNameHeader(schema, includeRowIdAndVersion, includeRowEtag);
		String[] expected = new String[] { ROW_ID, ROW_VERSION, ROW_ETAG, "three", "two", "COUNT(*)" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testWriteRowToStringArrayIncludeRowId() {
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(2L);
		row.setValues(Arrays.asList("a", "b", "c"));
		boolean includeRowIdAndVersion = true;
		boolean includeRowEtag = false;
		String[] results = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersion, includeRowEtag);
		String[] expected = new String[] { "123", "2", "a", "b", "c" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}
	
	@Test
	public void testWriteRowToStringArrayIncludeRowIdWithEtag() {
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(2L);
		row.setEtag("someEtag");
		row.setValues(Arrays.asList("a", "b", "c"));
		boolean includeRowIdAndVersion = true;
		boolean includeRowEtag = true;
		String[] results = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersion, includeRowEtag);
		String[] expected = new String[] { "123", "2","someEtag", "a", "b", "c" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testWriteRowToStringArrayWihtoutRowId() {
		Row row = new Row();
		row.setRowId(123L);
		row.setVersionNumber(2L);
		row.setValues(Arrays.asList("a", "b", "c"));
		boolean includeRowIdAndVersion = false;
		boolean includeRowEtag = true;
		String[] results = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersion, includeRowEtag);
		String[] expected = new String[] { "a", "b", "c" };
		assertEquals(Arrays.toString(expected), Arrays.toString(results));
	}

	@Test
	public void testTranslateFromQuery() {
		assertEquals(null, TableModelUtils.translateRowValueFromQuery(null, ColumnTypeInfo.BOOLEAN));
		assertEquals(null, TableModelUtils.translateRowValueFromQuery("true", null));
		assertEquals("true", TableModelUtils.translateRowValueFromQuery("true", ColumnTypeInfo.BOOLEAN));
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
	
	@Test
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
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.getFileHandleIdsInRowSet(rowset);
		});
 	}
	
	@Test
	public void testValidateRowVersionsHappy(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		TableModelUtils.validateRowVersions(rows, versionNumber);
	}
	
	@Test
	public void testValidateRowVersionsNoMatch(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, 98L, "5","6","7","8"));
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, versionNumber);
		});
	}
	
	@Test
	public void testValidateRowVersionsNull(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, null, "5","6","7","8"));
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, versionNumber);
		});
	}
	
	@Test
	public void testValidateRowVersionsEmpty(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, versionNumber);
		});
	}
	
	@Test
	public void testValidateRowVersionsListNull(){
		Long versionNumber = 99L;
		List<Row> rows = null;
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, versionNumber);
		});
	}
	
	@Test
	public void testValidateRowVersionNull(){
		Long versionNumber = null;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, versionNumber);
		});
	}
	
	@Test
	public void testValidateRowVersionPassedNull(){
		Long versionNumber = 99L;
		List<Row> rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2","3","4"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6","7","8"));
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.validateRowVersions(rows, null);
		});
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
		assertEquals(expectedMd5Hex, md5Hex, "The MD5 should be the same regardless of order.");
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
	public void testCreateSparseChangeSetEntityRow(){
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
		row1.setEtag("etag1");
		row1.setValues(Lists.newArrayList("1", "true"));
		
		Row row2 = new Row();
		row2.setRowId(2L);
		row2.setVersionNumber(versionNumber);
		row2.setEtag("etag2");
		row2.setValues(Lists.newArrayList("2", "false"));
		
		
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(headers);
		rowSet.setEtag("etag");
		rowSet.setRows(Lists.newArrayList(row1, row2));
		rowSet.setTableId(tableId);
		
		// Call under test
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, currentScema);
		assertNotNull(sparse);
		assertEquals("etag", sparse.getEtag());
		assertEquals(currentScema, sparse.getSchema());
		assertEquals(2, sparse.getRowCount());
		List<SparseRow> rows = new LinkedList<SparseRow>();
		for(SparseRow row:sparse.rowIterator()){
			rows.add(row);
		}
		assertEquals(2, rows.size());
		SparseRow one = rows.get(0);
		assertEquals(row1.getRowId(), one.getRowId());
		assertEquals(row1.getVersionNumber(), one.getVersionNumber());
		assertEquals(row1.getEtag(), one.getRowEtag());
		assertTrue(one.hasCellValue(c1.getId()));
		assertEquals("true", one.getCellValue(c1.getId()));
		assertFalse(one.hasCellValue(c2.getId()));
		assertFalse(one.hasCellValue(c3.getId()));
	}
	
	@Test
	public void testCreateSparseChangeSetPLFM_4180(){
		ColumnModel c1 = TableModelTestUtils.createColumn(1L, "aBoolean", ColumnType.BOOLEAN);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "anInteger", ColumnType.INTEGER);
		
		List<ColumnModel> rowSetSchema = Lists.newArrayList(c2,c1);
		// the current schema is not the same as the rowset schema.
		List<ColumnModel> currentScema = Lists.newArrayList(c1,c2);
		
		Long versionNumber = 45L;
		String tableId = "syn123";
		List<String> headerIds = Lists.newArrayList("1","2");
		List<SelectColumn> headers = TableModelUtils.getSelectColumnsFromColumnIds(headerIds, rowSetSchema);
		
		Row row1 = new Row();
		row1.setRowId(1L);
		row1.setVersionNumber(versionNumber);
		row1.setValues(Lists.newArrayList("true", "1"));
		
		Row row2 = new Row();
		row2.setRowId(2L);
		row2.setVersionNumber(versionNumber);
		row2.setValues(Lists.newArrayList("false"));
		
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(headers);
		rowSet.setEtag("etag");
		rowSet.setRows(Lists.newArrayList(row1, row2));
		rowSet.setTableId(tableId);
		
		// Call under test
		SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(rowSet, currentScema);
		assertNotNull(sparse);
		assertEquals("etag", sparse.getEtag());
		assertEquals(currentScema, sparse.getSchema());
		assertEquals(2, sparse.getRowCount());
		List<SparseRow> rows = new LinkedList<SparseRow>();
		for(SparseRow row:sparse.rowIterator()){
			rows.add(row);
		}
		assertEquals(2, rows.size());
		SparseRow two = rows.get(1);
		assertEquals(row2.getRowId(), two.getRowId());
		assertEquals(row2.getVersionNumber(), two.getVersionNumber());
		assertTrue(two.hasCellValue(c1.getId()));
		assertEquals("false", two.getCellValue(c1.getId()));
		assertFalse(two.hasCellValue(c2.getId()));
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
		one.setEtag("etag1");
		one.getValues().put("11", "one");
		
		PartialRow two = new PartialRow();
		two.setRowId(2L);
		two.setEtag("etag2");
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
		assertEquals(one.getEtag(), sparseOne.getEtag());
		assertEquals(one.getValues(), sparseOne.getValues());
		// two
		assertEquals(two.getRowId(), sparseTwo.getRowId());
		assertEquals(versionNumber, sparseTwo.getVersionNumber());
		assertEquals(two.getEtag(), sparseTwo.getEtag());
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
	
	@Test
	public void testWrapInTransactionRequest(){
		UploadToTableRequest toWrap = new UploadToTableRequest();
		toWrap.setEntityId("syn123");
		// call under test.
		TableUpdateTransactionRequest result = TableModelUtils.wrapInTransactionRequest(toWrap);
		assertNotNull(result);
		assertEquals(toWrap.getEntityId(), result.getEntityId());
		assertNotNull(result.getChanges());
		assertEquals(1, result.getChanges().size());
		assertEquals(toWrap, result.getChanges().get(0));
	}
	@Test
	public void testWrapInTransactionRequestNull(){
		UploadToTableRequest toWrap =null;
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.wrapInTransactionRequest(toWrap);
		});
	}
	
	@Test
	public void testWrapInTransactionRequestNullEntityId(){
		UploadToTableRequest toWrap = new UploadToTableRequest();
		toWrap.setEntityId(null);
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.wrapInTransactionRequest(toWrap);
		});
	}
	
	@Test
	public void testExtractResponseFromTransaction(){
		TableUpdateTransactionResponse wrapped = new TableUpdateTransactionResponse();
		UploadToTableResult body = new UploadToTableResult();
		body.setEtag("123");
		wrapped.setResults(new LinkedList<TableUpdateResponse>());
		wrapped.getResults().add(body);
		// call under test
		UploadToTableResult result = TableModelUtils.extractResponseFromTransaction(wrapped, UploadToTableResult.class);
		assertEquals(body, result);
	}
	
	@Test
	public void testExtractResponseFromTransactionTooMany(){
		TableUpdateTransactionResponse wrapped = new TableUpdateTransactionResponse();
		UploadToTableResult body = new UploadToTableResult();
		body.setEtag("123");
		wrapped.setResults(new LinkedList<TableUpdateResponse>());
		wrapped.getResults().add(body);
		wrapped.getResults().add(body);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.extractResponseFromTransaction(wrapped, UploadToTableResult.class);
		});
	}
	
	
	@Test
	public void testExtractResponseFromTransactionNullList(){
		TableUpdateTransactionResponse wrapped = new TableUpdateTransactionResponse();
		UploadToTableResult body = new UploadToTableResult();
		body.setEtag("123");
		wrapped.setResults(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.extractResponseFromTransaction(wrapped, UploadToTableResult.class);
		});
	}
	
	@Test
	public void testExtractResponseFromTransactionEmptyList(){
		TableUpdateTransactionResponse wrapped = new TableUpdateTransactionResponse();
		UploadToTableResult body = new UploadToTableResult();
		body.setEtag("123");
		wrapped.setResults(new LinkedList<TableUpdateResponse>());
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.extractResponseFromTransaction(wrapped, UploadToTableResult.class);
		});
	}
	
	@Test
	public void testExtractResponseFromTransactionWrongType(){
		TableUpdateTransactionResponse wrapped = new TableUpdateTransactionResponse();
		wrapped.setResults(new LinkedList<TableUpdateResponse>());
		wrapped.getResults().add(new RowReferenceSetResults());
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			TableModelUtils.extractResponseFromTransaction(wrapped, UploadToTableResult.class);
		});
	}
	
	@Test
	public void testCalculatetPartialRowBytes(){
		PartialRow row = new PartialRow();
		Map<String, String> values = new HashMap<>();
		row.setValues(values);
		values.put("123", "45678");
		values.put("9", null);
		int expectedSize = ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8*9;
		assertEquals(expectedSize, TableModelUtils.calculatetPartialRowBytes(row));
	}
	
	@Test
	public void testCalculatetPartialRowBytesEmpty(){
		PartialRow row = new PartialRow();
		Map<String, String> values = new HashMap<>();
		row.setValues(values);
		int expectedSize = 0;
		assertEquals(expectedSize, TableModelUtils.calculatetPartialRowBytes(row));
	}
	
	@Test
	public void testCalculatetPartialRowBytesNull(){
		PartialRow row = new PartialRow();
		row.setValues(null);
		int expectedSize = 0;
		assertEquals(expectedSize, TableModelUtils.calculatetPartialRowBytes(row));
	}
	
	@Test
	public void testCalculatePartialRowSetBytes(){
		PartialRow row = new PartialRow();
		Map<String, String> values = new HashMap<>();
		row.setValues(values);
		values.put("123", "45678");
		values.put("9", null);
		
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(Lists.newArrayList(row,row));
		
		int expectedSize = ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8*9*2;
		assertEquals(expectedSize, TableModelUtils.calculatePartialRowSetBytes(rowSet));
	}
	
	@Test
	public void testValidateRequestSizeUnder(){
		PartialRow row = new PartialRow();
		Map<String, String> values = new HashMap<>();
		row.setValues(values);
		values.put("1", "23");
		
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(Lists.newArrayList(row));
		int maxBytesPerRequest = TableModelUtils.calculatePartialRowSetBytes(rowSet)+1;
		// call under test
		TableModelUtils.validateRequestSize(rowSet, maxBytesPerRequest);
	}
	
	@Test
	public void testValidateRequestSizeOver(){
		PartialRow row = new PartialRow();
		Map<String, String> values = new HashMap<>();
		row.setValues(values);
		values.put("1", "23");
		
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(Lists.newArrayList(row));
		int maxBytesPerRequest = TableModelUtils.calculatePartialRowSetBytes(rowSet)-1;
		try {
			// call under test
			TableModelUtils.validateRequestSize(rowSet, maxBytesPerRequest);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals(String.format(TableModelUtils.EXCEEDS_MAX_SIZE_TEMPLATE, maxBytesPerRequest), e.getMessage());
		}
	}
	
	@Test
	public void testValiteRequestSizeRowSetUnder(){
		RowSet set = new RowSet();
		set.setRows(validRowSet.getRows());
		int rowSize = TableModelUtils.calculateMaxRowSize(validModel);
		int maxBytesPerRequest = rowSize * set.getRows().size() + 1;
		TableModelUtils.validateRequestSize(validModel, set, maxBytesPerRequest);
	}
	
	@Test
	public void testValiteRequestSizeRowSetOver(){
		RowSet set = new RowSet();
		set.setRows(validRowSet.getRows());
		int rowSize = TableModelUtils.calculateMaxRowSize(validModel);
		int maxBytesPerRequest = rowSize * set.getRows().size() - 1;
		try {
			TableModelUtils.validateRequestSize(validModel, set, maxBytesPerRequest);
			fail("Should have failed");
		} catch (Exception e) {
			assertEquals(String.format(TableModelUtils.EXCEEDS_MAX_SIZE_TEMPLATE, maxBytesPerRequest), e.getMessage());
		}
	}
	
	@Test
	public void testCreateListOfAllColumnModelsBothNull() {
		ColumnModel oldModel = null;
		ColumnModel newModel = null;
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldModel, newModel);
		assertEquals("ColumnChange [oldColumn=null, newColumn=null]", changeOne.toString());
		List<ColumnModel> results = TableModelUtils.createListOfAllColumnModels(Lists.newArrayList(changeOne));
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCreateListOfAllColumnModelsOldNull() {
		ColumnModel oldModel = null;
		ColumnModel newModel = columnOne;
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldModel, newModel);
		assertEquals("ColumnChange [oldColumn=null, newColumn=1]", changeOne.toString());
		List<ColumnModel> results = TableModelUtils.createListOfAllColumnModels(Lists.newArrayList(changeOne));
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(columnOne, results.get(0));
	}
	
	@Test
	public void testCreateListOfAllColumnModelsNewNull() {
		ColumnModel oldModel = columnOne;
		ColumnModel newModel = null;
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldModel, newModel);
		assertEquals("ColumnChange [oldColumn=1, newColumn=null]", changeOne.toString());
		List<ColumnModel> results = TableModelUtils.createListOfAllColumnModels(Lists.newArrayList(changeOne));
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(columnOne, results.get(0));
	}
	
	@Test
	public void testCreateListOfAllColumnModelsWithBoth() {
		ColumnModel oldModel = columnOne;
		ColumnModel newModel = columnTwo;
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldModel, newModel);
		assertEquals("ColumnChange [oldColumn=1, newColumn=2]", changeOne.toString());
		List<ColumnModel> results = TableModelUtils.createListOfAllColumnModels(Lists.newArrayList(changeOne));
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(newModel, results.get(0));
		assertEquals(oldModel, results.get(1));
	}
	
	@Test
	public void testCreateChangesFromOldSchemaToNew() {
		List<String> oldSchema = Lists.newArrayList("1","2");
		List<String> newSchema = Lists.newArrayList("2","4");
		// call under test
		List<ColumnChange> changes = TableModelUtils.createChangesFromOldSchemaToNew(oldSchema, newSchema);
		assertNotNull(changes);
		assertEquals(2, changes.size());
		ColumnChange removeOne = changes.get(0);
		assertEquals(null, removeOne.getNewColumnId());
		assertEquals("1", removeOne.getOldColumnId());
		ColumnChange addFour = changes.get(1);
		assertEquals("4", addFour.getNewColumnId());
		assertEquals(null, addFour.getOldColumnId());
	}
	
	/**
	 * Old is null then add the new schema
	 */
	@Test
	public void testCreateChangesFromOldSchemaToNewNullOld() {
		List<String> oldSchema = null;
		List<String> newSchema = Lists.newArrayList("2","4");
		// call under test
		List<ColumnChange> changes = TableModelUtils.createChangesFromOldSchemaToNew(oldSchema, newSchema);
		assertNotNull(changes);
		assertEquals(2, changes.size());
		ColumnChange addTwo = changes.get(0);
		assertEquals("2", addTwo.getNewColumnId());
		assertEquals(null, addTwo.getOldColumnId());
		ColumnChange addFour = changes.get(1);
		assertEquals("4", addFour.getNewColumnId());
		assertEquals(null, addFour.getOldColumnId());
	}
	
	/**
	 * New is null then clear the schema
	 */
	@Test
	public void testCreateChangesFromOldSchemaToNewNullNew() {
		List<String> oldSchema = Lists.newArrayList("1","2");
		List<String> newSchema = null;
		// call under test
		List<ColumnChange> changes = TableModelUtils.createChangesFromOldSchemaToNew(oldSchema, newSchema);
		assertNotNull(changes);
		assertEquals(2, changes.size());
		ColumnChange removeOne = changes.get(0);
		assertEquals(null, removeOne.getNewColumnId());
		assertEquals("1", removeOne.getOldColumnId());
		ColumnChange removeTwo = changes.get(1);
		assertEquals(null, removeTwo.getNewColumnId());
		assertEquals("2", removeTwo.getOldColumnId());
	}

	@Test
	public void testCreateSelectColumn_givenColumnModelWithQuotedName(){
		ColumnModel cm = new ColumnModel();
		cm.setName("quoted\"Name\"");
		cm.setColumnType(ColumnType.DOUBLE);
		cm.setId("123");

		SelectColumn selectColumn = TableModelUtils.createSelectColumn(cm);

		assertNotNull(selectColumn);
		assertEquals(cm.getName(), selectColumn.getName());
		assertEquals(cm.getColumnType(), selectColumn.getColumnType());
		assertEquals(cm.getId(), selectColumn.getId());
	}
}
