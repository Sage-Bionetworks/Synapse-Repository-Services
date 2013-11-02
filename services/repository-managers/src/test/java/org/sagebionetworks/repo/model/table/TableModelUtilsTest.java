package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.table.TableModelUtils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * 
 * @author jmhill
 *
 */
public class TableModelUtilsTest {
	
	List<ColumnModel> validModel;
	RowSet validRowSet;
	StringWriter outWritter;
	CSVWriter out;
	
	@Before
	public void before(){
		validModel = new LinkedList<ColumnModel>();
		ColumnModel cm = new ColumnModel();
		cm.setName("one");
		cm.setId("1");
		cm.setColumnType(ColumnType.BOOLEAN);
		validModel.add(cm);
		cm = new ColumnModel();
		cm.setName("two");
		cm.setId("2");
		cm.setColumnType(ColumnType.LONG);
		validModel.add(cm);
		
		validRowSet = new RowSet();
		List<String> headers = new LinkedList<String>();
		headers.add("2");
		headers.add("1");
		validRowSet.setHeaders(headers);
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
		values.add("anything that is not 'true' is treated as false so this is false");
		row.setValues(values);
		rows.add(row);
		validRowSet.setRows(rows);
		
		
		outWritter = new StringWriter();
		out = new CSVWriter(outWritter);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullModel(){
		TableModelUtils.validateAndWriteToCSV(null, validRowSet, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullRowSet(){
		TableModelUtils.validateAndWriteToCSV(validModel, null, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullOut(){
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullHeaders(){
		validRowSet.setHeaders(null);
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVNullRows(){
		validRowSet.setRows(null);
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVRowsEmpty(){
		validRowSet.setRows(new LinkedList<Row>());
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndWriteToCSVModelsEmpty(){
		TableModelUtils.validateAndWriteToCSV(new LinkedList<ColumnModel>(), validRowSet, out);
	}
	
	@Test
	public void testValidateAndWriteToCSVOneNullRow(){
		try{
			validRowSet.getRows().get(0).setValues(null);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		}catch (IllegalArgumentException e){
			assertEquals("Row 0 has null list of values", e.getMessage());
		}
	}
	
	@Test
	public void testValidateAndWriteToCSVWrongValueSize(){
		try{
			validRowSet.getRows().get(0).getValues().add("too many");
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		}catch (IllegalArgumentException e){
			assertEquals("Row.value size must be equal to the number of columns in the table.  The table has :2 columns and the passed Row.value has: 3 for row number: 0", e.getMessage());
		}
	}
	
	@Test
	public void testValidateAndWriteToCSVRowIdNull(){
		try{
			validRowSet.getRows().get(0).setRowId(null);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		}catch (IllegalArgumentException e){
			assertEquals("Row.rowId cannot be null for row number: 0", e.getMessage());
		}
	}
	
	@Test
	public void testValidateAndWriteToCSVRowVersionNull(){
		try{
			validRowSet.getRows().get(1).setVersionNumber(null);
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		}catch (IllegalArgumentException e){
			assertEquals("Row.versionNumber cannot be null for row number: 1", e.getMessage());
		}
	}
	
	@Test
	public void testValidateAndWriteToCSVHeaderMissmatch(){
		try{
			validRowSet.getHeaders().remove(0);
			validRowSet.getHeaders().add(0, "3");
			TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
			fail("Should have failed");
		}catch (IllegalArgumentException e){
			assertEquals("The Table's ColumnModels includes: name=two with id=2 but 2 was not found in the headers of the RowResults", e.getMessage());
		}
	}
	
	@Test
	public void testHappyCase() throws IOException{
		// Write the following data
		TableModelUtils.validateAndWriteToCSV(validModel, validRowSet, out);
		String csv = outWritter.toString();
		System.out.println(csv);
		StringReader reader = new StringReader(csv);
		// There should be two rows
		CSVReader in = new CSVReader(reader);
		List<String[]> results = in.readAll();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertTrue(Arrays.equals(new String[]{"456","2","true","9999"}, results.get(0)));
		assertTrue(Arrays.equals(new String[]{"457","2","false","0"}, results.get(1)));
	}
	
	@Test
	public void testValidateBoolean(){
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		assertEquals(Boolean.FALSE.toString(), TableModelUtils.validateRowValue("some string", cm, 0, 0));
		assertEquals(Boolean.TRUE.toString(), TableModelUtils.validateRowValue("true", cm, 1, 1));
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue(Boolean.TRUE.toString());
		assertEquals(Boolean.TRUE.toString(), TableModelUtils.validateRowValue(null, cm, 2, 2));
	}
	
	@Test
	public void testValidateLong(){
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.LONG);
		assertEquals("123", TableModelUtils.validateRowValue("123", cm, 0, 0));
		try {
			TableModelUtils.validateRowValue("true", cm, 1, 3);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,3] was not a valid LONG. For input string: \"true\"", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("890");
		assertEquals("890", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
	
	@Test
	public void testValidateFileHandleId(){
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
	public void testValidateDouble(){
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		assertEquals("123.1", TableModelUtils.validateRowValue("123.1", cm, 0, 0));
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
	public void testValidateString(){
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		assertEquals("some string", TableModelUtils.validateRowValue("some string", cm, 0, 0));
		try {
			char[] tooLarge = new char[TableModelUtils.MAX_STRING_LENGTH+1];
			Arrays.fill(tooLarge, 'b');
			TableModelUtils.validateRowValue(new String(tooLarge), cm, 1, 4);
			fail("should have failed");
		} catch (IllegalArgumentException e) {
			assertEquals("Value at [1,4] was not a valid STRING. String exceeds the maximum length of 2000 characters. Consider using a FileHandle to store large strings.", e.getMessage());
		}
		assertEquals(null, TableModelUtils.validateRowValue(null, cm, 2, 2));
		// Set the default to boolean
		cm.setDefaultValue("-89.3e12");
		assertEquals("-89.3e12", TableModelUtils.validateRowValue(null, cm, 2, 3));
	}
}
